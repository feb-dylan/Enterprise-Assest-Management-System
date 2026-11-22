package com.eams.service.impl;

import com.eams.dto.request.DamageCreateRequest;
import com.eams.dto.request.DamageUpdateRequest;
import com.eams.dto.response.DamageResponse;
import com.eams.entity.AiTriageStatus;
import com.eams.entity.Asset;
import com.eams.entity.AssetAssignment;
import com.eams.entity.AssetStatus;
import com.eams.entity.DamagePhoto;
import com.eams.entity.DamageReport;
import com.eams.entity.DamageStatus;
import com.eams.entity.Employee;
import com.eams.entity.Maintenance;
import com.eams.exception.BadRequestException;
import com.eams.exception.ConflictException;
import com.eams.exception.ForbiddenException;
import com.eams.exception.NotFoundException;
import com.eams.exception.ServiceUnavailableException;
import com.eams.repository.AssetAssignmentRepository;
import com.eams.repository.AssetRepository;
import com.eams.repository.DamageReportRepository;
import com.eams.repository.MaintenanceRepository;
import com.eams.security.CurrentUserService;
import com.eams.service.AssetLifecycleService;
import com.eams.service.AuditService;
import com.eams.service.DamageService;
import com.eams.service.DtoMapper;
import com.eams.service.FileStorageService;
import com.eams.service.NotificationService;
import com.eams.service.ai.AiTriageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class DamageServiceImpl implements DamageService {

    private static final int MAX_PHOTOS = 5;

    /** Allowed damage status changes. RESOLVED is final. */
    private static final Map<DamageStatus, Set<DamageStatus>> TRANSITIONS = Map.of(
            DamageStatus.REPORTED, Set.of(DamageStatus.UNDER_REVIEW, DamageStatus.REPAIRING, DamageStatus.RESOLVED),
            DamageStatus.UNDER_REVIEW, Set.of(DamageStatus.REPORTED, DamageStatus.REPAIRING, DamageStatus.RESOLVED),
            DamageStatus.REPAIRING, Set.of(DamageStatus.UNDER_REVIEW, DamageStatus.RESOLVED),
            DamageStatus.RESOLVED, Set.of());

    private final DamageReportRepository damageReportRepository;
    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final AssetLifecycleService lifecycleService;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUser;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AiTriageService aiTriageService;
    private final DtoMapper mapper;

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    public DamageResponse createDamageReport(DamageCreateRequest request) {
        Employee employee = currentUser.employee();

        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> new NotFoundException("Asset not found"));

        if (asset.getStatus() == AssetStatus.RETIRED) {
            throw new BadRequestException("Cannot report damage for a retired asset");
        }

        // Employees can only report damage for assets they currently hold.
        boolean holdsAsset = assignmentRepository
                .findFirstByAssetIdAndStatusInOrderByIdDesc(asset.getId(), AssetAssignmentRepository.HOLDING)
                .map(AssetAssignment::getEmployee)
                .map(holder -> Objects.equals(holder.getId(), employee.getId()))
                .orElse(false);
        if (!holdsAsset) {
            throw new ForbiddenException("You can only report damage for assets assigned to you");
        }

        DamageReport report = new DamageReport();
        report.setEmployee(employee);
        report.setAsset(asset);
        report.setDescription(request.getDescription().trim());
        report.setEvidenceUrl(blankToNull(request.getEvidenceUrl()));
        report.setStatus(DamageStatus.REPORTED);

        DamageReport saved = damageReportRepository.save(report);
        lifecycleService.refresh(asset, "damage reported");

        auditService.record(asset.getId(), "DAMAGE", saved.getId(), "REPORTED",
                DtoMapper.fullName(employee) + " reported damage: " + saved.getDescription());
        String message = DtoMapper.fullName(employee) + " reported damage on "
                + asset.getAssetCode() + " - " + asset.getName() + ".";
        notificationService.notifyRole("TECHNICIAN", "DAMAGE_REPORTED",
                "New damage report", message, "/damage/" + saved.getId());
        notificationService.notifyRole("ADMIN", "DAMAGE_REPORTED",
                "New damage report", message, "/damage/" + saved.getId());

        return mapper.damage(saved);
    }

    // =========================================================
    // PHOTOS (+ AI triage)
    // =========================================================

    @Override
    public DamageResponse addPhotos(Long id, List<MultipartFile> files) {
        DamageReport report = findReport(id);

        if (currentUser.isEmployeeRole()
                && !Objects.equals(report.getEmployee().getId(), currentUser.employee().getId())) {
            throw new ForbiddenException("You can only add photos to your own damage reports");
        }
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least one photo is required");
        }
        if (report.getPhotos().size() + files.size() > MAX_PHOTOS) {
            throw new BadRequestException("A damage report can have at most " + MAX_PHOTOS + " photos");
        }

        for (MultipartFile file : files) {
            FileStorageService.StoredFile stored = fileStorageService.storeImage(file, "damage");
            DamagePhoto photo = new DamagePhoto();
            photo.setDamageReport(report);
            photo.setUrl(stored.url());
            photo.setContentType(stored.contentType());
            report.getPhotos().add(photo);
        }
        if (report.getEvidenceUrl() == null) {
            report.setEvidenceUrl(report.getPhotos().get(0).getUrl());
        }

        report.setAiStatus(aiTriageService.isEnabled()
                ? AiTriageStatus.PENDING
                : AiTriageStatus.DISABLED);
        DamageReport saved = damageReportRepository.saveAndFlush(report);

        auditService.record(report.getAsset().getId(), "DAMAGE", id, "PHOTOS_ADDED",
                files.size() + " photo(s) added");
        triageAfterCommit(id);

        return mapper.damage(saved);
    }

    @Override
    public DamageResponse requestAiTriage(Long id) {
        DamageReport report = findReport(id);
        if (!aiTriageService.isEnabled()) {
            throw new ServiceUnavailableException(
                    "AI triage is not configured (set OPENROUTER_API_KEY and AI_MODEL on the server)");
        }
        if (report.getPhotos().isEmpty()) {
            throw new BadRequestException("Upload at least one photo before running AI triage");
        }
        report.setAiStatus(AiTriageStatus.PENDING);
        triageAfterCommit(id);
        return mapper.damage(report);
    }

    private void triageAfterCommit(Long id) {
        if (!aiTriageService.isEnabled()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    aiTriageService.analyzeAsync(id);
                }
            });
        } else {
            aiTriageService.analyzeAsync(id);
        }
    }

    // =========================================================
    // READ
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<DamageResponse> getMyDamageReports(Long employeeIdFromPath) {
        Employee me = currentUser.requireSelf(employeeIdFromPath);
        return damageReportRepository.findByEmployeeIdOrderByIdDesc(me.getId())
                .stream().map(mapper::damage).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DamageResponse> getAllDamageReports() {
        return damageReportRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .map(mapper::damage)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DamageResponse> getDamageReportsByAsset(Long assetId) {
        if (!assetRepository.existsById(assetId)) {
            throw new NotFoundException("Asset not found");
        }
        return damageReportRepository.findByAssetIdOrderByIdAsc(assetId)
                .stream().map(mapper::damage).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DamageResponse getDamageReportById(Long id) {
        DamageReport report = findReport(id);
        if (currentUser.isEmployeeRole()
                && !Objects.equals(report.getEmployee().getId(), currentUser.employee().getId())) {
            throw new ForbiddenException("You can only view your own damage reports");
        }
        return mapper.damage(report);
    }

    // =========================================================
    // UPDATE (technician / admin)
    // =========================================================

    @Override
    public DamageResponse updateDamageReport(Long id, DamageUpdateRequest request) {
        DamageReport report = findReport(id);
        DamageStatus from = report.getStatus();
        DamageStatus to = request.getStatus();

        if (from != to) {
            if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
                throw new BadRequestException("Cannot change damage status from " + from + " to " + to);
            }
            if (to == DamageStatus.RESOLVED && hasOpenMaintenance(report)) {
                throw new ConflictException(
                        "This damage has an open maintenance job. Complete or cancel it first.");
            }
        }

        report.setStatus(to);
        if (request.getResolutionNote() != null) {
            report.setResolutionNote(request.getResolutionNote());
        }

        Asset asset = report.getAsset();
        lifecycleService.refresh(asset, "damage " + to.name().toLowerCase());

        if (from != to) {
            auditService.record(asset.getId(), "DAMAGE", id, "STATUS_CHANGED",
                    "Damage report " + from + " -> " + to);
            notificationService.notifyEmployee(report.getEmployee(), "DAMAGE_UPDATED",
                    "Damage report " + to.name().replace('_', ' ').toLowerCase(),
                    "Your damage report for " + asset.getAssetCode() + " - " + asset.getName()
                            + " is now " + to + ".",
                    "/damage/" + id);
        }

        return mapper.damage(report);
    }

    private boolean hasOpenMaintenance(DamageReport report) {
        // Open = SCHEDULED or IN_PROGRESS, linked to this report (or unlinked legacy jobs)
        List<Maintenance> open = maintenanceRepository.findByAssetIdAndStatusIn(
                report.getAsset().getId(), MaintenanceRepository.OPEN);
        return open.stream().anyMatch(m ->
                m.getDamageReport() == null
                        || Objects.equals(m.getDamageReport().getId(), report.getId()));
    }

    // =========================================================

    private DamageReport findReport(Long id) {
        return damageReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Damage report not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
