package com.chatroom.config;

import com.chatroom.entity.User;
import com.chatroom.entity.Role;
import com.chatroom.repository.UserRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        // Create a default SUPER_ADMIN if none exists
        boolean hasSuperAdmin = userRepository.findAll().stream()
                .anyMatch(u -> u.getRole() == Role.SUPER_ADMIN);
        Optional<User> superOpt = userRepository.findByUsername("superadmin");
        if (!hasSuperAdmin) {
            if (superOpt.isPresent()) {
                User admin = superOpt.get();
                admin.setRole(Role.SUPER_ADMIN);
                admin.setEnabled(true);
                userRepository.save(admin);
                System.out.println("Restored SUPER_ADMIN role for existing user 'superadmin'");
            } else {
                User admin = new User();
                admin.setUsername("superadmin");
                admin.setPassword("admin123"); // NOTE: plain text for now; consider hashing later
                admin.setRole(Role.SUPER_ADMIN);
                admin.setEnabled(true);
                userRepository.save(admin);
                System.out.println("Created default SUPER_ADMIN: username=superadmin, password=admin123");
            }
        } else {
            // Ensure existing SUPER_ADMIN (e.g., 'superadmin') is enabled after schema change
            superOpt.ifPresent(admin -> {
                if (!Boolean.TRUE.equals(admin.isEnabled())) {
                    admin.setEnabled(true);
                    userRepository.save(admin);
                    System.out.println("Enabled existing SUPER_ADMIN user: superadmin");
                }
            });
        }
        // Normalize existing users: assign missing roles ONLY; do not override enabled flag
        for (User u : userRepository.findAll()) {
            if (u.getRole() == null) {
                u.setRole(Role.USER);
                userRepository.save(u);
                System.out.println("Normalized user role: username=" + u.getUsername() + ", role=" + u.getRole());
            }
        }
    }
}