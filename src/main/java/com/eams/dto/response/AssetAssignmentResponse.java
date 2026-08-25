package com.eams.dto.response;

import com.eams.entity.AssignmentStatus;

import java.time.LocalDate;

public class AssetAssignmentResponse {

    private Long id;
    private Long assetId;
    private Long employeeId;
    private LocalDate assignedDate;
    private LocalDate returnedDate;
    private AssignmentStatus status;
    private Long assignedBy;

    public AssetAssignmentResponse(
            Long id,
            Long assetId,
            Long employeeId,
            LocalDate assignedDate,
            LocalDate returnedDate,
            AssignmentStatus status,
            Long assignedBy
    ) {
        this.id = id;
        this.assetId = assetId;
        this.employeeId = employeeId;
        this.assignedDate = assignedDate;
        this.returnedDate = returnedDate;
        this.status = status;
        this.assignedBy = assignedBy;
    }

    public Long getId() {
        return id;
    }

    public Long getAssetId() {
        return assetId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public LocalDate getAssignedDate() {
        return assignedDate;
    }

    public LocalDate getReturnedDate() {
        return returnedDate;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public Long getAssignedBy() {
        return assignedBy;
    }
}

