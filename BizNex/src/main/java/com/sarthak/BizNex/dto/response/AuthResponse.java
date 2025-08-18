package com.sarthak.BizNex.dto.response;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expireAt;
    private String username;
    private String userRole;
    private boolean mustChangePassword; // new flag for client enforcement
}
