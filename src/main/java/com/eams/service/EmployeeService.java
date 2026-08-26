package com.eams.service;

import com.eams.dto.request.EmployeeRequest;
import com.eams.dto.response.EmployeeResponse;
import org.springframework.data.domain.Page;

public interface EmployeeService {

    EmployeeResponse createEmployee(EmployeeRequest request);

    Page<EmployeeResponse> getAllEmployees(int page, int size);

    EmployeeResponse getEmployeeById(Long id);

    EmployeeResponse updateEmployee(Long id, EmployeeRequest request);

    void deleteEmployee(Long id);

    Page<EmployeeResponse> searchEmployees(
            String keyword,
            int page,
            int size
    );
}