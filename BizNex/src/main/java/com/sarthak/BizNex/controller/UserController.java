package com.sarthak.BizNex.controller;

import com.sarthak.BizNex.dto.UserDto;
import com.sarthak.BizNex.dto.request.AdminPasswordUpdateRequest;
import com.sarthak.BizNex.dto.response.AdminPasswordUpdateResponse;
import com.sarthak.BizNex.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Administrative user management endpoints. All operations restricted to ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Administrative user management")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Return all users (ADMIN only). */
    @GetMapping()
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /** Update a password securely (hashing handled in service). */
    @PatchMapping("/{userName}/password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a user's password", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<String> updatePassword(
            @PathVariable String userName,
            @RequestBody @Valid AdminPasswordUpdateRequest request) {
        AdminPasswordUpdateResponse response = userService.updatePassword(userName, request);
        return ResponseEntity.ok(response.getStatus());
    }

    /**
     * Delete a user by username. Idempotent: deleting a non-existent user still returns 204.
     * Guard prevents removing the last remaining ADMIN.
     */
    @DeleteMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a user by username", description = "Idempotent; returns 204 even if user already absent. Fails if attempting to remove last admin.", security = @SecurityRequirement(name = "bearer-jwt"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted (idempotent)"),
            @ApiResponse(responseCode = "409", description = "Cannot delete last remaining admin"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Void> deleteUserByUsername(@PathVariable String username) {
        userService.deleteUserByUsername(username);
        return ResponseEntity.noContent().build();
    }
}
