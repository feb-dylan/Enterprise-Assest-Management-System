package com.eams.service;

import com.eams.dto.response.DashboardResponse;

public interface DashboardService {

    DashboardResponse getAdminDashboard();

    DashboardResponse getManagerDashboard();

    DashboardResponse getEmployeeDashboard(
            Long employeeId
    );

    DashboardResponse getTechnicianDashboard();
}