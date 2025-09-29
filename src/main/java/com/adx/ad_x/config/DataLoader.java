package com.adx.ad_x.config;

import com.adx.ad_x.model.Product;
import com.adx.ad_x.model.User;
import com.adx.ad_x.repository.ProductRepository;
import com.adx.ad_x.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        // Create default admin user if not exists
        if (userRepository.findByEmail("admin@adx.com").isEmpty()) {
            User admin = new User();
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setEmail("admin@adx.com");
            admin.setPassword("admin123");
            admin.setRole("ADMIN");
            userRepository.save(admin);
            System.out.println("Default admin user created: admin@adx.com / admin123");
        }

        // Create sample seller user if not exists
        User seller = userRepository.findByEmail("seller@example.com").orElse(null);
        if (seller == null) {
            seller = new User();
            seller.setFirstName("John");
            seller.setLastName("Seller");
            seller.setEmail("seller@example.com");
            seller.setPassword("seller123");
            seller.setRole("SELLER");
            seller = userRepository.save(seller);
            System.out.println("Sample seller created: seller@example.com / seller123");
        }

        // Create 2 sample products if none exist
        if (productRepository.count() == 0) {
            Product product1 = new Product(
                    "Social Media Campaign Package",
                    "Boost your brand with a 30-day social media campaign across Instagram, Facebook, and TikTok. Includes content creation, scheduling, and performance analytics.",
                    new BigDecimal("299.99"),
                    "Social Media",
                    seller
            );
            product1.setImageUrl("https://via.placeholder.com/300x200?text=Social+Media");
            productRepository.save(product1);

            Product product2 = new Product(
                    "Billboard Advertising - City Center",
                    "High-visibility billboard in downtown area. 4-week display with digital rotation. Perfect for local business awareness.",
                    new BigDecimal("899.00"),
                    "Outdoor",
                    seller
            );
            product2.setImageUrl("https://via.placeholder.com/300x200?text=Billboard");
            productRepository.save(product2);

            System.out.println("2 sample products created for testing Buyer Interface");
        }
    }
}