package com.sarthak.BizNex.service.impl;

import com.sarthak.BizNex.dto.UserDto;
import com.sarthak.BizNex.dto.request.AdminPasswordUpdateRequest;
import com.sarthak.BizNex.dto.request.UserRegistrationRequest;
import com.sarthak.BizNex.dto.response.AdminPasswordUpdateResponse;
import com.sarthak.BizNex.entity.User;
import com.sarthak.BizNex.exception.DuplicateEntityException;
import com.sarthak.BizNex.exception.EntityNotFoundException;
import com.sarthak.BizNex.exception.WeakPasswordException;
import com.sarthak.BizNex.repository.UserRepository;
import com.sarthak.BizNex.service.UserService;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImp implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImp.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImp(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User registerUser(UserRegistrationRequest request) {
        String username = request.getUsername().toLowerCase();
        String userEmail = request.getUserEmail();

        if(userRepository.existsByUsername(username)){
            throw new DuplicateEntityException("Username already exists" );
        }

        User user = new User(username,userEmail);
        user.setUserPassword(passwordEncoder.encode(request.getUserPassword()));
        user.setUserContact(request.getUserContact());
        user.setUserRole(request.getUserRole());
        user.setUserSalary(request.getUserSalary());
        user.setMustChangePassword(false); // normal registrations do not require forced change

        return userRepository.save(user);

    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username.toLowerCase())
                .orElseThrow(() -> new EntityNotFoundException("User not found with the username"));
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> new UserDto(
                        user.getUsername(),
                        user.getUserEmail(),
                        user.getUserRole().name(),
                        user.getUserContact(),
                        user.getUserSalary()))
                .toList();
    }

    @Override
    public AdminPasswordUpdateResponse updatePassword(String username, AdminPasswordUpdateRequest request) {

        User user = userRepository.findByUsername(username.toLowerCase())
                .orElseThrow(() -> new EntityNotFoundException("User not found with the username"));

        validatePassword(request.getNewPassword());

        user.setUserPassword(passwordEncoder.encode(request.getNewPassword()));
        // Clear forced-change flag after successful update
        if (user.isMustChangePassword()) {
            user.setMustChangePassword(false);
        }
        userRepository.save(user);

        log.info("Password updated for user='{}'", user.getUsername());
        return new AdminPasswordUpdateResponse(user.getUsername(),"Password updated successfully");

    }

    @Override
    public void deleteUserByUsername(String username) {
        String normalized = username.toLowerCase();
        Optional<User> optional = userRepository.findByUsername(normalized);
        if(optional.isEmpty()) {
            log.info("Delete requested for non-existent user='{}' - ignoring", normalized);
            return;
        }
        User user = optional.get();
        if(user.getUserRole() == User.UserRole.ADMIN && userRepository.countByUserRole(User.UserRole.ADMIN) == 1) {
            log.warn("Attempt to delete last remaining admin user='{}' blocked", normalized);
            throw new IllegalStateException("Cannot delete the last remaining admin user");
        }
        userRepository.delete(user);
        log.info("User deleted user='{}' role='{}'", normalized, user.getUserRole());
    }

    @Override
    public boolean selfFirstLoginPasswordChange(String username, String newPassword) {
        User user = userRepository.findByUsername(username.toLowerCase())
                .orElseThrow(() -> new EntityNotFoundException("User not found with the username"));
        if(!user.isMustChangePassword()) {
            return false; // nothing to do
        }
        validatePassword(newPassword);
        user.setUserPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.info("First-login password changed for user='{}'", user.getUsername());
        return true;
    }

    private void validatePassword(@NotBlank String newPassword) {
        if (newPassword.length() < 8
                || newPassword.chars().noneMatch(Character::isUpperCase)
                || newPassword.chars().noneMatch(Character::isLowerCase)
                || newPassword.chars().noneMatch(Character::isDigit)
                || newPassword.chars().noneMatch(c -> "!@#$%^&*()_+-=[]{}|;:,.<>/?".indexOf(c) >= 0)) {
            throw new WeakPasswordException("Weak password");
        }
    }
}
