package com.eams.controller;

import com.eams.dto.request.Requests.InviteUserRequest;
import com.eams.dto.request.Requests.UpdateUserRequest;
import com.eams.dto.response.Responses.UserResponse;
import com.eams.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin user management (create managers / technicians / admins by invitation). */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserAdminService userAdminService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userAdminService.listUsers());
    }

    /** For the "assign technician" dropdown (admin + technicians). */
    @GetMapping("/technicians")
    public ResponseEntity<List<UserResponse>> technicians() {
        return ResponseEntity.ok(userAdminService.listByRole("TECHNICIAN"));
    }

    /**
     * Creates the account and emails a "set your password" link (valid 7 days).
     * The link is also returned so an admin can share it when email is not configured.
     */
    @PostMapping("/invite")
    public ResponseEntity<UserAdminService.InviteResult> invite(
            @Valid @RequestBody InviteUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userAdminService.invite(request));
    }

    /** Enable/disable an account or change its role. */
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userAdminService.update(id, request));
    }
}
