package com.adx.ad_x.config;

import com.adx.ad_x.model.User;
import com.adx.ad_x.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        // Create default admin user if not exists
        if (userRepository.findByEmail("admin@adx.com").isEmpty()) {
            User admin = new User();
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setEmail("admin@adx.com");
            admin.setPassword("admin123"); // Plain text password
            admin.setRole("ADMIN");

            userRepository.save(admin);
            System.out.println("Default admin user created: admin@adx.com / admin123");
        }
    }
}