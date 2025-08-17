package com.sarthak.BizNex.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import com.sarthak.BizNex.entity.User;

@Getter
@Setter
public class UserRegistrationRequest {

    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String userEmail;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String userPassword;

    @NotNull
    private User.UserRole userRole; // enum ADMIN / USER

    @NotBlank
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be 10 digits")
    private String userContact;

    private double userSalary;

}
