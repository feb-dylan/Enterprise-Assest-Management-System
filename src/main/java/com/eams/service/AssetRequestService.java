package com.eams.service;

import com.eams.dto.request.AssetRequestCreateRequest;
import com.eams.dto.request.AssetRequestRejectRequest;
import com.eams.dto.response.AssetRequestResponse;
import com.eams.entity.Asset;
import com.eams.entity.AssetRequest;
import com.eams.entity.AssetStatus;
import com.eams.entity.Employee;
import com.eams.entity.RequestStatus;
import com.eams.entity.User;
import com.eams.repository.AssetRepository;
import com.eams.repository.AssetRequestRepository;
import com.eams.repository.EmployeeRepository;
import com.eams.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import com.eams.repository.AssetAssignmentRepository;
import com.eams.dto.response.AssetAssignmentResponse;
import com.eams.entity.AssetAssignment;
import com.eams.entity.AssignmentStatus;

import com.eams.dto.request.AssetReturnRequest;

@Service
public class AssetRequestService {

    private final AssetRequestRepository assetRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final AssetAssignmentRepository assetAssignmentRepository;

    public AssetRequestService(
            AssetRequestRepository assetRequestRepository,
            EmployeeRepository employeeRepository,
            AssetRepository assetRepository,
            UserRepository userRepository,
            AssetAssignmentRepository assetAssignmentRepository
    ) {
        this.assetRequestRepository = assetRequestRepository;
        this.employeeRepository = employeeRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.assetAssignmentRepository = assetAssignmentRepository;
    }

    // FUNCTION 1 — Employee Creates an Asset Request

    @Transactional
    public AssetRequestResponse createRequest(
            AssetRequestCreateRequest request
    ) {

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Employee not found"
                        )
                );

        Asset asset = assetRepository.findById(request.getAssetId())
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

    // FUNCTION 2 — Employee Views My Requests

    @Transactional(readOnly = true)
    public List<AssetRequestResponse> getMyRequests(Long employeeId) {

        List<AssetRequest> requests =
                assetRequestRepository.findByEmployeeId(employeeId);

        return requests.stream()
                .map(this::toResponse)
                .toList();
    }

    // FUNCTION 3 — Manager Views Pending Requests

    @Transactional(readOnly = true)
    public List<AssetRequestResponse> getPendingRequests() {

        List<AssetRequest> requests =
                assetRequestRepository.findByStatus(RequestStatus.PENDING);

        return requests.stream()
                .map(this::toResponse)
                .toList();
    }

    // FUNCTION 4 — Manager Approves Request

    @Transactional
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

        User manager =
                userRepository.findById(managerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Manager not found"
                                )
                        );

        if (manager.getRole() == null ||
                !"MANAGER".equalsIgnoreCase(manager.getRole().getName())) {

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

        return toResponse(savedRequest);
    }

    // FUNCTION 5 — Manager Rejects Request

    @Transactional
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

        User manager =
                userRepository.findById(managerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Manager not found"
                                )
                        );

        if (manager.getRole() == null ||
                !"MANAGER".equalsIgnoreCase(manager.getRole().getName())) {

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

        return toResponse(savedRequest);
    }

    // FUNCTION 6 — Admin Views Approved Requests

    @Transactional(readOnly = true)
    public List<AssetRequestResponse> getApprovedRequests() {

        List<AssetRequest> requests =
                assetRequestRepository.findByStatus(RequestStatus.APPROVED);

        return requests.stream()
                .map(this::toResponse)
                .toList();
    }


