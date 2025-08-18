package com.sarthak.BizNex.bootstrap;

import com.sarthak.BizNex.entity.User;
import com.sarthak.BizNex.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates an initial ADMIN account on first application startup when the user table is empty.
 * Credentials must be provided via environment variables:
 *  - APP_BOOTSTRAP_ADMIN_USER (default: admin)
 *  - APP_BOOTSTRAP_ADMIN_PASSWORD (REQUIRED – no default, for security)
 *  - APP_BOOTSTRAP_ADMIN_EMAIL (default: <user>@example.com)
 *  - APP_BOOTSTRAP_ADMIN_CONTACT (default: 9999999999)
 * The created admin will have mustChangePassword=true to force rotation.
 */
@Component
public class InitialAdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminBootstrapRunner.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialAdminBootstrapRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long existing = userRepository.count();
        if (existing > 0) {
            return; // already provisioned
        }

        String username = getEnvOrDefault("APP_BOOTSTRAP_ADMIN_USER", "admin").toLowerCase();
        String rawPassword = System.getenv("APP_BOOTSTRAP_ADMIN_PASSWORD");
        if (rawPassword == null || rawPassword.isBlank()) {
            log.warn("No users exist but APP_BOOTSTRAP_ADMIN_PASSWORD not set - initial admin NOT created. Set env vars and restart.");
            return; // abort to avoid unknown password account
        }
        if (rawPassword.length() < 8) {
            log.warn("Provided APP_BOOTSTRAP_ADMIN_PASSWORD is too short (<8). Initial admin NOT created.");
            return;
        }
        String email = getEnvOrDefault("APP_BOOTSTRAP_ADMIN_EMAIL", username + "@example.com");
        String contact = getEnvOrDefault("APP_BOOTSTRAP_ADMIN_CONTACT", "9999999999");

        User admin = new User(username, email);
        admin.setUserPassword(passwordEncoder.encode(rawPassword));
        admin.setUserRole(User.UserRole.ADMIN);
        admin.setUserContact(contact);
        admin.setUserSalary(0);
        admin.setMustChangePassword(true);

        try {
            userRepository.save(admin);
            log.info("Initial admin user created username='{}' email='{}' mustChangePassword=true", username, email);
        } catch (DataIntegrityViolationException e) {
            // Possible race with another instance doing the same
            log.info("Initial admin creation skipped due to concurrent creation ({}).", e.getClass().getSimpleName());
        }
    }

    private String getEnvOrDefault(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}

