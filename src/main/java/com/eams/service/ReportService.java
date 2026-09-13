package com.eams.service;

import com.eams.dto.response.ReportResponse;

public interface ReportService {

    ReportResponse getAssetReport();

    ReportResponse getRequestReport();

    ReportResponse getMaintenanceReport();

    ReportResponse getDamageReport();

    ReportResponse getAssignmentReport();
}