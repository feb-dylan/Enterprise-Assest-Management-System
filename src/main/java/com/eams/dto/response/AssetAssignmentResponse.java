package com.eams.dto.response;

import com.eams.entity.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetAssignmentResponse {
    private Long id;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private LocalDate assignedDate;
    private LocalDate returnedDate;
    private AssignmentStatus status;
    private Long assignedBy;
    private String assignedByEmail;
    private String returnNote;
}
