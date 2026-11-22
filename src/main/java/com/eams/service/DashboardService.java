package com.eams.service;

import com.eams.dto.response.DashboardResponse;

public interface DashboardService {

    DashboardResponse getAdminDashboard();

    DashboardResponse getManagerDashboard();

    /** Dashboard of the logged-in employee (the path id, if given, must be their own). */
    DashboardResponse getEmployeeDashboard(Long employeeIdFromPath);

    /** Technician dashboard; "my jobs" counts are for the logged-in technician. */
    DashboardResponse getTechnicianDashboard();
}
