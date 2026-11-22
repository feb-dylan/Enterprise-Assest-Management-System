package com.eams.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * employeeId is IGNORED - the reporter is taken from the login token.
 * Photos are uploaded afterwards with POST /api/damage/{id}/photos.
 */
@Getter
@Setter
@NoArgsConstructor
public class DamageCreateRequest {

    @Deprecated
    private Long employeeId;

    @NotNull(message = "Asset ID is required")
    private Long assetId;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Size(max = 500, message = "Evidence URL must not exceed 500 characters")
    private String evidenceUrl;
}
