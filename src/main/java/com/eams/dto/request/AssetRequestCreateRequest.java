package com.eams.dto.request;

import com.eams.entity.RequestPriority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * An employee asks either for a specific asset (assetId)
 * or for "any asset of this category" (categoryId).
 *
 * employeeId is IGNORED - the employee is taken from the login token.
 * (The field is kept only so older frontend code does not break.)
 */
@Getter
@Setter
@NoArgsConstructor
public class AssetRequestCreateRequest {

    @Deprecated
    private Long employeeId;

    private Long assetId;

    private Long categoryId;

    @NotBlank(message = "Reason is required")
    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;

    private RequestPriority priority;

    @FutureOrPresent(message = "Needed-by date cannot be in the past")
    private LocalDate neededBy;

    @AssertTrue(message = "Either assetId or categoryId is required")
    public boolean isTargetPresent() {
        return assetId != null || categoryId != null;
    }
}
