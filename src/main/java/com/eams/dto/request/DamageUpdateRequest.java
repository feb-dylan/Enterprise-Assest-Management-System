package com.eams.dto.request;

import com.eams.entity.DamageStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DamageUpdateRequest {

    @NotNull(message = "Damage status is required")
    private DamageStatus status;

    @Size(
            max = 1000,
            message = "Resolution note must not exceed 1000 characters"
    )
    private String resolutionNote;
}