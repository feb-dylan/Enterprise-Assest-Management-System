package com.eams.service;

import com.eams.dto.request.AssetRequestCreateRequest;
import com.eams.dto.request.AssetRequestRejectRequest;
import com.eams.dto.request.AssetReturnRequest;
import com.eams.dto.response.AssetAssignmentResponse;
import com.eams.dto.response.AssetRequestResponse;

import java.util.List;

/**
 * Request -> approve -> assign -> return workflow.
 *
 * The acting user (employee, manager, admin) is ALWAYS taken from the JWT.
 * Methods that accept an employeeId only use it for backward-compatible URLs,
 * and it must match the logged-in employee.
 */
public interface AssetRequestService {

    AssetRequestResponse createRequest(AssetRequestCreateRequest request);

    List<AssetRequestResponse> getMyRequests(Long employeeIdFromPath);

    List<AssetAssignmentResponse> getMyAssignedAssets(Long employeeIdFromPath);

    AssetAssignmentResponse requestAssetReturn(Long assignmentId, Long employeeIdFromPath,
                                               AssetReturnRequest request);

    List<AssetRequestResponse> getPendingRequests();

    List<AssetRequestResponse> getApprovedRequests();

    List<AssetRequestResponse> getAllRequests();

    AssetRequestResponse approveRequest(Long requestId);

    AssetRequestResponse rejectRequest(Long requestId, AssetRequestRejectRequest request);

    /**
     * @param assetId required for category-based requests (the admin picks the unit);
     *                ignored / must match for requests that name a specific asset
     */
    AssetAssignmentResponse assignAsset(Long requestId, Long assetId);

    AssetAssignmentResponse returnAsset(Long assignmentId, AssetReturnRequest request);

    List<AssetAssignmentResponse> getAssignmentHistory();

    AssetRequestResponse getRequestById(Long id);

    AssetAssignmentResponse getAssignmentById(Long assignmentId);
}
