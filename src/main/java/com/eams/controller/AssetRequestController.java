package com.eams.controller;

import com.eams.dto.request.AssetRequestCreateRequest;
import com.eams.dto.request.AssetRequestRejectRequest;
import com.eams.dto.response.AssetRequestResponse;
import com.eams.service.AssetRequestService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


import com.eams.dto.response.AssetAssignmentResponse;

import com.eams.dto.request.AssetReturnRequest;

@RestController
@RequestMapping("/api/asset-requests")
public class AssetRequestController {

    private final AssetRequestService assetRequestService;

    public AssetRequestController(
            AssetRequestService assetRequestService
    ) {
        this.assetRequestService = assetRequestService;
    }

    // FUNCTION 1 — Employee Creates Request

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

    // FUNCTION 2 — Employee Views My Requests

    @GetMapping("/my/{employeeId}")
    public ResponseEntity<List<AssetRequestResponse>> getMyRequests(
            @PathVariable Long employeeId
    ) {

        List<AssetRequestResponse> responses =
                assetRequestService.getMyRequests(employeeId);

        return ResponseEntity.ok(responses);
    }

    // FUNCTION 3 — Manager Views Pending Requests

    @GetMapping("/pending")
    public ResponseEntity<List<AssetRequestResponse>> getPendingRequests() {

        List<AssetRequestResponse> responses =
                assetRequestService.getPendingRequests();

        return ResponseEntity.ok(responses);
    }

    // FUNCTION 4 — Manager Approves Request

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

    // FUNCTION 5 — Manager Rejects Request

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

// FUNCTION 6 — Admin Views Approved Requests

    @GetMapping("/approved")
    public ResponseEntity<List<AssetRequestResponse>> getApprovedRequests() {

        List<AssetRequestResponse> responses =
                assetRequestService.getApprovedRequests();

        return ResponseEntity.ok(responses);
    }

// FUNCTION 7 — Admin Assign Asset

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
// FUNCTION 8 — Admin Returns Asset

    @PutMapping("/assignments/{assignmentId}/return/{adminId}")
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
// FUNCTION 9 — Admin Views Assignment History

    @GetMapping("/assignments/history")
    public ResponseEntity<List<AssetAssignmentResponse>> getAssignmentHistory() {

        List<AssetAssignmentResponse> responses =
                assetRequestService.getAssignmentHistory();

        return ResponseEntity.ok(responses);
    }

}