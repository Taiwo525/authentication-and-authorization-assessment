package com.assessment.sample;

import com.assessment.sample.domain.entity.Role;
import com.assessment.sample.domain.entity.User;
import com.assessment.sample.domain.repository.RoleRepository;
import com.assessment.sample.domain.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class TestDataInitializer {

    @Bean
    @Order(1)
    CommandLineRunner initTestData(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // Only initialize if no users exist (Flyway may have already seeded)
            if (userRepository.count() > 0) {
                return;
            }

            // Create roles
            Role userRole = roleRepository.save(new Role("ROLE_USER"));
            Role adminRole = roleRepository.save(new Role("ROLE_ADMIN"));

            // Create john with ROLE_USER
            User john = new User("john", passwordEncoder.encode("password123"));
            john.addRole(userRole);
            userRepository.save(john);

            // Create admin with ROLE_USER and ROLE_ADMIN
            User admin = new User("admin", passwordEncoder.encode("admin123"));
            admin.addRole(userRole);
            admin.addRole(adminRole);
            userRepository.save(admin);
        };
    }
}
