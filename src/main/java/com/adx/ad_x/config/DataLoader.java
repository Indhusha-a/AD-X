package com.adx.ad_x.config;

import com.adx.ad_x.model.*;
import com.adx.ad_x.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private FinancialTransactionRepository financialTransactionRepository;

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

            // Create seller profile
            SellerProfile sellerProfile = new SellerProfile(seller);
            sellerProfile.setBusinessName("John's Advertising Agency");
            sellerProfile.setBusinessDescription("Professional advertising services with 10+ years of experience. Specializing in digital marketing, social media campaigns, and outdoor advertising.");
            sellerProfile.setPhoneNumber("+1 (555) 123-4567");
            sellerProfile.setBusinessAddress("123 Business Ave, Suite 100, New York, NY 10001");
            sellerProfile.setWebsiteUrl("https://johns-ad-agency.com");
            sellerProfile.setIsVerified(true);
            sellerProfile.setTotalRevenue(new BigDecimal("2500.00"));
            sellerProfile.setTotalOrders(8);
            sellerProfile.setAverageRating(new BigDecimal("4.8"));
            sellerProfile.setResponseRate(new BigDecimal("95.5"));
            sellerProfileRepository.save(sellerProfile);
            System.out.println("Seller profile created for John Seller");
        }

        // Create sample products if none exist
        if (productRepository.count() == 0) {
            // Product 1
            Product product1 = new Product();
            product1.setTitle("Social Media Campaign Package");
            product1.setDescription("Boost your brand with a 30-day social media campaign across Instagram, Facebook, and TikTok. Includes content creation, scheduling, and performance analytics.");
            product1.setPrice(new BigDecimal("299.99"));
            product1.setCategory("Social Media");
            product1.setSeller(seller);
            product1.setActive(true);
            product1.setImageUrl("https://via.placeholder.com/300x200?text=Social+Media");
            productRepository.save(product1);

            // Product 2
            Product product2 = new Product();
            product2.setTitle("Billboard Advertising - City Center");
            product2.setDescription("High-visibility billboard in downtown area. 4-week display with digital rotation. Perfect for local business awareness.");
            product2.setPrice(new BigDecimal("899.00"));
            product2.setCategory("Outdoor");
            product2.setSeller(seller);
            product2.setActive(true);
            product2.setImageUrl("https://via.placeholder.com/300x200?text=Billboard");
            productRepository.save(product2);

            // Product 3
            Product product3 = new Product();
            product3.setTitle("Google Ads Management Service");
            product3.setDescription("Professional Google Ads campaign management. Includes keyword research, ad creation, A/B testing, and performance optimization.");
            product3.setPrice(new BigDecimal("499.99"));
            product3.setCategory("Digital Marketing");
            product3.setSeller(seller);
            product3.setActive(true);
            product3.setImageUrl("https://via.placeholder.com/300x200?text=Google+Ads");
            productRepository.save(product3);

            // Product 4
            Product product4 = new Product();
            product4.setTitle("Email Marketing Campaign");
            product4.setDescription("Complete email marketing solution including list building, template design, automation setup, and performance tracking.");
            product4.setPrice(new BigDecimal("199.99"));
            product4.setCategory("Email Marketing");
            product4.setSeller(seller);
            product4.setActive(true);
            product4.setImageUrl("https://via.placeholder.com/300x200?text=Email+Marketing");
            productRepository.save(product4);

            System.out.println("4 sample products created for testing");
        }

        // Create a sample buyer for testing
        User buyer = userRepository.findByEmail("buyer@example.com").orElse(null);
        if (buyer == null) {
            buyer = new User();
            buyer.setFirstName("Alice");
            buyer.setLastName("Buyer");
            buyer.setEmail("buyer@example.com");
            buyer.setPassword("buyer123");
            buyer.setRole("BUYER");
            buyer = userRepository.save(buyer);
            System.out.println("Sample buyer created: buyer@example.com / buyer123");

            // Create sample orders for testing
            createSampleOrders(buyer, seller);

            // Create sample inquiries for testing
            createSampleInquiries(buyer, seller);

            // Create sample favorites for testing
            createSampleFavorites(buyer);
        }

        // Create additional seller for testing
        User seller2 = userRepository.findByEmail("sarah@marketingpros.com").orElse(null);
        if (seller2 == null) {
            seller2 = new User();
            seller2.setFirstName("Sarah");
            seller2.setLastName("Johnson");
            seller2.setEmail("sarah@marketingpros.com");
            seller2.setPassword("seller123");
            seller2.setRole("SELLER");
            seller2 = userRepository.save(seller2);

            SellerProfile sellerProfile2 = new SellerProfile(seller2);
            sellerProfile2.setBusinessName("Marketing Pros Digital Agency");
            sellerProfile2.setBusinessDescription("Award-winning digital marketing agency specializing in SEO, content marketing, and conversion optimization.");
            sellerProfile2.setPhoneNumber("+1 (555) 987-6543");
            sellerProfile2.setWebsiteUrl("https://marketingpros.com");
            sellerProfile2.setIsVerified(true);
            sellerProfile2.setTotalRevenue(new BigDecimal("1800.00"));
            sellerProfile2.setTotalOrders(5);
            sellerProfile2.setAverageRating(new BigDecimal("4.9"));
            sellerProfile2.setResponseRate(new BigDecimal("98.2"));
            sellerProfileRepository.save(sellerProfile2);

            // Add products for second seller
            Product product5 = new Product();
            product5.setTitle("SEO Optimization Package");
            product5.setDescription("Comprehensive SEO audit and optimization service. Improve your search engine rankings and organic traffic.");
            product5.setPrice(new BigDecimal("799.99"));
            product5.setCategory("SEO Services");
            product5.setSeller(seller2);
            product5.setActive(true);
            productRepository.save(product5);

            System.out.println("Second seller created: sarah@marketingpros.com / seller123");
        }
    }

    private void createSampleOrders(User buyer, User seller) {
        try {
            // Get some products to create orders for
            Product socialMediaProduct = productRepository.findByTitleContainingIgnoreCaseAndActiveTrue("Social Media").get(0);
            Product googleAdsProduct = productRepository.findByTitleContainingIgnoreCaseAndActiveTrue("Google Ads").get(0);

            // Order 1 - Confirmed
            Order order1 = new Order();
            order1.setBuyer(buyer);
            order1.setStatus("CONFIRMED");
            order1.setTotalAmount(socialMediaProduct.getPrice());
            order1.setCreatedAt(LocalDateTime.now().minusDays(2));

            OrderItem item1 = new OrderItem();
            item1.setProduct(socialMediaProduct);
            item1.setQuantity(1);
            item1.setPrice(socialMediaProduct.getPrice());
            item1.setOrder(order1);

            order1.getItems().add(item1);
            orderRepository.save(order1);

            // Create payment for order1
            createPaymentTestData(buyer, seller, order1);

            // Order 2 - Pending
            Order order2 = new Order();
            order2.setBuyer(buyer);
            order2.setStatus("PENDING");
            order2.setTotalAmount(googleAdsProduct.getPrice());
            order2.setCreatedAt(LocalDateTime.now().minusDays(1));

            OrderItem item2 = new OrderItem();
            item2.setProduct(googleAdsProduct);
            item2.setQuantity(1);
            item2.setPrice(googleAdsProduct.getPrice());
            item2.setOrder(order2);

            order2.getItems().add(item2);
            orderRepository.save(order2);

            System.out.println("Sample orders created for testing");
        } catch (Exception e) {
            System.out.println("Error creating sample orders: " + e.getMessage());
        }
    }

    private void createPaymentTestData(User buyer, User seller, Order order) {
        try {
            // Create completed payment using the default constructor and setters
            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setBuyer(buyer);
            payment.setAmount(order.getTotalAmount());
            payment.setPaymentMethod("CREDIT_CARD");
            payment.setStatus("COMPLETED");
            payment.setTransactionId("TXN_" + System.currentTimeMillis());
            payment.setCreatedAt(LocalDateTime.now().minusDays(2));
            paymentRepository.save(payment);

            // Link payment to order
            order.setPayment(payment);
            orderRepository.save(order);

            // Create financial transaction if repository exists
            if (financialTransactionRepository != null) {
                try {
                    FinancialTransaction transaction = new FinancialTransaction();
                    transaction.setType("PAYMENT"); // FIXED: Use setType() instead of setTransactionType()
                    transaction.setAmount(order.getTotalAmount());
                    transaction.setUser(buyer);
                    transaction.setDescription("Payment for Order #" + order.getId());
                    transaction.setPayment(payment);
                    transaction.setCreatedAt(LocalDateTime.now().minusDays(2));
                    transaction.setTransactionDate(LocalDateTime.now().minusDays(2));
                    financialTransactionRepository.save(transaction);
                } catch (Exception e) {
                    System.out.println("Financial transaction creation skipped: " + e.getMessage());
                }
            }

            // Create payout for seller using the default constructor and setters
            Payout payout = new Payout();
            payout.setSeller(seller);
            payout.setAmount(order.getTotalAmount().multiply(new BigDecimal("0.90"))); // 90% to seller, 10% commission
            payout.setPayoutMethod("BANK_TRANSFER");
            payout.setStatus("COMPLETED");
            payout.setCreatedAt(LocalDateTime.now().minusDays(1));
            payout.setProcessedAt(LocalDateTime.now().minusDays(1));
            payoutRepository.save(payout);

            System.out.println("Payment test data created for Order #" + order.getId());
        } catch (Exception e) {
            System.out.println("Error creating payment test data: " + e.getMessage());
        }
    }

    private void createSampleInquiries(User buyer, User seller) {
        try {
            // Get products for inquiries
            Product billboardProduct = productRepository.findByTitleContainingIgnoreCaseAndActiveTrue("Billboard").get(0);
            Product emailProduct = productRepository.findByTitleContainingIgnoreCaseAndActiveTrue("Email Marketing").get(0);

            // Inquiry 1 - Read
            Inquiry inquiry1 = new Inquiry();
            inquiry1.setBuyer(buyer);
            inquiry1.setSeller(seller);
            inquiry1.setProduct(billboardProduct);
            inquiry1.setMessage("Hello, I'm interested in your billboard advertising service. Can you provide more details about the location and visibility? Also, do you offer any discounts for longer-term contracts?");
            inquiry1.setIsRead(true);
            inquiry1.setCreatedAt(LocalDateTime.now().minusDays(3));
            inquiryRepository.save(inquiry1);

            // Inquiry 2 - Unread
            Inquiry inquiry2 = new Inquiry();
            inquiry2.setBuyer(buyer);
            inquiry2.setSeller(seller);
            inquiry2.setProduct(emailProduct);
            inquiry2.setMessage("Hi there, I'd like to know more about your email marketing campaign service. What's included in the automation setup and do you provide templates?");
            inquiry2.setIsRead(false);
            inquiry2.setCreatedAt(LocalDateTime.now().minusHours(5));
            inquiryRepository.save(inquiry2);

            System.out.println("Sample inquiries created for testing");
        } catch (Exception e) {
            System.out.println("Error creating sample inquiries: " + e.getMessage());
        }
    }

    private void createSampleFavorites(User buyer) {
        try {
            // Get products for favorites
            Product socialMediaProduct = productRepository.findByTitleContainingIgnoreCaseAndActiveTrue("Social Media").get(0);
            Product billboardProduct = productRepository.findByTitleContainingIgnoreCaseAndActiveTrue("Billboard").get(0);

            // Favorite 1
            Favorite favorite1 = new Favorite();
            favorite1.setUser(buyer);
            favorite1.setProduct(socialMediaProduct);
            favorite1.setCreatedAt(LocalDateTime.now().minusDays(5));
            favoriteRepository.save(favorite1);

            // Favorite 2
            Favorite favorite2 = new Favorite();
            favorite2.setUser(buyer);
            favorite2.setProduct(billboardProduct);
            favorite2.setCreatedAt(LocalDateTime.now().minusDays(2));
            favoriteRepository.save(favorite2);

            System.out.println("Sample favorites created for testing");
        } catch (Exception e) {
            System.out.println("Error creating sample favorites: " + e.getMessage());
        }
    }
}