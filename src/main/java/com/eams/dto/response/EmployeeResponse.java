package com.eams.dto.response;

import com.eams.entity.EmployeeStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmployeeResponse {
    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Long departmentId;
    private String departmentName;
    private String jobTitle;
    private EmployeeStatus status;
    private LocalDateTime createdAt;
}