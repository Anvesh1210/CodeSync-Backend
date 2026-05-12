package com.codesync.auth.config;

import com.codesync.auth.entity.AuthProvider;
import com.codesync.auth.entity.User;
import com.codesync.auth.entity.UserRole;
import com.codesync.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs on every application startup and ensures the single admin user exists.
 * If the admin account is already present, this is a no-op.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@codesync.com}")
    private String adminEmail;

    @Value("${admin.password:Admin@123}")
    private String adminPassword;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.fullName:CodeSync Admin}")
    private String adminFullName;

    @Override
    public void run(String... args) {
        String normalizedEmail = adminEmail.trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.info("Admin user [{}] already exists — skipping seed.", normalizedEmail);
            return;
        }

        User admin = User.builder()
                .username(adminUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .fullName(adminFullName)
                .role(UserRole.ADMIN)
                .provider(AuthProvider.LOCAL)
                .isActive(true)   // Admin is pre-verified; no OTP needed
                .build();

        userRepository.save(admin);
        log.info("========================================================");
        log.info("Admin user created: email={}, username={}", normalizedEmail, adminUsername);
        log.info("========================================================");
    }
}
