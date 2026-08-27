package com.eams.service;

import com.eams.dto.request.CreateDepartmentRequest;
import com.eams.dto.response.DepartmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DepartmentService {
    DepartmentResponse createDepartment(CreateDepartmentRequest request);
    DepartmentResponse getDepartmentById(Long id);
    Page<DepartmentResponse> getAllDepartments(Pageable pageable);
}