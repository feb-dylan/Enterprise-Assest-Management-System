package com.eams.service.impl;

import com.eams.dto.request.MaintenanceCreateRequest;
import com.eams.dto.request.MaintenanceUpdateRequest;
import com.eams.dto.response.MaintenanceAssetResponse;
import com.eams.dto.response.MaintenanceResponse;
import com.eams.entity.Asset;
import com.eams.entity.AssetStatus;
import com.eams.entity.DamageReport;
import com.eams.entity.DamageStatus;
import com.eams.entity.Maintenance;
import com.eams.entity.MaintenanceStatus;
import com.eams.entity.User;
import com.eams.exception.BadRequestException;
import com.eams.exception.ConflictException;
import com.eams.exception.NotFoundException;
import com.eams.repository.AssetRepository;
import com.eams.repository.DamageReportRepository;
import com.eams.repository.MaintenanceRepository;
import com.eams.repository.UserRepository;
import com.eams.security.CurrentUserService;
import com.eams.service.AssetLifecycleService;
import com.eams.service.AuditService;
import com.eams.service.DtoMapper;
import com.eams.service.MaintenanceService;
import com.eams.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class MaintenanceServiceImpl implements MaintenanceService {

    /** Allowed status changes. COMPLETED and CANCELLED are final. */
    private static final Map<MaintenanceStatus, Set<MaintenanceStatus>> TRANSITIONS = Map.of(
            MaintenanceStatus.SCHEDULED, Set.of(MaintenanceStatus.IN_PROGRESS,
                    MaintenanceStatus.COMPLETED, MaintenanceStatus.CANCELLED),
            MaintenanceStatus.IN_PROGRESS, Set.of(MaintenanceStatus.COMPLETED,
                    MaintenanceStatus.CANCELLED),
            MaintenanceStatus.COMPLETED, Set.of(),
            MaintenanceStatus.CANCELLED, Set.of());

    private final MaintenanceRepository maintenanceRepository;
    private final AssetRepository assetRepository;
    private final DamageReportRepository damageReportRepository;
    private final UserRepository userRepository;
    private final AssetLifecycleService lifecycleService;
    private final CurrentUserService currentUser;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final DtoMapper mapper;

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    public MaintenanceResponse createMaintenance(MaintenanceCreateRequest request) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> new NotFoundException("Asset not found"));

        if (asset.getStatus() == AssetStatus.RETIRED) {
            throw new BadRequestException("Cannot create maintenance for a retired asset");
        }
        validateDates(request.getStartDate(), request.getEndDate());

        DamageReport damage = findRepairingDamage(asset, request.getDamageReportId());

        boolean alreadyOpen = maintenanceRepository
                .findByAssetIdAndStatusIn(asset.getId(), MaintenanceRepository.OPEN).stream()
                .anyMatch(m -> m.getDamageReport() == null
                        || Objects.equals(m.getDamageReport().getId(), damage.getId()));
        if (alreadyOpen) {
            throw new ConflictException("There is already an open maintenance job for this damage report");
        }

        User technician = resolveTechnician(request.getTechnicianUserId());

        Maintenance maintenance = new Maintenance();
        maintenance.setAsset(asset);
        maintenance.setDamageReport(damage);
        maintenance.setTechnicianUser(technician);
        maintenance.setTechnician(request.getTechnician() != null && !request.getTechnician().isBlank()
                ? request.getTechnician().trim()
                : technician != null ? technician.getEmail() : null);
        maintenance.setDescription(request.getDescription().trim());
        maintenance.setStartDate(request.getStartDate());
        maintenance.setEndDate(request.getEndDate());
        maintenance.setRepairCost(request.getRepairCost());
        maintenance.setNotes(request.getNotes());
        maintenance.setStatus(MaintenanceStatus.SCHEDULED);

        Maintenance saved = maintenanceRepository.save(maintenance);
        lifecycleService.refresh(asset, "maintenance scheduled");

        auditService.record(asset.getId(), "MAINTENANCE", saved.getId(), "CREATED",
                "Maintenance scheduled" + (technician != null ? " for " + technician.getEmail() : ""));
        if (technician != null && !Objects.equals(technician.getId(), currentUser.userId())) {
            notificationService.notify(technician, "MAINTENANCE_ASSIGNED",
                    "Maintenance job assigned to you",
                    asset.getAssetCode() + " - " + asset.getName() + ": " + saved.getDescription(),
                    "/maintenance/" + saved.getId());
        }

        return mapper.maintenance(saved);
    }

    // =========================================================
    // READ
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceAssetResponse> getRepairingAssets() {
        return damageReportRepository.findByStatus(DamageStatus.REPAIRING).stream()
                .map(DamageReport::getAsset)
                .filter(Objects::nonNull)
                .filter(a -> a.getStatus() != AssetStatus.RETIRED)
                .distinct()
                .map(a -> MaintenanceAssetResponse.builder()
                        .id(a.getId())
                        .assetCode(a.getAssetCode())
                        .name(a.getName())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getAllMaintenance() {
        return maintenanceRepository.findAll().stream()
                .sorted(Comparator.comparing(Maintenance::getId).reversed())
                .map(mapper::maintenance)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getMyMaintenance() {
        return maintenanceRepository.findByTechnicianUserIdOrderByIdDesc(currentUser.userId())
                .stream().map(mapper::maintenance).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceResponse getMaintenanceById(Long id) {
        return mapper.maintenance(findMaintenance(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getMaintenanceByAsset(Long assetId) {
        if (!assetRepository.existsById(assetId)) {
            throw new NotFoundException("Asset not found");
        }
        return maintenanceRepository.findByAssetIdOrderByIdAsc(assetId)
                .stream().map(mapper::maintenance).toList();
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    public MaintenanceResponse updateMaintenance(Long id, MaintenanceUpdateRequest request) {
        Maintenance m = findMaintenance(id);
        MaintenanceStatus from = m.getStatus();
        MaintenanceStatus to = request.getStatus() != null ? request.getStatus() : from;

        if (from != to && !TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new BadRequestException("Cannot change maintenance status from " + from + " to " + to);
        }

        if (request.getEndDate() != null) {
            validateDates(m.getStartDate(), request.getEndDate());
            m.setEndDate(request.getEndDate());
        }
        if (request.getRepairCost() != null) {
            m.setRepairCost(request.getRepairCost());
        }
        if (request.getNotes() != null) {
            m.setNotes(request.getNotes());
        }
        if (request.getTechnicianUserId() != null) {
            User technician = resolveTechnician(request.getTechnicianUserId());
            m.setTechnicianUser(technician);
            m.setTechnician(technician.getEmail());
        } else if (request.getTechnician() != null && !request.getTechnician().isBlank()) {
            m.setTechnician(request.getTechnician().trim());
        }

        m.setStatus(to);
        Asset asset = m.getAsset();

        if (from != to) {
            if (to == MaintenanceStatus.COMPLETED || to == MaintenanceStatus.CANCELLED) {
                if (m.getEndDate() == null) {
                    m.setEndDate(LocalDate.now());
                }
            }
            if (to == MaintenanceStatus.COMPLETED) {
                resolveLinkedDamage(m);
            } else if (to == MaintenanceStatus.CANCELLED) {
                reopenLinkedDamage(m);
            }

            auditService.record(asset.getId(), "MAINTENANCE", id, "STATUS_CHANGED",
                    "Maintenance " + from + " -> " + to
                            + (m.getRepairCost() != null ? " (cost " + m.getRepairCost() + ")" : ""));
        }

        lifecycleService.refresh(asset, "maintenance " + to.name().toLowerCase());
        return mapper.maintenance(m);
    }

    /** Repair finished: the damage it was fixing is resolved too. */
    private void resolveLinkedDamage(Maintenance m) {
        List<DamageReport> reports = m.getDamageReport() != null
                ? List.of(m.getDamageReport())
                : damageReportRepository.findByAssetIdAndStatus(m.getAsset().getId(), DamageStatus.REPAIRING);

        for (DamageReport report : reports) {
            if (report.getStatus() == DamageStatus.RESOLVED) {
                continue;
            }
            report.setStatus(DamageStatus.RESOLVED);
            if (report.getResolutionNote() == null || report.getResolutionNote().isBlank()) {
                report.setResolutionNote("Repaired (maintenance #" + m.getId() + ")");
            }
            notificationService.notifyEmployee(report.getEmployee(), "DAMAGE_RESOLVED",
                    "Repair completed",
                    m.getAsset().getAssetCode() + " - " + m.getAsset().getName()
                            + " has been repaired.",
                    "/damage/" + report.getId());
        }
    }

    /** Repair cancelled: the damage still exists, so it goes back to review. */
    private void reopenLinkedDamage(Maintenance m) {
        DamageReport report = m.getDamageReport();
        if (report != null && report.getStatus() == DamageStatus.REPAIRING) {
            report.setStatus(DamageStatus.UNDER_REVIEW);
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private DamageReport findRepairingDamage(Asset asset, Long damageReportId) {
        if (damageReportId != null) {
            DamageReport report = damageReportRepository.findById(damageReportId)
                    .orElseThrow(() -> new NotFoundException("Damage report not found"));
            if (!Objects.equals(report.getAsset().getId(), asset.getId())) {
                throw new BadRequestException("That damage report belongs to a different asset");
            }
            if (report.getStatus() != DamageStatus.REPAIRING) {
                throw new BadRequestException("The damage report must be in REPAIRING status");
            }
            return report;
        }
        return damageReportRepository.findByAssetIdAndStatus(asset.getId(), DamageStatus.REPAIRING)
                .stream()
                .max(Comparator.comparing(DamageReport::getId))
                .orElseThrow(() -> new BadRequestException(
                        "Maintenance can only be created for an asset with a REPAIRING damage report"));
    }

    private User resolveTechnician(Long technicianUserId) {
        if (technicianUserId == null) {
            return currentUser.hasRole("TECHNICIAN") ? currentUser.user() : null;
        }
        User user = userRepository.findById(technicianUserId)
                .orElseThrow(() -> new NotFoundException("Technician user not found"));
        if (!"TECHNICIAN".equals(user.getRole().getName())) {
            throw new BadRequestException("The selected user is not a technician");
        }
        return user;
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BadRequestException("End date cannot be before the start date");
        }
    }

    private Maintenance findMaintenance(Long id) {
        return maintenanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Maintenance record not found"));
    }
}
