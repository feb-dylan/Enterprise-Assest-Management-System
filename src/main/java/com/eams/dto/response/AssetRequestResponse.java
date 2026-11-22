package com.eams.dto.response;

import com.eams.entity.AssignmentStatus;
import com.eams.entity.RequestPriority;
import com.eams.entity.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Same JSON fields as before (id, employeeId, assetId, requestDate, reason, status,
 * assignmentStatus, approvedBy, approvedAt, rejectionReason) plus readable names,
 * so the frontend no longer has to show "Employee #12" / "User #5".
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetRequestResponse {
    private Long id;

    private Long employeeId;
    private String employeeCode;
    private String employeeName;

    private Long assetId;
    private String assetCode;
    private String assetName;

    private Long categoryId;
    private String categoryName;

    private LocalDateTime requestDate;
    private String reason;
    private RequestStatus status;
    private AssignmentStatus assignmentStatus;
    private RequestPriority priority;
    private LocalDate neededBy;

    private Long approvedBy;
    private String approvedByEmail;
    private LocalDateTime approvedAt;
    private String rejectionReason;
}
