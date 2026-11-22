package com.eams.controller;

import com.eams.dto.request.AssetRequestCreateRequest;
import com.eams.dto.request.AssetRequestRejectRequest;
import com.eams.dto.request.AssetReturnRequest;
import com.eams.dto.response.AssetAssignmentResponse;
import com.eams.dto.response.AssetRequestResponse;
import com.eams.service.AssetRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Request -> approve -> assign -> return.
 *
 * NEW clean URLs take no user ids (the user comes from the JWT).
 * OLD URLs with {employeeId}/{managerId}/{adminId} still work so the current
 * frontend keeps running, but:
 *   - an {employeeId} must be the caller's own id (otherwise 403)
 *   - {managerId}/{adminId} are ignored; the logged-in user is recorded
 */
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class AssetRequestController {

    private final AssetRequestService service;

    // ---------------- EMPLOYEE ----------------

    @PostMapping
    public ResponseEntity<AssetRequestResponse> createRequest(
            @Valid @RequestBody AssetRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRequest(request));
    }

    @GetMapping({"/my", "/my/{employeeId}"})
    public ResponseEntity<List<AssetRequestResponse>> getMyRequests(
            @PathVariable(required = false) Long employeeId) {
        return ResponseEntity.ok(service.getMyRequests(employeeId));
    }

    @GetMapping({"/assignments/my", "/assignments/my/{employeeId}"})
    public ResponseEntity<List<AssetAssignmentResponse>> getMyAssignedAssets(
            @PathVariable(required = false) Long employeeId) {
        return ResponseEntity.ok(service.getMyAssignedAssets(employeeId));
    }

    @PutMapping({
            "/assignments/{assignmentId}/return-request",
            "/assignments/{assignmentId}/return-request/{employeeId}"})
    public ResponseEntity<AssetAssignmentResponse> requestAssetReturn(
            @PathVariable Long assignmentId,
            @PathVariable(required = false) Long employeeId,
            @Valid @RequestBody(required = false) AssetReturnRequest request) {
        return ResponseEntity.ok(service.requestAssetReturn(assignmentId, employeeId, request));
    }

    // ---------------- MANAGER ----------------

    @GetMapping("/pending")
    public ResponseEntity<List<AssetRequestResponse>> getPendingRequests() {
        return ResponseEntity.ok(service.getPendingRequests());
    }

    @PutMapping({"/{requestId}/approve", "/{requestId}/approve/{ignoredManagerId}"})
    public ResponseEntity<AssetRequestResponse> approveRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(service.approveRequest(requestId));
    }

    @PutMapping({"/{requestId}/reject", "/{requestId}/reject/{ignoredManagerId}"})
    public ResponseEntity<AssetRequestResponse> rejectRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody AssetRequestRejectRequest request) {
        return ResponseEntity.ok(service.rejectRequest(requestId, request));
    }

    // ---------------- ADMIN ----------------

    @GetMapping
    public ResponseEntity<List<AssetRequestResponse>> getAllRequests() {
        return ResponseEntity.ok(service.getAllRequests());
    }

    @GetMapping("/approved")
    public ResponseEntity<List<AssetRequestResponse>> getApprovedRequests() {
        return ResponseEntity.ok(service.getApprovedRequests());
    }

    /** For category requests pass ?assetId= to choose which unit to hand out. */
    @PutMapping({"/{requestId}/assign", "/{requestId}/assign/{ignoredAdminId}"})
    public ResponseEntity<AssetAssignmentResponse> assignAsset(
            @PathVariable Long requestId,
            @RequestParam(required = false) Long assetId) {
        return ResponseEntity.ok(service.assignAsset(requestId, assetId));
    }

    @PutMapping({
            "/assignments/{assignmentId}/return",
            "/assignments/{assignmentId}/return/{ignoredAdminId}"})
    public ResponseEntity<AssetAssignmentResponse> returnAsset(
            @PathVariable Long assignmentId,
            @Valid @RequestBody(required = false) AssetReturnRequest request) {
        return ResponseEntity.ok(service.returnAsset(assignmentId, request));
    }

    @GetMapping("/assignments/history")
    public ResponseEntity<List<AssetAssignmentResponse>> getAssignmentHistory() {
        return ResponseEntity.ok(service.getAssignmentHistory());
    }

    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<AssetAssignmentResponse> getAssignmentById(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(service.getAssignmentById(assignmentId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetRequestResponse> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getRequestById(id));
    }
}
