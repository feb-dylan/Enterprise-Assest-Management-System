package com.eams.dto.response;

import com.eams.entity.AssignmentStatus;

import java.time.LocalDate;

public class AssetAssignmentResponse {

    private Long id;

    private Long assetId;

    private String assetCode;

    private String assetName;

    private Long employeeId;

    private LocalDate assignedDate;

    private LocalDate returnedDate;

    private AssignmentStatus status;

    private Long assignedBy;

    private String returnNote;

    public AssetAssignmentResponse(
            Long id,
            Long assetId,
            String assetCode,
            String assetName,
            Long employeeId,
            LocalDate assignedDate,
            LocalDate returnedDate,
            AssignmentStatus status,
            Long assignedBy,
            String returnNote
    ) {
        this.id = id;
        this.assetId = assetId;
        this.assetCode = assetCode;
        this.assetName = assetName;
        this.employeeId = employeeId;
        this.assignedDate = assignedDate;
        this.returnedDate = returnedDate;
        this.status = status;
        this.assignedBy = assignedBy;
        this.returnNote = returnNote;
    }

    public Long getId() {
        return id;
    }

    public Long getAssetId() {
        return assetId;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public String getAssetName() {
        return assetName;
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

    public String getReturnNote() {
        return returnNote;
    }
}