package com.eams.mapper;

import com.eams.dto.request.CreateDepartmentRequest;
import com.eams.dto.response.DepartmentResponse;
import com.eams.entity.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public Department toEntity(CreateDepartmentRequest request) {
        if (request == null) return null;
        Department department = new Department();
        department.setName(request.getName());
        department.setCode(request.getCode());
        department.setDescription(request.getDescription());
        return department;
    }

    public DepartmentResponse toResponse(Department entity) {
        if (entity == null) return null;
        DepartmentResponse response = new DepartmentResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setCode(entity.getCode());
        response.setDescription(entity.getDescription());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}