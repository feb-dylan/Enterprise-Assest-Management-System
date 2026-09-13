package com.eams.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeProfileRequest {

    @Size(max = 30)
    private String phone;

    @Size(max = 100)
    private String position;
}