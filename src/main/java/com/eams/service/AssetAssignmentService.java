package com.eams.service;

import com.eams.dto.request.AssetAssignmentRequest;
import com.eams.entity.AssetAssignment;

public interface AssetAssignmentService {
    AssetAssignment createAssignment(AssetAssignmentRequest request);
}