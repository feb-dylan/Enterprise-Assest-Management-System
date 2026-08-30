package com.eams.dto.response;

import com.eams.entity.EmployeeStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class EmployeeResponse {

    private Long id;

    private Long userId;

    private String employeeCode;

    private String firstName;

    private String lastName;

    private String phone;

    private Long departmentId;

    private String departmentName;

    private String position;

    private LocalDate hireDate;

    private EmployeeStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}