package com.eams.service;

import com.eams.dto.request.DepartmentRequest;
import com.eams.dto.response.DepartmentResponse;
import com.eams.dto.response.EmployeeResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse createDepartment(
            DepartmentRequest request
    );

    List<DepartmentResponse> getAllDepartments();

    DepartmentResponse getDepartmentById(
            Long id
    );

    Page<EmployeeResponse> getEmployeesByDepartment(
            Long departmentId,
            int page,
            int size
    );

    DepartmentResponse updateDepartment(
            Long id,
            DepartmentRequest request
    );

    void deleteDepartment(
            Long id
    );
}