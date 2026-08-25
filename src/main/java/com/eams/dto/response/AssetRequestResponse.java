package com.eams.dto.response;

import com.eams.entity.RequestStatus;

import java.time.LocalDateTime;

public class AssetRequestResponse {

    private Long id;
    private Long employeeId;
    private Long assetId;
    private LocalDateTime requestDate;
    private String reason;
    private RequestStatus status;

    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;

    public AssetRequestResponse(
            Long id,
            Long employeeId,
            Long assetId,
            LocalDateTime requestDate,
            String reason,
            RequestStatus status,
            Long approvedBy,
            LocalDateTime approvedAt,
            String rejectionReason
    ) {
        this.id = id;
        this.employeeId = employeeId;
        this.assetId = assetId;
        this.requestDate = requestDate;
        this.reason = reason;
        this.status = status;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.rejectionReason = rejectionReason;
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public String getReason() {
        return reason;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}


/*package com.eams.dto.response;

import com.eams.entity.RequestStatus;

import java.time.LocalDateTime;

public class AssetRequestResponse {

    private Long id;
    private Long employeeId;
    private Long assetId;
    private LocalDateTime requestDate;
    private String reason;
    private RequestStatus status;

    public AssetRequestResponse(
            Long id,
            Long employeeId,
            Long assetId,
            LocalDateTime requestDate,
            String reason,
            RequestStatus status
    ) {
        this.id = id;
        this.employeeId = employeeId;
        this.assetId = assetId;
        this.requestDate = requestDate;
        this.reason = reason;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public String getReason() {
        return reason;
    }

    public RequestStatus getStatus() {
        return status;
    }
} */