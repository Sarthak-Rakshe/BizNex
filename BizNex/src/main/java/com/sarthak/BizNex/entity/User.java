package com.sarthak.BizNex.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;


    @NotBlank
    @Column(nullable = false, unique = true,updatable = false)
    private String username;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true, updatable = false)
    private String userEmail;

    @Setter
    @NotBlank
    @JsonIgnore
    @Column(nullable = false)
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String userPassword;

    public enum UserRole {
        ADMIN, USER
    }

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private UserRole userRole; // enum role

    @Setter
    @NotBlank
    @Column(nullable = false, unique = true)
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be 10 digits")
    private String userContact;

    @Setter
    private double userSalary;

    public User(String username, String userEmail){
        this.username = username;
        this.userEmail = userEmail;
    }

}