// FUNCTION 7 — Admin Assign Asset

    @Transactional
    public AssetAssignmentResponse assignAsset(
            Long requestId,
            Long adminId
    ) {

        // 1. Check whether the request exists

        AssetRequest assetRequest =
                assetRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Asset request not found"
                                )
                        );

        // 2. Check whether the request is APPROVED

        if (assetRequest.getStatus() != RequestStatus.APPROVED) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only APPROVED requests can be assigned"
            );
        }

        // 3. Check whether Admin exists

        User admin =
                userRepository.findById(adminId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Admin not found"
                                )
                        );

        // 4. Check whether the user is really an ADMIN

        if (admin.getRole() == null ||
                !"ADMIN".equalsIgnoreCase(
                        admin.getRole().getName()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only an Admin can assign assets"
            );
        }

        // 5. Check whether Employee exists

        Employee employee =
                employeeRepository.findById(
                                assetRequest.getEmployee().getId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Employee not found"
                                )
                        );

        // 6. Check whether Asset exists

        if (assetRequest.getAsset() == null) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Asset is not associated with this request"
            );
        }

        Asset asset =
                assetRepository.findById(
                                assetRequest.getAsset().getId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Asset not found"
                                )
                        );

        // 7. Check whether Asset is AVAILABLE

        if (asset.getStatus() != AssetStatus.AVAILABLE) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Asset is not available for assignment"
            );
        }

        // 8. Check whether Asset is already assigned

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

        // 9. Create AssetAssignment record

        AssetAssignment assignment =
                new AssetAssignment(
                        asset,
                        employee,
                        java.time.LocalDate.now(),
                        admin
                );

        // 10. Save AssetAssignment

        AssetAssignment savedAssignment =
                assetAssignmentRepository.save(assignment);

        // 11. Change Asset status

        asset.setStatus(AssetStatus.ASSIGNED);

        assetRepository.save(asset);

        // 12. Return response

        return toAssignmentResponse(savedAssignment);
    }

    // FUNCTION 8 — Admin Returns Asset

    @Transactional
    public AssetAssignmentResponse returnAsset(
            Long assignmentId,
            Long adminId,
            AssetReturnRequest request
    ) {

        // 1. Check whether the assignment exists
        AssetAssignment assignment =
                assetAssignmentRepository.findById(assignmentId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Asset assignment not found"
                                )
                        );

        // 2. Check whether Admin exists
        User admin =
                userRepository.findById(adminId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Admin not found"
                                )
                        );

        // 3. Check whether the user is really an ADMIN
        if (admin.getRole() == null ||
                !"ADMIN".equalsIgnoreCase(
                        admin.getRole().getName()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only an Admin can return assets"
            );
        }

        // 4. Check whether assignment is still ACTIVE
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only ACTIVE assignments can be returned"
            );
        }

        // 5. Get the assigned Asset
        Asset asset =
                assetRepository.findById(
                                assignment.getAsset().getId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Asset not found"
                                )
                        );

        // 6. Update assignment
        assignment.setStatus(AssignmentStatus.RETURNED);
        assignment.setReturnedDate(
                java.time.LocalDate.now()
        );
        assignment.setReturnNote(
                request.getReturnNote()
        );

        // 7. Save assignment
        AssetAssignment savedAssignment =
                assetAssignmentRepository.save(assignment);

        // 8. Change Asset status back to AVAILABLE
        asset.setStatus(AssetStatus.AVAILABLE);
        assetRepository.save(asset);

        // 9. Return response
        return toAssignmentResponse(savedAssignment);
    }
    // FUNCTION 9 — Admin Views Assignment History

    @Transactional(readOnly = true)
    public List<AssetAssignmentResponse> getAssignmentHistory() {

        List<AssetAssignment> assignments =
                assetAssignmentRepository.findAll();

        return assignments.stream()
                .map(this::toAssignmentResponse)
                .toList();
    }
// Convert AssetAssignment Entity → Response DTO

    private AssetAssignmentResponse toAssignmentResponse(
            AssetAssignment assignment
    ) {

        return new AssetAssignmentResponse(
                assignment.getId(),
                assignment.getAsset().getId(),
                assignment.getEmployee().getId(),
                assignment.getAssignedDate(),
                assignment.getReturnedDate(),
                assignment.getStatus(),
                assignment.getAssignedBy() != null
                        ? assignment.getAssignedBy().getId()
                        : null
        );
    }

    // Convert Entity → Response DTO

    private AssetRequestResponse toResponse(
            AssetRequest request
    ) {

        return new AssetRequestResponse(
                request.getId(),
                request.getEmployee().getId(),
                request.getAsset() != null
                        ? request.getAsset().getId()
                        : null,
                request.getRequestDate(),
                request.getReason(),
                request.getStatus(),
                request.getApprovedBy() != null
                        ? request.getApprovedBy().getId()
                        : null,
                request.getApprovedAt(),
                request.getRejectionReason()
        );
    }
}



