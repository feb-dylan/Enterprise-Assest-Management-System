package com.eams.dto.request;

import com.eams.entity.AuditScanResult;
import com.eams.entity.DisposalMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Small request bodies for the new endpoints, grouped in one file.
 */
public final class Requests {

    private Requests() {
    }

    public record ForgotPasswordRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "Token is required")
            String token,
            @NotBlank(message = "New password is required")
            @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
            String newPassword) {}

    public record InviteUserRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,
            @NotBlank(message = "Role is required")
            @Pattern(regexp = "ADMIN|MANAGER|EMPLOYEE|TECHNICIAN",
                    message = "Role must be ADMIN, MANAGER, EMPLOYEE or TECHNICIAN")
            String role) {}

    public record UpdateUserRequest(
            Boolean enabled,
            @Pattern(regexp = "ADMIN|MANAGER|EMPLOYEE|TECHNICIAN",
                    message = "Role must be ADMIN, MANAGER, EMPLOYEE or TECHNICIAN")
            String role) {}

    public record RetireAssetRequest(
            @NotNull(message = "Disposal method is required")
            DisposalMethod disposalMethod,
            @Size(max = 500, message = "Note must not exceed 500 characters")
            String note) {}

    public record AuditCampaignRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 150)
            String name,
            @Size(max = 255)
            String location) {}

    public record AuditScanRequest(
            @NotBlank(message = "Asset code is required")
            String assetCode,
            AuditScanResult result,
            @Size(max = 255)
            String scannedLocation,
            @Size(max = 500)
            String note) {}

    public record AssistantRequest(
            @NotBlank(message = "Question is required")
            @Size(max = 1000, message = "Question must not exceed 1000 characters")
            String question) {}
}
