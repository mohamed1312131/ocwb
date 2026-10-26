package com.omnicare.config;

import com.omnicare.model.User;
import com.omnicare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.first-name:Admin}")
    private String adminFirstName;

    @Value("${app.admin.last-name:OmniCare}")
    private String adminLastName;

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() && adminPassword.isBlank()) {
            return;
        }

        if (adminEmail.isBlank() || adminPassword.length() < 6) {
            throw new IllegalStateException(
                "ADMIN_EMAIL and an ADMIN_PASSWORD of at least 6 characters are required to bootstrap an admin"
            );
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();
        User existingUser = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (existingUser != null) {
            boolean changed = false;

            if (existingUser.getRole() != User.Role.ADMIN) {
                existingUser.setRole(User.Role.ADMIN);
                changed = true;
            }
            if (!Boolean.TRUE.equals(existingUser.getEnabled())) {
                existingUser.setEnabled(true);
                changed = true;
            }
            if (!passwordEncoder.matches(adminPassword, existingUser.getPassword())) {
                existingUser.setPassword(passwordEncoder.encode(adminPassword));
                changed = true;
            }

            if (changed) {
                existingUser.setUpdatedAt(LocalDateTime.now());
                userRepository.save(existingUser);
                log.info("Synchronized admin account {} from environment configuration", normalizedEmail);
            } else {
                log.info("Admin account {} is already configured", normalizedEmail);
            }
            return;
        }

        User admin = new User();
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setFirstName(adminFirstName);
        admin.setLastName(adminLastName);
        admin.setRole(User.Role.ADMIN);
        admin.setEnabled(true);
        admin.setCreatedAt(LocalDateTime.now());
        userRepository.save(admin);

        log.info("Bootstrapped admin account {}", normalizedEmail);
    }
}
