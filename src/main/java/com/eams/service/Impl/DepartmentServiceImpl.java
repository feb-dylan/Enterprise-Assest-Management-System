package com.eams.service.impl;

import com.eams.dto.request.DepartmentRequest;
import com.eams.dto.response.DepartmentResponse;
import com.eams.dto.response.EmployeeResponse;
import com.eams.entity.Department;
import com.eams.repository.DepartmentRepository;
import com.eams.repository.EmployeeRepository;
import com.eams.service.DepartmentService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentServiceImpl(
            DepartmentRepository departmentRepository,
            EmployeeRepository employeeRepository) {

        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public DepartmentResponse createDepartment(
            DepartmentRequest request) {

        String name = request.getName().trim();

        if (departmentRepository.existsByName(name)) {
            throw new IllegalArgumentException(
                    "Department name already exists"
            );
        }

        Department department = new Department(
                name,
                request.getDescription()
        );

        Department savedDepartment =
                departmentRepository.save(department);

        return mapToResponse(savedDepartment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {

        return departmentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {

        Department department =
                departmentRepository.findById(id)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Department not found with id: " + id
                                )
                        );

        return mapToResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getEmployeesByDepartment(
            Long departmentId,
            int page,
            int size) {

        validatePageAndSize(page, size);

        // Make sure the department exists
        departmentRepository.findById(departmentId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Department not found with id: " + departmentId
                        )
                );

        Pageable pageable = PageRequest.of(page, size);

        return employeeRepository
                .findByDepartmentId(departmentId, pageable)
                .map(this::mapEmployeeToResponse);
    }

    @Override
    public DepartmentResponse updateDepartment(
            Long id,
            DepartmentRequest request) {

        Department department =
                departmentRepository.findById(id)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Department not found with id: " + id
                                )
                        );

        String name = request.getName().trim();

        departmentRepository.findByName(name)
                .filter(existing ->
                        !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Department name already exists"
                    );
                });

        department.setName(name);
        department.setDescription(request.getDescription());

        Department updatedDepartment =
                departmentRepository.save(department);

        return mapToResponse(updatedDepartment);
    }

    @Override
    public void deleteDepartment(Long id) {

        // 1. Check whether department exists
        departmentRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Department not found with id: " + id
                        )
                );

        // 2. Check whether employees are assigned
        if (employeeRepository.existsByDepartmentId(id)) {

            throw new IllegalArgumentException(
                    "Cannot delete this department because it has employees assigned to it."
            );
        }

        // 3. Delete department
        departmentRepository.deleteById(id);
    }

    private DepartmentResponse mapToResponse(
            Department department) {

        DepartmentResponse response =
                new DepartmentResponse();

        response.setId(department.getId());
        response.setName(department.getName());
        response.setDescription(department.getDescription());
        response.setCreatedAt(department.getCreatedAt());
        response.setUpdatedAt(department.getUpdatedAt());

        return response;
    }

    private EmployeeResponse mapEmployeeToResponse(
            com.eams.entity.Employee employee) {

        EmployeeResponse response =
                new EmployeeResponse();

        response.setId(employee.getId());

        if (employee.getUser() != null) {
            response.setUserId(employee.getUser().getId());
        }

        response.setEmployeeCode(employee.getEmployeeCode());
        response.setFirstName(employee.getFirstName());
        response.setLastName(employee.getLastName());
        response.setPhone(employee.getPhone());

        if (employee.getDepartment() != null) {
            response.setDepartmentId(
                    employee.getDepartment().getId()
            );

            response.setDepartmentName(
                    employee.getDepartment().getName()
            );
        }

        response.setPosition(employee.getPosition());
        response.setHireDate(employee.getHireDate());
        response.setStatus(employee.getStatus());
        response.setCreatedAt(employee.getCreatedAt());
        response.setUpdatedAt(employee.getUpdatedAt());

        return response;
    }

    private void validatePageAndSize(int page, int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Size must be between 1 and 100"
            );
        }
    }
}