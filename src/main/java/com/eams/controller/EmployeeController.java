package com.eams.controller;

import com.eams.dto.request.EmployeeProfileRequest;
import com.eams.dto.request.EmployeeRequest;
import com.eams.dto.response.EmployeeResponse;
import com.eams.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeRequest request) {

        EmployeeResponse response =
                employeeService.createEmployee(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<EmployeeResponse>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                employeeService.getAllEmployees(page, size)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<Page<EmployeeResponse>> searchEmployees(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                employeeService.searchEmployees(keyword, page, size)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> getCurrentEmployee() {
        return ResponseEntity.ok(
                employeeService.getCurrentEmployee()
        );
    }

    @PostMapping("/me")
    public ResponseEntity<EmployeeResponse> createCurrentEmployee(
            @Valid @RequestBody EmployeeRequest request) {

        EmployeeResponse response =
                employeeService.createCurrentEmployee(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================
    // PROFILE — update own profile
    // =========================================================

    @PutMapping("/me/profile")
    public ResponseEntity<EmployeeResponse> updateCurrentEmployee(
            @Valid @RequestBody EmployeeProfileRequest request) {

        return ResponseEntity.ok(
                employeeService.updateCurrentEmployee(request)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                employeeService.getEmployeeById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request) {

        return ResponseEntity.ok(
                employeeService.updateEmployee(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {

        employeeService.deleteEmployee(id);

        return ResponseEntity.noContent().build();
    }
}