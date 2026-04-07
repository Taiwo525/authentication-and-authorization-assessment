package com.assessment.sample.config;

import com.assessment.sample.domain.entity.Role;
import com.assessment.sample.domain.entity.User;
import com.assessment.sample.domain.repository.RoleRepository;
import com.assessment.sample.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initData(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() > 0) {
                log.info("Users already exist, skipping data initialization");
                return;
            }

            log.info("Initializing demo users...");

            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));

            User john = new User("john", passwordEncoder.encode("password123"));
            john.addRole(userRole);
            userRepository.save(john);
            log.info("Created user: john with ROLE_USER");

            User admin = new User("admin", passwordEncoder.encode("admin123"));
            admin.addRole(userRole);
            admin.addRole(adminRole);
            userRepository.save(admin);
            log.info("Created user: admin with ROLE_USER, ROLE_ADMIN");

            log.info("Demo users initialized successfully");
        };
    }
}
