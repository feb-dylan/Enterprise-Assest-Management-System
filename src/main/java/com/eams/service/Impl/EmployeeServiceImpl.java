package com.eams.service.impl;

import com.eams.dto.request.EmployeeProfileRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Override
    public EmployeeResponse createCurrentEmployee(EmployeeRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found: " + email
                ));

        if (employeeRepository.existsByUserId(user.getId())) {
            throw new IllegalArgumentException(
                    "Employee profile already exists for this user"
            );
        }

        String employeeCode = request.getEmployeeCode().trim();

        if (employeeRepository.existsByEmployeeCode(employeeCode)) {
            throw new IllegalArgumentException(
                    "Employee code already exists"
            );
        }

        Department department = departmentRepository
                .findById(request.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Department not found: " + request.getDepartmentId()
                ));

        Employee employee = new Employee(
                employeeCode,
                request.getFirstName().trim(),
                request.getLastName().trim(),
                department
        );

        employee.setUser(user);
        employee.setPhone(request.getPhone());
        employee.setPosition(request.getPosition());
        employee.setHireDate(request.getHireDate());

        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
        }

        Employee savedEmployee = employeeRepository.save(employee);

        return mapToResponse(savedEmployee);
    }

    @Override
    public EmployeeResponse createEmployee(EmployeeRequest request) {

        String employeeCode = request.getEmployeeCode().trim();

        if (employeeRepository.existsByEmployeeCode(employeeCode)) {
            throw new IllegalArgumentException(
                    "Employee code already exists"
            );
        }

        Department department = departmentRepository
                .findById(request.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Department not found: " + request.getDepartmentId()
                ));

        Employee employee = new Employee(
                employeeCode,
                request.getFirstName().trim(),
                request.getLastName().trim(),
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
                    .orElseThrow(() -> new EntityNotFoundException(
                            "User not found: " + request.getUserId()
                    ));

            employee.setUser(user);
        }

        Employee savedEmployee = employeeRepository.save(employee);

        return mapToResponse(savedEmployee);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployees(
            int page,
            int size) {

        validatePageAndSize(page, size);

        Pageable pageable = PageRequest.of(page, size);

        return employeeRepository
                .findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employee not found: " + id
                ));

        return mapToResponse(employee);
    }

    @Override
    public EmployeeResponse updateEmployee(
            Long id,
            EmployeeRequest request) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employee not found: " + id
                ));

        String employeeCode = request.getEmployeeCode().trim();

        employeeRepository.findByEmployeeCode(employeeCode)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Employee code already exists"
                    );
                });

        Department department = departmentRepository
                .findById(request.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Department not found: " + request.getDepartmentId()
                ));

        employee.setEmployeeCode(employeeCode);
        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName().trim());
        employee.setPhone(request.getPhone());
        employee.setDepartment(department);
        employee.setPosition(request.getPosition());
        employee.setHireDate(request.getHireDate());

        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
        }

        if (request.getUserId() != null) {

            employeeRepository.findByUserId(request.getUserId())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException(
                                "User is already linked to another employee"
                        );
                    });

            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "User not found: " + request.getUserId()
                    ));

            employee.setUser(user);

        } else {
            employee.setUser(null);
        }

        return mapToResponse(employee);
    }

    @Override
    public void deleteEmployee(Long id) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employee not found: " + id
                ));

        employeeRepository.delete(employee);
    }

    // =========================================================
    // SEARCH EMPLOYEES
    // Supports:
    // - First name
    // - Last name
    // - Employee code
    // - First name + last name
    // - Last name + first name
    // - Case-insensitive search
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> searchEmployees(
            String keyword,
            int page,
            int size) {

        validatePageAndSize(page, size);

        String searchKeyword =
                keyword == null ? "" : keyword.trim();

        Pageable pageable = PageRequest.of(page, size);

        return employeeRepository
                .searchEmployees(searchKeyword, pageable)
                .map(this::mapToResponse);
    }

    // =========================================================
    // CURRENT EMPLOYEE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getCurrentEmployee() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        Employee employee = employeeRepository.findByUserEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employee profile not found for user: " + email
                ));

        return mapToResponse(employee);
    }

    @Override
    public EmployeeResponse updateCurrentEmployee(
            EmployeeProfileRequest request) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Employee employee = employeeRepository.findByUserEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employee profile not found for user: " + email
                ));

        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone());
        }

        if (request.getPosition() != null) {
            employee.setPosition(request.getPosition());
        }

        employeeRepository.save(employee);

        return mapToResponse(employee);
    }

    // =========================================================
    // MAPPER
    // =========================================================

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