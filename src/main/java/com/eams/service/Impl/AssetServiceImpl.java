package com.eams.service.impl;

import com.eams.config.AppProperties;
import com.eams.dto.request.AssetRequestDto;
import com.eams.dto.request.Requests.RetireAssetRequest;
import com.eams.dto.response.AssetResponseDto;
import com.eams.dto.response.Responses.AssetScanResponse;
import com.eams.dto.response.Responses.MessageResponse;
import com.eams.entity.Asset;
import com.eams.entity.AssetAssignment;
import com.eams.entity.AssetRequest;
import com.eams.entity.AssetStatus;
import com.eams.entity.AssignmentStatus;
import com.eams.entity.Category;
import com.eams.entity.DisposalMethod;
import com.eams.entity.Employee;
import com.eams.entity.RequestStatus;
import com.eams.exception.BadRequestException;
import com.eams.exception.ConflictException;
import com.eams.exception.NotFoundException;
import com.eams.repository.AssetAssignmentRepository;
import com.eams.repository.AssetRepository;
import com.eams.repository.AssetRequestRepository;
import com.eams.repository.CategoryRepository;
import com.eams.security.AppUserPrincipal;
import com.eams.security.CurrentUserService;
import com.eams.service.AssetLifecycleService;
import com.eams.service.AssetService;
import com.eams.service.AuditService;
import com.eams.service.DtoMapper;
import com.eams.service.FileStorageService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final CategoryRepository categoryRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final AssetRequestRepository requestRepository;
    private final AssetLifecycleService lifecycleService;
    private final AuditService auditService;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;
    private final DtoMapper mapper;
    private final AppProperties properties;

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    public AssetResponseDto createAsset(AssetRequestDto dto) {
        String assetCode = dto.getAssetCode().trim();
        if (assetRepository.existsByAssetCodeIgnoreCase(assetCode)) {
            throw new ConflictException("Asset code already exists: " + assetCode);
        }

        String serialNumber = normalize(dto.getSerialNumber());
        if (serialNumber != null && assetRepository.existsBySerialNumberIgnoreCase(serialNumber)) {
            throw new ConflictException("Serial number already exists: " + serialNumber);
        }

        // A new asset can only start as AVAILABLE (or RETIRED for historic imports).
        AssetStatus status = dto.getStatus() == null ? AssetStatus.AVAILABLE : dto.getStatus();
        if (status != AssetStatus.AVAILABLE && status != AssetStatus.RETIRED) {
            throw new BadRequestException(
                    "A new asset must be AVAILABLE. Statuses like ASSIGNED or DAMAGED "
                            + "are set by the assignment / damage / maintenance workflows.");
        }

        Asset asset = new Asset();
        asset.setAssetCode(assetCode);
        apply(asset, dto, serialNumber);
        asset.setStatus(status);
        if (status == AssetStatus.RETIRED) {
            asset.setRetiredAt(LocalDateTime.now());
        }

        Asset saved = assetRepository.save(asset);
        auditService.record(saved.getId(), "ASSET", saved.getId(), "CREATED",
                "Registered " + saved.getAssetCode() + " - " + saved.getName());
        return mapper.asset(saved);
    }

    // =========================================================
    // READ
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> getAllAssets() {
        return assetRepository.findAll().stream().map(mapper::asset).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> filterAssets(AssetStatus status, Long categoryId, String query) {
        String q = normalize(query);
        return assetRepository.filter(status, categoryId, q).stream()
                .map(mapper::asset)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponseDto getAssetById(Long id) {
        Asset asset = findAsset(id);
        return mapper.asset(asset, currentHolder(id).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> searchAssets(String keyword) {
        return filterAssets(null, null, keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> getAssetsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Category not found with id: " + categoryId);
        }
        return assetRepository.findByCategoryId(categoryId).stream().map(mapper::asset).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> getAssetsByStatus(AssetStatus status) {
        if (status == null) {
            throw new BadRequestException("Asset status is required");
        }
        return assetRepository.findByStatus(status).stream().map(mapper::asset).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> getWarrantyExpiring(int days) {
        int window = Math.min(Math.max(days, 1), 3650);
        LocalDate today = LocalDate.now();
        return assetRepository.findWarrantyExpiringBetween(today, today.plusDays(window))
                .stream().map(mapper::asset).toList();
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    public AssetResponseDto updateAsset(Long id, AssetRequestDto dto) {
        Asset asset = findAsset(id);

        String assetCode = dto.getAssetCode().trim();
        if (assetRepository.existsByAssetCodeIgnoreCaseAndIdNot(assetCode, id)) {
            throw new ConflictException("Asset code already exists: " + assetCode);
        }
        String serialNumber = normalize(dto.getSerialNumber());
        if (serialNumber != null
                && assetRepository.existsBySerialNumberIgnoreCaseAndIdNot(serialNumber, id)) {
            throw new ConflictException("Serial number already exists: " + serialNumber);
        }

        asset.setAssetCode(assetCode);
        apply(asset, dto, serialNumber);

        AssetStatus requested = dto.getStatus();
        AssetStatus current = asset.getStatus();

        if (requested != null && requested != current) {
            if (requested == AssetStatus.RETIRED) {
                retire(asset, DisposalMethod.DISPOSE, "Retired via asset edit");
            } else if (current == AssetStatus.RETIRED && requested == AssetStatus.AVAILABLE) {
                // Re-activate a retired asset (e.g. retired by mistake)
                asset.setStatus(AssetStatus.AVAILABLE);
                asset.setRetiredAt(null);
                asset.setDisposalMethod(null);
                auditService.record(id, "ASSET", id, "REACTIVATED", "RETIRED -> AVAILABLE");
                lifecycleService.refresh(asset, "reactivated");
            } else {
                throw new BadRequestException(
                        "Status " + requested + " cannot be set manually. "
                                + "It is managed by the assignment, damage and maintenance workflows.");
            }
        }

        Asset saved = assetRepository.save(asset);
        auditService.record(id, "ASSET", id, "UPDATED", "Asset details updated");
        return mapper.asset(saved, currentHolder(id).orElse(null));
    }

    // =========================================================
    // DELETE / RETIRE
    // =========================================================

    @Override
    public MessageResponse deleteAsset(Long id) {
        Asset asset = findAsset(id);

        if (lifecycleService.isHeld(id)) {
            throw new ConflictException(
                    "This asset is currently assigned to an employee. Receive the return first.");
        }

        boolean hasHistory = lifecycleService.hasHistory(id) || requestRepository.existsByAssetId(id);
        if (hasHistory) {
            // Soft delete: keep the history, take the asset out of circulation.
            if (asset.getStatus() != AssetStatus.RETIRED) {
                retire(asset, DisposalMethod.DISPOSE, "Deleted by admin (asset has history)");
                assetRepository.save(asset);
            }
            return new MessageResponse(
                    "Asset has assignment/damage/maintenance history, so it was retired instead of deleted.");
        }

        assetRepository.delete(asset);
        auditService.record(id, "ASSET", id, "DELETED",
                "Deleted " + asset.getAssetCode() + " - " + asset.getName());
        return new MessageResponse("Asset deleted successfully");
    }

    @Override
    public AssetResponseDto retireAsset(Long id, RetireAssetRequest request) {
        Asset asset = findAsset(id);
        if (asset.getStatus() == AssetStatus.RETIRED) {
            throw new ConflictException("Asset is already retired");
        }
        if (lifecycleService.isHeld(id)) {
            throw new ConflictException(
                    "This asset is currently assigned to an employee. Receive the return first.");
        }
        retire(asset, request.disposalMethod(), request.note());
        return mapper.asset(assetRepository.save(asset));
    }

    private void retire(Asset asset, DisposalMethod method, String note) {
        if (lifecycleService.isHeld(asset.getId())) {
            throw new ConflictException(
                    "This asset is currently assigned to an employee. Receive the return first.");
        }
        AssetStatus before = asset.getStatus();
        asset.setStatus(AssetStatus.RETIRED);
        asset.setRetiredAt(LocalDateTime.now());
        asset.setDisposalMethod(method);

        // Nobody can receive a retired asset: close open requests for it.
        for (AssetRequest r : requestRepository.findByAssetIdAndStatusIn(
                asset.getId(), List.of(RequestStatus.PENDING, RequestStatus.APPROVED))) {
            r.setStatus(RequestStatus.REJECTED);
            r.setRejectionReason("Asset was retired");
        }

        auditService.record(asset.getId(), "ASSET", asset.getId(), "RETIRED",
                before + " -> RETIRED, disposal: " + method
                        + (note != null && !note.isBlank() ? " (" + note + ")" : ""));
    }

    // =========================================================
    // IMAGE
    // =========================================================

    @Override
    public String uploadAssetImage(Long id, MultipartFile file) {
        Asset asset = findAsset(id);
        FileStorageService.StoredFile stored = fileStorageService.storeImage(file, "assets");
        asset.setImageUrl(stored.url());
        assetRepository.save(asset);
        auditService.record(id, "ASSET", id, "IMAGE_UPLOADED", stored.url());
        return stored.url();
    }

    // =========================================================
    // QR CODE
    // =========================================================

    /**
     * The QR code now contains a URL (frontend /a/{assetCode}), so scanning it
     * with any phone camera opens the asset page directly.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] generateQrCode(Long id) {
        Asset asset = findAsset(id);
        return renderQr(qrUrl(asset), 300);
    }

    public String qrUrl(Asset asset) {
        return properties.frontendUrl() + "/a/"
                + URLEncoder.encode(asset.getAssetCode(), StandardCharsets.UTF_8);
    }

    public static byte[] renderQr(String content, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    content, BarcodeFormat.QR_CODE, size, size,
                    Map.of(EncodeHintType.MARGIN, 1));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error generating QR code", e);
        }
    }

    // =========================================================
    // SCAN
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public AssetScanResponse scanByCode(String assetCode) {
        Asset asset = assetRepository.findByAssetCodeIgnoreCase(assetCode.trim())
                .orElseThrow(() -> new NotFoundException("No asset with code " + assetCode));

        AppUserPrincipal me = currentUserService.principal();
        Optional<AssetAssignment> holding = assignmentRepository
                .findFirstByAssetIdAndStatusInOrderByIdDesc(asset.getId(),
                        AssetAssignmentRepository.HOLDING);
        Employee holder = holding.map(AssetAssignment::getEmployee).orElse(null);

        Long myEmployeeId = "EMPLOYEE".equals(me.role())
                ? currentUserService.optionalEmployee().map(Employee::getId).orElse(null)
                : null;
        boolean heldByMe = holder != null && Objects.equals(holder.getId(), myEmployeeId);

        List<String> actions = new ArrayList<>();
        actions.add("VIEW");
        AssetStatus status = asset.getStatus();

        switch (me.role()) {
            case "EMPLOYEE" -> {
                if (heldByMe) {
                    actions.add("REPORT_DAMAGE");
                    if (holding.get().getStatus() == AssignmentStatus.ACTIVE) {
                        actions.add("REQUEST_RETURN");
                    }
                } else if (status == AssetStatus.AVAILABLE && myEmployeeId != null) {
                    actions.add("REQUEST_ASSET");
                }
            }
            case "TECHNICIAN" -> {
                actions.add("VIEW_HISTORY");
                if (status == AssetStatus.DAMAGED || status == AssetStatus.MAINTENANCE) {
                    actions.add("UPDATE_DAMAGE");
                    actions.add("MANAGE_MAINTENANCE");
                }
                actions.add("AUDIT_SCAN");
            }
            case "ADMIN" -> {
                actions.add("VIEW_HISTORY");
                actions.add("EDIT");
                actions.add("PRINT_LABEL");
                actions.add("AUDIT_SCAN");
                if (holding.isPresent()
                        && holding.get().getStatus() == AssignmentStatus.RETURN_REQUESTED) {
                    actions.add("RECEIVE_RETURN");
                }
                if (status != AssetStatus.RETIRED && holding.isEmpty()) {
                    actions.add("RETIRE");
                }
            }
            case "MANAGER" -> actions.add("VIEW_HISTORY");
            default -> {
            }
        }

        // Employees only see the holder's name when it is themselves.
        Employee visibleHolder = "EMPLOYEE".equals(me.role()) && !heldByMe ? null : holder;

        return new AssetScanResponse(
                mapper.asset(asset, visibleHolder),
                heldByMe,
                heldByMe ? holding.get().getId() : null,
                actions);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void apply(Asset asset, AssetRequestDto dto, String serialNumber) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new NotFoundException(
                        "Category not found with id: " + dto.getCategoryId()));

        if (dto.getWarrantyExpiryDate() != null && dto.getPurchaseDate() != null
                && dto.getWarrantyExpiryDate().isBefore(dto.getPurchaseDate())) {
            throw new BadRequestException("Warranty expiry date cannot be before the purchase date");
        }

        asset.setName(dto.getName().trim());
        asset.setDescription(normalize(dto.getDescription()));
        asset.setSerialNumber(serialNumber);
        asset.setCategory(category);
        asset.setPurchaseDate(dto.getPurchaseDate());
        asset.setPurchasePrice(dto.getPurchasePrice());
        asset.setLocation(normalize(dto.getLocation()));
        asset.setWarrantyExpiryDate(dto.getWarrantyExpiryDate());
        asset.setVendor(normalize(dto.getVendor()));
    }

    private Optional<Employee> currentHolder(Long assetId) {
        return assignmentRepository
                .findFirstByAssetIdAndStatusInOrderByIdDesc(assetId, AssetAssignmentRepository.HOLDING)
                .map(AssetAssignment::getEmployee);
    }

    private Asset findAsset(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Asset not found with id: " + id));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
