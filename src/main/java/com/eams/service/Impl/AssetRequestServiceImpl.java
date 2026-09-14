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
import com.eams.entity.Employee;
import com.eams.entity.RequestStatus;
import com.eams.entity.User;

import com.eams.repository.AssetAssignmentRepository;
import com.eams.repository.AssetRepository;
import com.eams.repository.AssetRequestRepository;
import com.eams.repository.EmployeeRepository;
import com.eams.repository.UserRepository;

import com.eams.service.AssetRequestService;
import com.eams.service.NotificationService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class AssetRequestServiceImpl implements AssetRequestService {

    private final AssetRequestRepository assetRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final AssetAssignmentRepository assetAssignmentRepository;
    private final NotificationService notificationService;

    public AssetRequestServiceImpl(
            AssetRequestRepository assetRequestRepository,
            EmployeeRepository employeeRepository,
            AssetRepository assetRepository,
            UserRepository userRepository,
            AssetAssignmentRepository assetAssignmentRepository,
            NotificationService notificationService
    ) {
        this.assetRequestRepository = assetRequestRepository;
        this.employeeRepository = employeeRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.assetAssignmentRepository = assetAssignmentRepository;
        this.notificationService = notificationService;
    }

    // =========================================================
    // FUNCTION 1 — Employee Creates an Asset Request
    // =========================================================

    @Override
    public AssetRequestResponse createRequest(
            AssetRequestCreateRequest request
    ) {

        Employee employee = employeeRepository
                .findById(request.getEmployeeId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Employee not found"
                        )
                );

        Asset asset = assetRepository
                .findById(request.getAssetId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Asset not found"
                        )
                );

        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Asset is not available for request"
            );
        }

        AssetRequest assetRequest =
                new AssetRequest(
                        employee,
                        asset,
                        request.getReason()
                );

        AssetRequest savedRequest =
                assetRequestRepository.save(assetRequest);

        return toResponse(savedRequest);
    }

    // =========================================================
    // FUNCTION 2 — Employee Views My Requests
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssetRequestResponse> getMyRequests(Long employeeId) {

        return assetRequestRepository
                .findByEmployeeId(employeeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // FUNCTION 3 — Manager Views Pending Requests
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssetRequestResponse> getPendingRequests() {

        return assetRequestRepository
                .findByStatus(RequestStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // FUNCTION 4 — Manager Approves Request
    // =========================================================

    @Override
    public AssetRequestResponse approveRequest(
            Long requestId,
            Long managerId
    ) {

        AssetRequest assetRequest =
                assetRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Asset request not found"
                                )
                        );

        if (assetRequest.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PENDING requests can be approved"
            );
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Manager not found"
                        )
                );

        if (manager.getRole() == null ||
                !"MANAGER".equalsIgnoreCase(
                        manager.getRole().getName()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only a Manager can approve asset requests"
            );
        }

        assetRequest.setStatus(RequestStatus.APPROVED);
        assetRequest.setApprovedBy(manager);
        assetRequest.setApprovedAt(LocalDateTime.now());

        AssetRequest savedRequest =
                assetRequestRepository.save(assetRequest);

        notificationService.sendRequestApprovedNotification(
                savedRequest.getEmployee().getEmail(),
                savedRequest.getAsset() != null
                        ? savedRequest.getAsset().getName()
                        : "asset"
        );

        return toResponse(savedRequest);
    }

    // =========================================================
    // FUNCTION 5 — Manager Rejects Request
    // =========================================================

    @Override
    public AssetRequestResponse rejectRequest(
            Long requestId,
            Long managerId,
            AssetRequestRejectRequest request
    ) {

        AssetRequest assetRequest =
                assetRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Asset request not found"
                                )
                        );

        if (assetRequest.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PENDING requests can be rejected"
            );
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Manager not found"
                        )
                );

        if (manager.getRole() == null ||
                !"MANAGER".equalsIgnoreCase(
                        manager.getRole().getName()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only a Manager can reject asset requests"
            );
        }

        assetRequest.setStatus(RequestStatus.REJECTED);
        assetRequest.setApprovedBy(manager);
        assetRequest.setRejectionReason(
                request.getRejectionReason()
        );

        AssetRequest savedRequest =
                assetRequestRepository.save(assetRequest);

        notificationService.sendRequestRejectedNotification(
                savedRequest.getEmployee().getEmail(),
                savedRequest.getAsset() != null
                        ? savedRequest.getAsset().getName()
                        : "asset",
                request.getRejectionReason()
        );

        return toResponse(savedRequest);
    }

    // =========================================================
    // FUNCTION 6 — Admin Views Approved Requests
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssetRequestResponse> getApprovedRequests() {

        return assetRequestRepository
                .findByStatus(RequestStatus.APPROVED)
                .stream()
                .filter(request ->
                        request.getAsset() != null &&
                                request.getAsset().getStatus() ==
                                        AssetStatus.AVAILABLE
                )
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // FUNCTION 7 — Admin Assign Asset
    // =========================================================

    @Override
    public AssetAssignmentResponse assignAsset(
            Long requestId,
            Long adminId
    ) {

        AssetRequest assetRequest =
                assetRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Asset request not found"
                                )
                        );

        if (assetRequest.getStatus() != RequestStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only APPROVED requests can be assigned"
            );
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Admin not found"
                        )
                );

        if (admin.getRole() == null ||
                !"ADMIN".equalsIgnoreCase(
                        admin.getRole().getName()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only an Admin can assign assets"
            );
        }

        Employee employee = employeeRepository
                .findById(assetRequest.getEmployee().getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Employee not found"
                        )
                );

        if (assetRequest.getAsset() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Asset is not associated with this request"
            );
        }

        Asset asset = assetRepository
                .findById(assetRequest.getAsset().getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Asset not found"
                        )
                );

        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Asset is not available for assignment"
            );
        }

        boolean alreadyAssigned =
                assetAssignmentRepository
                        .existsByAssetIdAndStatus(
                                asset.getId(),
                                AssignmentStatus.ACTIVE
                        );

        if (alreadyAssigned) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Asset is already assigned to another employee"
            );
        }

        AssetAssignment assignment =
                new AssetAssignment(
                        asset,
                        employee,
                        LocalDate.now(),
                        admin
                );

        AssetAssignment savedAssignment =
                assetAssignmentRepository.save(assignment);

        asset.setStatus(AssetStatus.ASSIGNED);
        assetRepository.save(asset);

        assetRequest.setStatus(RequestStatus.ASSIGNED);
        assetRequestRepository.save(assetRequest);

        notificationService.sendAssetAssignedNotification(
                employee.getEmail(),
                asset.getName(),
                asset.getAssetCode()
        );

        return toAssignmentResponse(savedAssignment);
    }

    // =========================================================
    // FUNCTION 8 — Employee Requests Asset Return
    // =========================================================

    @Override
    public AssetAssignmentResponse requestAssetReturn(
            Long assignmentId,
            Long employeeId,
            AssetReturnRequest request
    ) {

        AssetAssignment assignment =
                assetAssignmentRepository.findById(assignmentId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Asset assignment not found"
                                )
                        );

        if (!assignment.getEmployee().getId().equals(employeeId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only request return for your own asset"
            );
        }

        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only ACTIVE assignments can request a return"
            );
        }

        assignment.setStatus(
                AssignmentStatus.RETURN_REQUESTED
        );

        assignment.setReturnNote(
                request.getReturnNote()
        );

        AssetAssignment savedAssignment =
                assetAssignmentRepository.save(assignment);

        return toAssignmentResponse(savedAssignment);
    }

    // =========================================================
    // FUNCTION 9 — Admin Receives Returned Asset
    // =========================================================

    @Override
    public AssetAssignmentResponse returnAsset(
            Long assignmentId,
            Long adminId,
            AssetReturnRequest request
    ) {

        AssetAssignment assignment =
                assetAssignmentRepository.findById(assignmentId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Asset assignment not found"
                                )
                        );

        User admin = userRepository.findById(adminId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Admin not found"
                        )
                );

        if (admin.getRole() == null ||
                !"ADMIN".equalsIgnoreCase(
                        admin.getRole().getName()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only an Admin can return assets"
            );
        }

        if (assignment.getStatus() !=
                AssignmentStatus.RETURN_REQUESTED) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only RETURN_REQUESTED assignments can be received"
            );
        }

        Asset asset = assetRepository
                .findById(assignment.getAsset().getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Asset not found"
                        )
                );

        assignment.setStatus(AssignmentStatus.RETURNED);
        assignment.setReturnedDate(LocalDate.now());

        /*
         * Keep the employee's return note if one already exists.
         * If the admin supplies a note, replace it.
         */
        if (request != null &&
                request.getReturnNote() != null &&
                !request.getReturnNote().isBlank()) {

            assignment.setReturnNote(
                    request.getReturnNote()
            );
        }

        AssetAssignment savedAssignment =
                assetAssignmentRepository.save(assignment);

        asset.setStatus(AssetStatus.AVAILABLE);
        assetRepository.save(asset);

        notificationService.sendAssetReturnedNotification(
                savedAssignment.getEmployee().getEmail(),
                asset.getName(),
                asset.getAssetCode()
        );

        return toAssignmentResponse(savedAssignment);
    }

    // =========================================================
    // FUNCTION 10 — Admin Views Assignment History
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssetAssignmentResponse> getAssignmentHistory() {

        return assetAssignmentRepository.findAll()
                .stream()
                .map(this::toAssignmentResponse)
                .toList();
    }

    // =========================================================
    // FUNCTION 11 — Get One Request By ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public AssetRequestResponse getRequestById(Long id) {

        AssetRequest assetRequest =
                assetRequestRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Asset request not found with id: " + id
                                )
                        );

        return toResponse(assetRequest);
    }

    // =========================================================
    // FUNCTION 12 — Employee Views My Assigned Assets
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssetAssignmentResponse> getMyAssignedAssets(
            Long employeeId
    ) {

        return assetAssignmentRepository
                .findByEmployeeId(employeeId)
                .stream()
                .filter(assignment ->
                        assignment.getStatus() ==
                                AssignmentStatus.ACTIVE
                                ||
                                assignment.getStatus() ==
                                        AssignmentStatus.RETURN_REQUESTED
                )
                .map(this::toAssignmentResponse)
                .toList();
    }

    // =========================================================
    // FUNCTION 13 — Get One Assignment By ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public AssetAssignmentResponse getAssignmentById(
            Long assignmentId
    ) {

        AssetAssignment assignment =
                assetAssignmentRepository.findById(assignmentId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Asset assignment not found"
                                )
                        );

        return toAssignmentResponse(assignment);
    }

    // =========================================================
    // MAPPERS
    // =========================================================

    private AssetAssignmentResponse toAssignmentResponse(
            AssetAssignment assignment
    ) {

        return new AssetAssignmentResponse(
                assignment.getId(),

                assignment.getAsset().getId(),

                assignment.getAsset().getAssetCode(),

                assignment.getAsset().getName(),

                assignment.getEmployee().getId(),

                assignment.getAssignedDate(),

                assignment.getReturnedDate(),

                assignment.getStatus(),

                assignment.getAssignedBy() != null
                        ? assignment.getAssignedBy().getId()
                        : null,

                assignment.getReturnNote()
        );
    }

    private AssetRequestResponse toResponse(
            AssetRequest request
    ) {

        AssignmentStatus assignmentStatus = null;

        if (request.getAsset() != null) {

            assignmentStatus = assetAssignmentRepository
                    .findByEmployeeId(
                            request.getEmployee().getId()
                    )
                    .stream()
                    .filter(assignment ->
                            assignment.getAsset().getId()
                                    .equals(
                                            request.getAsset().getId()
                                    )
                    )
                    .max(
                            Comparator.comparing(
                                    AssetAssignment::getId
                            )
                    )
                    .map(AssetAssignment::getStatus)
                    .orElse(null);
        }

        return new AssetRequestResponse(
                request.getId(),
                request.getEmployee().getId(),
                request.getAsset() != null
                        ? request.getAsset().getId()
                        : null,
                request.getRequestDate(),
                request.getReason(),
                request.getStatus(),
                assignmentStatus,
                request.getApprovedBy() != null
                        ? request.getApprovedBy().getId()
                        : null,
                request.getApprovedAt(),
                request.getRejectionReason()
        );
    }
}
