package com.chatroom.config;

import com.chatroom.entity.User;
import com.chatroom.entity.Role;
import com.chatroom.repository.UserRepository;
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
        if (!hasSuperAdmin) {
            if (userRepository.findByUsername("superadmin").isEmpty()) {
                User admin = new User();
                admin.setUsername("superadmin");
                admin.setPassword("admin123"); // NOTE: plain text for now; consider hashing later
                admin.setRole(Role.SUPER_ADMIN);
                admin.setEnabled(true);
                userRepository.save(admin);
                System.out.println("Created default SUPER_ADMIN: username=superadmin, password=admin123");
            }
        }
    }
}