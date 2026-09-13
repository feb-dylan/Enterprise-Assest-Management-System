package com.eams.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    private String reportType;

    private long total;

    private long available;

    private long assigned;

    private long damaged;

    private long maintenance;

    private long retired;

    private long pending;

    private long approved;

    private long rejected;

    private long completed;

    private long inProgress;

    private long cancelled;

    private double totalRepairCost;
}