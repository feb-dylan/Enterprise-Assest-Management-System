package com.eams.dto.request;

import com.eams.entity.EmployeeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EmployeeRequest {

    private Long userId;

    @NotBlank(message = "Employee code is required")
    @Size(max = 50, message = "Employee code must not exceed 50 characters")
    private String employeeCode;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Size(max = 30, message = "Phone number must not exceed 30 characters")
    private String phone;

    @NotNull(message = "Department is required")
    private Long departmentId;

    @Size(max = 100, message = "Position must not exceed 100 characters")
    private String position;

    private LocalDate hireDate;

    private EmployeeStatus status;
}