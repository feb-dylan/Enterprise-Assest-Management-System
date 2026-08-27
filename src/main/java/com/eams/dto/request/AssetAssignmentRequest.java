package com.eams.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AssetAssignmentRequest {
    private Long assetId;
    private Long employeeId;
    private Long assignedById;
    private LocalDate assignedDate;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
    private String notes;
    private String status;
}