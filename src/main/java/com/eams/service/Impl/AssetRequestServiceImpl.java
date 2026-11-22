package com.eams.service.impl;

import com.eams.dto.request.AssetRequestCreateRequest;
import com.eams.dto.request.AssetRequestRejectRequest;
import com.eams.dto.request.AssetReturnRequest;
import com.eams.dto.response.AssetAssignmentResponse;
import com.eams.dto.response.AssetRequestResponse;
import com.eams.entity.Asset;
import com.eams.entity.AssetAssignment;
import com.eams.entity.AssetRequest;
import com.eams.entity.AssetStatus;
import com.eams.entity.AssignmentStatus;
import com.eams.entity.Category;
import com.eams.entity.Employee;
import com.eams.entity.EmployeeStatus;
import com.eams.entity.RequestPriority;
import com.eams.entity.RequestStatus;
import com.eams.entity.User;
import com.eams.exception.BadRequestException;
import com.eams.exception.ConflictException;
import com.eams.exception.ForbiddenException;
import com.eams.exception.NotFoundException;
import com.eams.repository.AssetAssignmentRepository;
import com.eams.repository.AssetRepository;
import com.eams.repository.AssetRequestRepository;
import com.eams.repository.CategoryRepository;
import com.eams.security.CurrentUserService;
import com.eams.service.AssetLifecycleService;
import com.eams.service.AssetRequestService;
import com.eams.service.AuditService;
import com.eams.service.DtoMapper;
import com.eams.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class AssetRequestServiceImpl implements AssetRequestService {

    private final AssetRequestRepository requestRepository;
    private final AssetRepository assetRepository;
    private final CategoryRepository categoryRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final AssetLifecycleService lifecycleService;
    private final CurrentUserService currentUser;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final DtoMapper mapper;

    // =========================================================
    // 1. EMPLOYEE CREATES A REQUEST
    // =========================================================

    @Override
    public AssetRequestResponse createRequest(AssetRequestCreateRequest dto) {
        Employee employee = currentUser.employee();

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new ForbiddenException("Inactive employees cannot request assets");
        }

        Asset asset = null;
        Category category;

        if (dto.getAssetId() != null) {
            asset = assetRepository.findById(dto.getAssetId())
                    .orElseThrow(() -> new NotFoundException("Asset not found"));
            if (asset.getStatus() != AssetStatus.AVAILABLE) {
                throw new BadRequestException("Asset is not available for request");
            }
            if (requestRepository.existsByEmployeeIdAndAssetIdAndStatusIn(
                    employee.getId(), asset.getId(), AssetRequestRepository.OPEN)) {
                throw new ConflictException("You already have an open request for this asset");
            }
            category = asset.getCategory();
        } else {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
        }

        AssetRequest request = new AssetRequest(employee, asset, dto.getReason().trim());
        request.setCategory(category);
        request.setPriority(dto.getPriority() != null ? dto.getPriority() : RequestPriority.NORMAL);
        request.setNeededBy(dto.getNeededBy());

        AssetRequest saved = requestRepository.save(request);

        String what = asset != null
                ? asset.getAssetCode() + " - " + asset.getName()
                : "any " + category.getName();
        auditService.record(asset != null ? asset.getId() : null, "REQUEST", saved.getId(),
                "CREATED", DtoMapper.fullName(employee) + " requested " + what);
        notificationService.notifyRole("MANAGER", "REQUEST_CREATED",
                "New asset request",
                DtoMapper.fullName(employee) + " requested " + what
                        + " (priority " + saved.getPriority() + ").",
                "/requests/" + saved.getId());

        return toResponse(saved);
    }

    // =========================================================
    // 2. EMPLOYEE SELF-SERVICE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssetRequestResponse> getMyRequests(Long employeeIdFromPath) {
        Employee me = currentUser.requireSelf(employeeIdFromPath);
        return requestRepository.findByEmployeeIdOrderByIdDesc(me.getId())
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetAssignmentResponse> getMyAssignedAssets(Long employeeIdFromPath) {
        Employee me = currentUser.requireSelf(employeeIdFromPath);
        return assignmentRepository
                .findByEmployeeIdAndStatusIn(me.getId(), AssetAssignmentRepository.HOLDING)
                .stream().map(mapper::assignment).toList();
    }

    @Override
    public AssetAssignmentResponse requestAssetReturn(
            Long assignmentId, Long employeeIdFromPath, AssetReturnRequest request) {

        Employee me = currentUser.requireSelf(employeeIdFromPath);
        AssetAssignment assignment = findAssignment(assignmentId);

        if (!Objects.equals(assignment.getEmployee().getId(), me.getId())) {
            throw new ForbiddenException("You can only request return for your own asset");
        }
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE assignments can request a return");
        }

        // The employee still physically has the asset, so it stays ASSIGNED.
        assignment.setStatus(AssignmentStatus.RETURN_REQUESTED);
        if (request != null && request.getReturnNote() != null) {
            assignment.setReturnNote(request.getReturnNote());
        }

        Asset asset = assignment.getAsset();
        auditService.record(asset.getId(), "ASSIGNMENT", assignment.getId(),
                "RETURN_REQUESTED", DtoMapper.fullName(me) + " requested to return the asset");
        notificationService.notifyRole("ADMIN", "RETURN_REQUESTED",
                "Asset return requested",
                DtoMapper.fullName(me) + " wants to return " + asset.getAssetCode()
                        + " - " + asset.getName() + ".",
                "/admin/assignments/" + assignment.getId());

        return mapper.assignment(assignment);
    }

    // =========================================================
    // 3. MANAGER: VIEW / APPROVE / REJECT
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssetRequestResponse> getPendingRequests() {
        return requestRepository.findByStatus(RequestStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetRequestResponse> getAllRequests() {
        return requestRepository.findAllByOrderByIdDesc()
                .stream().map(this::toResponse).toList();
    }

    @Override
    public AssetRequestResponse approveRequest(Long requestId) {
        AssetRequest request = findRequest(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Only PENDING requests can be approved");
        }
        Employee employee = request.getEmployee();
        if (employee == null || employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BadRequestException("Cannot approve a request from an inactive employee");
        }
        if (request.getAsset() != null && request.getAsset().getStatus() != AssetStatus.AVAILABLE) {
            throw new ConflictException("The requested asset is no longer available");
        }

        User manager = currentUser.user();
        request.setStatus(RequestStatus.APPROVED);
        request.setApprovedBy(manager);
        request.setApprovedAt(LocalDateTime.now());

        String what = describeTarget(request);
        auditService.record(assetId(request), "REQUEST", request.getId(), "APPROVED",
                "Approved by " + manager.getEmail());
        notificationService.notifyEmployee(employee, "REQUEST_APPROVED",
                "Asset request approved",
                "Your request for " + what + " was approved. "
                        + "An administrator will assign it to you shortly.",
                "/requests/" + request.getId());
        notificationService.notifyRole("ADMIN", "REQUEST_READY_TO_ASSIGN",
                "Approved request ready to assign",
                "Request #" + request.getId() + " (" + what + ") for "
                        + DtoMapper.fullName(employee) + " is approved and waiting for assignment.",
                "/requests/" + request.getId());

        return toResponse(request);
    }

    @Override
    public AssetRequestResponse rejectRequest(Long requestId, AssetRequestRejectRequest dto) {
        AssetRequest request = findRequest(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Only PENDING requests can be rejected");
        }

        User manager = currentUser.user();
        request.setStatus(RequestStatus.REJECTED);
        request.setApprovedBy(manager);
        request.setApprovedAt(LocalDateTime.now());
        request.setRejectionReason(dto.getRejectionReason());

        auditService.record(assetId(request), "REQUEST", request.getId(), "REJECTED",
                "Rejected by " + manager.getEmail() + ": " + dto.getRejectionReason());
        notificationService.notifyEmployee(request.getEmployee(), "REQUEST_REJECTED",
                "Asset request rejected",
                "Your request for " + describeTarget(request) + " was rejected. Reason: "
                        + dto.getRejectionReason(),
                "/requests/" + request.getId());

        return toResponse(request);
    }

    // =========================================================
    // 4. ADMIN: ASSIGN / RECEIVE RETURN
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssetRequestResponse> getApprovedRequests() {
        return requestRepository.findByStatus(RequestStatus.APPROVED).stream()
                .filter(r -> r.getEmployee() != null
                        && r.getEmployee().getStatus() == EmployeeStatus.ACTIVE)
                .filter(r -> r.getAsset() == null
                        || r.getAsset().getStatus() == AssetStatus.AVAILABLE)
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AssetAssignmentResponse assignAsset(Long requestId, Long assetIdParam) {
        AssetRequest request = findRequest(requestId);

        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new BadRequestException("Only APPROVED requests can be assigned");
        }

        Employee employee = request.getEmployee();
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BadRequestException("Cannot assign an asset to an inactive employee");
        }

        Asset asset = resolveAssetToAssign(request, assetIdParam);

        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new ConflictException("Asset is not available for assignment");
        }
        if (lifecycleService.isHeld(asset.getId())) {
            throw new ConflictException("Asset is already assigned to another employee");
        }

        User admin = currentUser.user();
        AssetAssignment assignment = assignmentRepository.save(
                new AssetAssignment(asset, employee, LocalDate.now(), admin));

        request.setAsset(asset);
        request.setStatus(RequestStatus.ASSIGNED);

        // Touch the asset so its @Version increments: if two admins assign the
        // same asset at the same moment, the second one gets a 409 instead of
        // creating a second assignment.
        asset.setUpdatedAt(LocalDateTime.now());
        lifecycleService.refresh(asset, "assigned to " + DtoMapper.fullName(employee));

        closeCompetingRequests(asset, request.getId());

        auditService.record(asset.getId(), "ASSIGNMENT", assignment.getId(), "ASSIGNED",
                "Assigned to " + DtoMapper.fullName(employee) + " by " + admin.getEmail()
                        + " (request #" + request.getId() + ")");
        notificationService.notifyEmployee(employee, "ASSET_ASSIGNED",
                "Asset assigned to you",
                asset.getAssetCode() + " - " + asset.getName() + " has been assigned to you.",
                "/my-assets");

        return mapper.assignment(assignment);
    }

    @Override
    public AssetAssignmentResponse returnAsset(Long assignmentId, AssetReturnRequest dto) {
        AssetAssignment assignment = findAssignment(assignmentId);

        if (assignment.getStatus() == AssignmentStatus.RETURNED) {
            throw new BadRequestException("This asset has already been returned");
        }

        assignment.setStatus(AssignmentStatus.RETURNED);
        assignment.setReturnedDate(LocalDate.now());
        if (dto != null && dto.getReturnNote() != null && !dto.getReturnNote().isBlank()) {
            assignment.setReturnNote(dto.getReturnNote());
        }

        Asset asset = assignment.getAsset();
        // AVAILABLE, or still DAMAGED / MAINTENANCE if there is an open damage report.
        lifecycleService.refresh(asset, "returned by " + DtoMapper.fullName(assignment.getEmployee()));

        auditService.record(asset.getId(), "ASSIGNMENT", assignment.getId(), "RETURNED",
                "Return received from " + DtoMapper.fullName(assignment.getEmployee())
                        + " by " + currentUser.principal().email());
        notificationService.notifyEmployee(assignment.getEmployee(), "ASSET_RETURNED",
                "Asset return confirmed",
                "Your return of " + asset.getAssetCode() + " - " + asset.getName()
                        + " has been received.",
                "/my-assets");

        return mapper.assignment(assignment);
    }

    // =========================================================
    // 5. READ ONE / HISTORY
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssetAssignmentResponse> getAssignmentHistory() {
        return assignmentRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .map(mapper::assignment)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssetRequestResponse getRequestById(Long id) {
        AssetRequest request = findRequest(id);
        if (currentUser.isEmployeeRole()) {
            Employee me = currentUser.employee();
            if (!Objects.equals(request.getEmployee().getId(), me.getId())) {
                throw new ForbiddenException("You can only view your own requests");
            }
        }
        return toResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetAssignmentResponse getAssignmentById(Long assignmentId) {
        AssetAssignment assignment = findAssignment(assignmentId);
        if (currentUser.isEmployeeRole()) {
            Employee me = currentUser.employee();
            if (!Objects.equals(assignment.getEmployee().getId(), me.getId())) {
                throw new ForbiddenException("You can only view your own assignments");
            }
        } else if (currentUser.hasRole("TECHNICIAN")) {
            throw new ForbiddenException("You do not have permission to view assignments");
        }
        return mapper.assignment(assignment);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Asset resolveAssetToAssign(AssetRequest request, Long assetIdParam) {
        if (request.getAsset() != null) {
            if (assetIdParam != null && !assetIdParam.equals(request.getAsset().getId())) {
                throw new BadRequestException(
                        "This request is for a specific asset; it cannot be assigned a different one");
            }
            return assetRepository.findById(request.getAsset().getId())
                    .orElseThrow(() -> new NotFoundException("Asset not found"));
        }

        if (assetIdParam == null) {
            throw new BadRequestException(
                    "This is a category request - choose which asset to assign (assetId)");
        }
        Asset asset = assetRepository.findById(assetIdParam)
                .orElseThrow(() -> new NotFoundException("Asset not found"));
        if (request.getCategory() != null
                && !Objects.equals(asset.getCategory().getId(), request.getCategory().getId())) {
            throw new BadRequestException("The chosen asset is not in the requested category ("
                    + request.getCategory().getName() + ")");
        }
        return asset;
    }

    /**
     * When an asset is assigned, other employees' open requests for that
     * SAME asset can never be fulfilled - close them instead of leaving them
     * PENDING/APPROVED forever.
     */
    private void closeCompetingRequests(Asset asset, Long assignedRequestId) {
        for (AssetRequest other : requestRepository.findByAssetIdAndStatusIn(
                asset.getId(), AssetRequestRepository.OPEN)) {
            if (other.getId().equals(assignedRequestId)) {
                continue;
            }
            other.setStatus(RequestStatus.REJECTED);
            other.setRejectionReason("Asset was assigned to another employee");
            notificationService.notifyEmployee(other.getEmployee(), "REQUEST_REJECTED",
                    "Asset request closed",
                    asset.getAssetCode() + " - " + asset.getName()
                            + " was assigned to another employee. "
                            + "You can request a different asset of the same category.",
                    "/requests/" + other.getId());
        }
    }

    private AssetRequestResponse toResponse(AssetRequest request) {
        AssignmentStatus assignmentStatus = null;
        if (request.getStatus() == RequestStatus.ASSIGNED && request.getAsset() != null) {
            assignmentStatus = assignmentRepository
                    .findFirstByEmployeeIdAndAssetIdOrderByIdDesc(
                            request.getEmployee().getId(), request.getAsset().getId())
                    .map(AssetAssignment::getStatus)
                    .orElse(null);
        }
        return mapper.request(request, assignmentStatus);
    }

    private String describeTarget(AssetRequest request) {
        if (request.getAsset() != null) {
            return request.getAsset().getAssetCode() + " - " + request.getAsset().getName();
        }
        return request.getCategory() != null ? "a " + request.getCategory().getName() : "an asset";
    }

    private Long assetId(AssetRequest request) {
        return request.getAsset() != null ? request.getAsset().getId() : null;
    }

    private AssetRequest findRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Asset request not found with id: " + id));
    }

    private AssetAssignment findAssignment(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Asset assignment not found"));
    }
}
