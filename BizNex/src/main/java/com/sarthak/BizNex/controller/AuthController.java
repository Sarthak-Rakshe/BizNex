package com.sarthak.BizNex.controller;

import com.sarthak.BizNex.dto.UserDto;
import com.sarthak.BizNex.dto.request.AuthRequest;
import com.sarthak.BizNex.dto.request.UserRegistrationRequest;
import com.sarthak.BizNex.dto.response.AuthResponse;
import com.sarthak.BizNex.entity.User;
import com.sarthak.BizNex.security.JwtTokenProvider;
import com.sarthak.BizNex.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication, login, registration and password assistance")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;


    @PostMapping("/login")
    @Operation(summary = "Login with username & password", description = "Returns JWT access & refresh tokens.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Validation or bad request"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request){
        //Authenticate credentials
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getUserPassword())
        );

        // userService already throws if not found
        User user = userService.getUserByUsername(request.getUsername());

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String token = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
        long expiresAt = jwtTokenProvider.getExpirationEpochMillis(token);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .expireAt(expiresAt)
                .username(user.getUsername())
                .userRole(user.getUserRole().name())
                .build();

        return ResponseEntity.ok(authResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register")
    @Operation(summary = "Register a new user (ADMIN only)", security = @SecurityRequirement(name = "bearer-jwt"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (no/invalid token)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (not admin)"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    public ResponseEntity<UserDto> register(@RequestBody UserRegistrationRequest request){
        User user = userService.registerUser(request);
        return ResponseEntity.ok(new UserDto(
                user.getUsername(),
                user.getUserEmail(),
                user.getUserRole().name(),
                user.getUserContact(),
                user.getUserSalary()
        ));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password placeholder", description = "Returns instruction message.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Instruction returned")
    })
    public ResponseEntity<String> forgotPassword(@RequestParam("username")String username){
        return ResponseEntity.ok("Please contact the administrator to reset your password.");
    }
}
