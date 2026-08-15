package com.eams.service.impl;

import com.eams.dto.request.EmployeeRequest;
import com.eams.dto.response.EmployeeResponse;
import com.eams.entity.Department;
import com.eams.entity.Employee;
import com.eams.entity.User;
import com.eams.repository.DepartmentRepository;
import com.eams.repository.EmployeeRepository;
import com.eams.repository.UserRepository;
import com.eams.service.EmployeeService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository) {

        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    public EmployeeResponse createEmployee(EmployeeRequest request) {

        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new IllegalArgumentException(
                    "Employee code already exists"
            );
        }

        Department department = departmentRepository.findById(
                request.getDepartmentId()
        ).orElseThrow(() ->
                new EntityNotFoundException("Department not found")
        );

        Employee employee = new Employee(
                request.getEmployeeCode(),
                request.getFirstName(),
                request.getLastName(),
                department
        );

        employee.setPhone(request.getPhone());
        employee.setPosition(request.getPosition());
        employee.setHireDate(request.getHireDate());

        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
        }

        if (request.getUserId() != null) {

            if (employeeRepository.existsByUserId(request.getUserId())) {
                throw new IllegalArgumentException(
                        "User is already linked to another employee"
                );
            }

            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() ->
                            new EntityNotFoundException("User not found")
                    );

            employee.setUser(user);
        }

        Employee savedEmployee = employeeRepository.save(employee);

        return mapToResponse(savedEmployee);
    }

    @Override
    public Page<EmployeeResponse> getAllEmployees(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        return employeeRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public EmployeeResponse getEmployeeById(Long id) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Employee not found")
                );

        return mapToResponse(employee);
    }

    @Override
    public EmployeeResponse updateEmployee(
            Long id,
            EmployeeRequest request) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Employee not found")
                );

        employeeRepository.findByEmployeeCode(request.getEmployeeCode())
                .filter(existing ->
                        !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Employee code already exists"
                    );
                });

        Department department = departmentRepository.findById(
                request.getDepartmentId()
        ).orElseThrow(() ->
                new EntityNotFoundException("Department not found")
        );

        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setPhone(request.getPhone());
        employee.setDepartment(department);
        employee.setPosition(request.getPosition());
        employee.setHireDate(request.getHireDate());

        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
        }

        if (request.getUserId() != null) {

            employeeRepository.findByUserId(request.getUserId())
                    .filter(existing ->
                            !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException(
                                "User is already linked to another employee"
                        );
                    });

            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() ->
                            new EntityNotFoundException("User not found")
                    );

            employee.setUser(user);

        } else {
            employee.setUser(null);
        }

        Employee updatedEmployee =
                employeeRepository.save(employee);

        return mapToResponse(updatedEmployee);
    }

    @Override
    public void deleteEmployee(Long id) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Employee not found")
                );

        employeeRepository.delete(employee);
    }

    @Override
    public Page<EmployeeResponse> searchEmployees(
            String keyword,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size);

        return employeeRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCase(
                        keyword,
                        keyword,
                        keyword,
                        pageable
                )
                .map(this::mapToResponse);
    }

    private EmployeeResponse mapToResponse(Employee employee) {

        EmployeeResponse response = new EmployeeResponse();

        response.setId(employee.getId());

        if (employee.getUser() != null) {
            response.setUserId(employee.getUser().getId());
        }

        response.setEmployeeCode(employee.getEmployeeCode());
        response.setFirstName(employee.getFirstName());
        response.setLastName(employee.getLastName());
        response.setPhone(employee.getPhone());

        response.setDepartmentId(
                employee.getDepartment().getId()
        );

        response.setDepartmentName(
                employee.getDepartment().getName()
        );

        response.setPosition(employee.getPosition());
        response.setHireDate(employee.getHireDate());
        response.setStatus(employee.getStatus());
        response.setCreatedAt(employee.getCreatedAt());
        response.setUpdatedAt(employee.getUpdatedAt());

        return response;
    }
}