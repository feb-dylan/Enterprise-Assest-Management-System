package com.eams.dto.request;

import com.eams.entity.AssetStatus;
import com.eams.entity.DamageStatus;
import com.eams.entity.MaintenanceStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class ReportFilterRequest {
    // Pass 1 ID (?ids=1), multiple IDs (?ids=1,2,5), or leave blank for ALL
    private List<Long> ids;
    
    private Long departmentId;
    private Long categoryId;
    
    private AssetStatus assetStatus;
    private MaintenanceStatus maintenanceStatus;
    private DamageStatus damageStatus;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}