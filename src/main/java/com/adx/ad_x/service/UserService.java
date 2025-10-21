package com.adx.ad_x.service;

import com.adx.ad_x.model.*;
import com.adx.ad_x.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private ProductReviewRepository productReviewRepository;

    @Autowired
    private SellerAnalyticsRepository sellerAnalyticsRepository;

    @Autowired
    private FinancialTransactionRepository financialTransactionRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User updateUser(Long id, User userDetails) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            User existingUser = user.get();
            existingUser.setFirstName(userDetails.getFirstName());
            existingUser.setLastName(userDetails.getLastName());
            existingUser.setEmail(userDetails.getEmail());

            // REMOVED: existingUser.setRole(userDetails.getRole());
            // We don't update role during profile edits to avoid null errors

            // Only update password if provided (optional)
            if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                existingUser.setPassword(userDetails.getPassword());
            }

            return userRepository.save(existingUser);
        }
        return null;
    }

    @Transactional
    public void deleteUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Delete all related data to avoid foreign key constraint violations
            
            // Delete product reviews (both as buyer and seller)
            List<ProductReview> buyerReviews = productReviewRepository.findByBuyerAndActiveTrueOrderByCreatedAtDesc(user);
            for (ProductReview review : buyerReviews) {
                productReviewRepository.delete(review);
            }
            
            // Delete all reviews (including inactive ones) by this buyer
            List<ProductReview> allReviews = productReviewRepository.findAll();
            for (ProductReview review : allReviews) {
                if ((review.getBuyer() != null && review.getBuyer().getId().equals(user.getId())) ||
                    (review.getSeller() != null && review.getSeller().getId().equals(user.getId()))) {
                    productReviewRepository.delete(review);
                }
            }
            
            // Delete financial transactions
            List<FinancialTransaction> userTransactions = financialTransactionRepository.findByUserOrderByTransactionDateDesc(user);
            for (FinancialTransaction transaction : userTransactions) {
                financialTransactionRepository.delete(transaction);
            }
            
            // Delete notifications (all, active and inactive)
            List<Notification> allNotifications = notificationRepository.findAll();
            for (Notification notification : allNotifications) {
                if (notification.getUser() != null && notification.getUser().getId().equals(user.getId())) {
                    notificationRepository.delete(notification);
                }
            }
            
            // Delete favorites
            List<Favorite> userFavorites = favoriteRepository.findByUser(user);
            for (Favorite favorite : userFavorites) {
                favoriteRepository.delete(favorite);
            }
            
            // Delete inquiries (both as buyer and seller)
            List<Inquiry> buyerInquiries = inquiryRepository.findByBuyerOrderByCreatedAtDesc(user);
            for (Inquiry inquiry : buyerInquiries) {
                inquiryRepository.delete(inquiry);
            }
            List<Inquiry> sellerInquiries = inquiryRepository.findBySellerOrderByCreatedAtDesc(user);
            for (Inquiry inquiry : sellerInquiries) {
                inquiryRepository.delete(inquiry);
            }
            
            // Delete orders and payments for buyer
            List<Order> allOrders = orderRepository.findAll();
            for (Order order : allOrders) {
                if (order.getBuyer() != null && order.getBuyer().getId().equals(user.getId())) {
                    // Delete payments for this order first
                    List<Payment> allPayments = paymentRepository.findAll();
                    for (Payment payment : allPayments) {
                        if (payment.getOrder() != null && payment.getOrder().getId().equals(order.getId())) {
                            paymentRepository.delete(payment);
                        }
                    }
                    // Delete the order
                    orderRepository.delete(order);
                }
            }
            
            // Delete any remaining payments by this buyer
            List<Payment> buyerPayments = paymentRepository.findAll();
            for (Payment payment : buyerPayments) {
                if (payment.getBuyer() != null && payment.getBuyer().getId().equals(user.getId())) {
                    paymentRepository.delete(payment);
                }
            }
            
            // Delete seller-specific data if seller
            if ("SELLER".equals(user.getRole())) {
                // Delete seller analytics
                List<SellerAnalytics> sellerAnalytics = sellerAnalyticsRepository.findBySellerOrderByAnalyticsDateDesc(user);
                for (SellerAnalytics analytics : sellerAnalytics) {
                    sellerAnalyticsRepository.delete(analytics);
                }
                
                // Delete payouts
                List<Payout> allPayouts = payoutRepository.findAll();
                for (Payout payout : allPayouts) {
                    if (payout.getSeller() != null && payout.getSeller().getId().equals(user.getId())) {
                        payoutRepository.delete(payout);
                    }
                }
                
                // Get all seller's products first
                List<Product> sellerProducts = productRepository.findBySellerAndActiveTrue(user);
                List<Product> allProducts = productRepository.findAll();
                for (Product product : allProducts) {
                    if (product.getSeller() != null && product.getSeller().getId().equals(user.getId())) {
                        if (!sellerProducts.contains(product)) {
                            sellerProducts.add(product);
                        }
                    }
                }
                
                // Delete orders and payments that contain seller's products
                // We need to delete orders containing seller's products to avoid FK constraint violations
                List<Order> allOrdersForSeller = orderRepository.findAll();
                for (Order order : allOrdersForSeller) {
                    boolean containsSellerProduct = false;
                    for (OrderItem item : order.getItems()) {
                        if (item.getProduct() != null && 
                            item.getProduct().getSeller() != null && 
                            item.getProduct().getSeller().getId().equals(user.getId())) {
                            containsSellerProduct = true;
                            break;
                        }
                    }
                    
                    if (containsSellerProduct) {
                        // Delete payments for this order first
                        List<Payment> orderPayments = paymentRepository.findAll();
                        for (Payment payment : orderPayments) {
                            if (payment.getOrder() != null && payment.getOrder().getId().equals(order.getId())) {
                                paymentRepository.delete(payment);
                            }
                        }
                        // Delete the order (cascade will delete order items)
                        orderRepository.delete(order);
                    }
                }
                
                // Now it's safe to delete products
                for (Product product : sellerProducts) {
                    productRepository.delete(product);
                }
                
                // Delete seller profile
                Optional<SellerProfile> sellerProfile = sellerProfileRepository.findByUser(user);
                if (sellerProfile.isPresent()) {
                    sellerProfileRepository.delete(sellerProfile.get());
                }
            }
            
            // Finally, delete the user
            userRepository.delete(user);
        }
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getUsersByRole(String role) {
        return userRepository.findByRole(role);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public Long getUserCount() {
        return userRepository.count();
    }


}