package com.xsh.trueused.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.xsh.trueused.entity.Role;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.enums.UserStatus;
import com.xsh.trueused.repository.RoleRepository;
import com.xsh.trueused.repository.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDefaultData(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.username:admin}") String adminUsername,
            @Value("${app.bootstrap.admin.email:admin@example.com}") String adminEmail,
            @Value("${app.bootstrap.admin.password:Admin@123}") String adminPassword) {
        return args -> {
            // Create roles if not exists
            Role roleUser = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
                Role r = new Role();
                r.setName("ROLE_USER");
                r.setDescription("普通用户");
                return roleRepository.save(r);
            });
            Role roleAdmin = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
                Role r = new Role();
                r.setName("ROLE_ADMIN");
                r.setDescription("管理员");
                return roleRepository.save(r);
            });

            // Create admin user if not exists
            userRepository.findByUsername(adminUsername).orElseGet(() -> {
                User admin = new User();
                admin.setUsername(adminUsername);
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setStatus(UserStatus.ACTIVE);
                admin.getRoles().add(roleAdmin);
                admin.getRoles().add(roleUser);
                return userRepository.save(admin);
            });
        };
    }
}
