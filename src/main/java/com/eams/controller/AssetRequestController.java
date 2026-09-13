package com.eams.controller;

import com.eams.dto.request.AssetRequestCreateRequest;
import com.eams.dto.request.AssetRequestRejectRequest;
import com.eams.dto.request.AssetReturnRequest;

import com.eams.dto.response.AssetAssignmentResponse;
import com.eams.dto.response.AssetRequestResponse;

import com.eams.service.AssetRequestService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class AssetRequestController {

    private final AssetRequestService assetRequestService;

    public AssetRequestController(
            AssetRequestService assetRequestService
    ) {
        this.assetRequestService = assetRequestService;
    }

    // =========================================================
    // 1. EMPLOYEE CREATES REQUEST
    // =========================================================

    @PostMapping
    public ResponseEntity<AssetRequestResponse> createRequest(
            @Valid @RequestBody AssetRequestCreateRequest request
    ) {

        AssetRequestResponse response =
                assetRequestService.createRequest(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================
    // 2. EMPLOYEE VIEWS OWN REQUESTS
    // =========================================================

    @GetMapping("/my/{employeeId}")
    public ResponseEntity<List<AssetRequestResponse>> getMyRequests(
            @PathVariable Long employeeId
    ) {

        List<AssetRequestResponse> responses =
                assetRequestService.getMyRequests(employeeId);

        return ResponseEntity.ok(responses);
    }

    // =========================================================
    // 3. EMPLOYEE VIEWS CURRENTLY ASSIGNED ASSETS
    // =========================================================

    @GetMapping("/assignments/my/{employeeId}")
    public ResponseEntity<List<AssetAssignmentResponse>>
    getMyAssignedAssets(
            @PathVariable Long employeeId
    ) {

        List<AssetAssignmentResponse> responses =
                assetRequestService.getMyAssignedAssets(employeeId);

        return ResponseEntity.ok(responses);
    }

    // =========================================================
    // 4. EMPLOYEE REQUESTS ASSET RETURN
    // =========================================================

    @PutMapping(
            "/assignments/{assignmentId}/return-request/{employeeId}"
    )
    public ResponseEntity<AssetAssignmentResponse>
    requestAssetReturn(
            @PathVariable Long assignmentId,
            @PathVariable Long employeeId,
            @Valid @RequestBody AssetReturnRequest request
    ) {

        AssetAssignmentResponse response =
                assetRequestService.requestAssetReturn(
                        assignmentId,
                        employeeId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 5. MANAGER VIEWS PENDING REQUESTS
    // =========================================================

    @GetMapping("/pending")
    public ResponseEntity<List<AssetRequestResponse>>
    getPendingRequests() {

        List<AssetRequestResponse> responses =
                assetRequestService.getPendingRequests();

        return ResponseEntity.ok(responses);
    }

    // =========================================================
    // 6. MANAGER APPROVES REQUEST
    // =========================================================

    @PutMapping("/{requestId}/approve/{managerId}")
    public ResponseEntity<AssetRequestResponse> approveRequest(
            @PathVariable Long requestId,
            @PathVariable Long managerId
    ) {

        AssetRequestResponse response =
                assetRequestService.approveRequest(
                        requestId,
                        managerId
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 7. MANAGER REJECTS REQUEST
    // =========================================================

    @PutMapping("/{requestId}/reject/{managerId}")
    public ResponseEntity<AssetRequestResponse> rejectRequest(
            @PathVariable Long requestId,
            @PathVariable Long managerId,
            @Valid @RequestBody AssetRequestRejectRequest request
    ) {

        AssetRequestResponse response =
                assetRequestService.rejectRequest(
                        requestId,
                        managerId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 8. ADMIN VIEWS APPROVED REQUESTS
    // =========================================================

    @GetMapping("/approved")
    public ResponseEntity<List<AssetRequestResponse>>
    getApprovedRequests() {

        List<AssetRequestResponse> responses =
                assetRequestService.getApprovedRequests();

        return ResponseEntity.ok(responses);
    }

    // =========================================================
    // 9. ADMIN ASSIGNS ASSET
    // =========================================================

    @PutMapping("/{requestId}/assign/{adminId}")
    public ResponseEntity<AssetAssignmentResponse> assignAsset(
            @PathVariable Long requestId,
            @PathVariable Long adminId
    ) {

        AssetAssignmentResponse response =
                assetRequestService.assignAsset(
                        requestId,
                        adminId
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 10. ADMIN RECEIVES RETURNED ASSET
    // =========================================================

    @PutMapping(
            "/assignments/{assignmentId}/return/{adminId}"
    )
    public ResponseEntity<AssetAssignmentResponse> returnAsset(
            @PathVariable Long assignmentId,
            @PathVariable Long adminId,
            @Valid @RequestBody AssetReturnRequest request
    ) {

        AssetAssignmentResponse response =
                assetRequestService.returnAsset(
                        assignmentId,
                        adminId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 11. ADMIN VIEWS ASSIGNMENT HISTORY
    // =========================================================

    @GetMapping("/assignments/history")
    public ResponseEntity<List<AssetAssignmentResponse>>
    getAssignmentHistory() {

        List<AssetAssignmentResponse> responses =
                assetRequestService.getAssignmentHistory();

        return ResponseEntity.ok(responses);
    }

    // =========================================================
    // 12. GET ONE ASSIGNMENT BY ID
    // =========================================================

    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<AssetAssignmentResponse>
    getAssignmentById(
            @PathVariable Long assignmentId
    ) {

        AssetAssignmentResponse response =
                assetRequestService.getAssignmentById(
                        assignmentId
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // 13. GET ONE REQUEST BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<AssetRequestResponse> getRequestById(
            @PathVariable Long id
    ) {

        AssetRequestResponse response =
                assetRequestService.getRequestById(id);

        return ResponseEntity.ok(response);
    }
}