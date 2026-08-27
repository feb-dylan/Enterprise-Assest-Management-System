package com.eams.dto.request;

import com.eams.entity.DamageStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDamageStatusRequest {

    @NotNull(message = "Damage status is required")
    private DamageStatus status;
}