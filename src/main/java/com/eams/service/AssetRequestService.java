package com.eams.service;

import com.eams.dto.request.AssetRequestCreateRequest;
import com.eams.dto.request.AssetRequestRejectRequest;
import com.eams.dto.request.AssetReturnRequest;
import com.eams.dto.response.AssetAssignmentResponse;
import com.eams.dto.response.AssetRequestResponse;

import java.util.List;

public interface AssetRequestService {

    // 1. Employee creates an asset request
    AssetRequestResponse createRequest(AssetRequestCreateRequest request);

    // 2. Employee views own requests
    List<AssetRequestResponse> getMyRequests(Long employeeId);

    // 3. Manager views pending requests
    List<AssetRequestResponse> getPendingRequests();

    // 4. Manager approves request
    AssetRequestResponse approveRequest(
            Long requestId,
            Long managerId
    );

    // 5. Manager rejects request
    AssetRequestResponse rejectRequest(
            Long requestId,
            Long managerId,
            AssetRequestRejectRequest request
    );

    // 6. Admin views approved requests
    List<AssetRequestResponse> getApprovedRequests();

    // 7. Admin assigns asset
    AssetAssignmentResponse assignAsset(
            Long requestId,
            Long adminId
    );

    // 8. Employee requests asset return
    AssetAssignmentResponse requestAssetReturn(
            Long assignmentId,
            Long employeeId,
            AssetReturnRequest request
    );

    // 9. Admin receives returned asset
    AssetAssignmentResponse returnAsset(
            Long assignmentId,
            Long adminId,
            AssetReturnRequest request
    );

    // 10. Admin views assignment history
    List<AssetAssignmentResponse> getAssignmentHistory();

    // 11. Get one request by ID
    AssetRequestResponse getRequestById(Long id);

    // 12. Employee views currently assigned assets
    List<AssetAssignmentResponse> getMyAssignedAssets(
            Long employeeId
    );

    // 13. Get one assignment by ID
    AssetAssignmentResponse getAssignmentById(
            Long assignmentId
    );
}