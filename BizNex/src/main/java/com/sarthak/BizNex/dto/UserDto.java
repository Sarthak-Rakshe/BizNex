package com.sarthak.BizNex.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private String username;
    private String userEmail;
    private String userRole; // keep as String for JSON simplicity
    private String userContact;
    private double userSalary;

    public UserDto(@NotBlank String username, @NotBlank String userRole) {
        this.username = username;
        this.userRole = userRole;
    }
}
