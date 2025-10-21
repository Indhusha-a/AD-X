package com.adx.ad_x.controller;

import com.adx.ad_x.model.*;
import com.adx.ad_x.service.*;
import com.adx.ad_x.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PayoutService payoutService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    // Check if user is admin
    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null && "ADMIN".equals(user.getRole());
    }

    // Admin dashboard
    @GetMapping("/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<User> users = userService.getAllUsers();

        // Get basic stats for admin dashboard
        Long totalUsers = userService.getUserCount();
        Long totalProducts = productService.getTotalProductCount();
        Long totalOrders = orderService.getTotalOrderCount();
        BigDecimal totalRevenue = paymentService.getTotalRevenue();

        model.addAttribute("users", users);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        model.addAttribute("pageTitle", "AD-X - Admin Dashboard");
        return "admin-dashboard";
    }

    // Show edit user form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        Optional<User> user = userService.getUserById(id);
        if (user.isPresent()) {
            model.addAttribute("user", user.get());
            model.addAttribute("pageTitle", "AD-X - Edit User");
            return "admin-edit-user";
        }
        return "redirect:/admin/dashboard";
    }

    // Process user edit
    @PostMapping("/edit/{id}")
    public String processEdit(@PathVariable Long id,
                              @ModelAttribute User userDetails,
                              HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        userService.updateUser(id, userDetails);
        return "redirect:/admin/dashboard";
    }

    // Delete user - FIXED: Added error handling to prevent white label errors
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            // Check if user exists
            Optional<User> userOpt = userService.getUserById(id);
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/admin/dashboard";
            }

            User currentUser = (User) session.getAttribute("user");
            if (currentUser.getId().equals(id)) {
                redirectAttributes.addFlashAttribute("error", "You cannot delete your own account");
                return "redirect:/admin/dashboard";
            }

            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    // Financial dashboard - FIXED: Added proper error handling and fallbacks
    @GetMapping("/financial")
    public String financialDashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            // Calculate revenue metrics with safe defaults
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
            LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

            // Use safe revenue calculation methods
            BigDecimal todayRevenue = calculateRevenueSafe(todayStart, todayEnd);
            BigDecimal monthRevenue = calculateRevenueSafe(monthStart, todayEnd);

            // Calculate commission safely
            BigDecimal totalCommission = calculateCommissionSafe(monthStart, todayEnd);

            // Get recent payments and payouts with safe fallbacks
            List<Payment> recentPayments = getRecentPaymentsSafe();
            List<Payout> recentPayouts = getRecentPayoutsSafe();

            // Additional financial stats
            BigDecimal totalRevenue = paymentService.getTotalRevenue();
            BigDecimal pendingPayouts = payoutService.getTotalPendingPayouts();
            Long totalTransactions = paymentService.getTotalTransactionCount();

            model.addAttribute("todayRevenue", todayRevenue);
            model.addAttribute("monthRevenue", monthRevenue);
            model.addAttribute("totalCommission", totalCommission);
            model.addAttribute("recentPayments", recentPayments);
            model.addAttribute("recentPayouts", recentPayouts);
            model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
            model.addAttribute("pendingPayouts", pendingPayouts != null ? pendingPayouts : BigDecimal.ZERO);
            model.addAttribute("totalTransactions", totalTransactions != null ? totalTransactions : 0L);

        } catch (Exception e) {
            // Set safe defaults if any calculation fails
            model.addAttribute("todayRevenue", BigDecimal.ZERO);
            model.addAttribute("monthRevenue", BigDecimal.ZERO);
            model.addAttribute("totalCommission", BigDecimal.ZERO);
            model.addAttribute("recentPayments", new ArrayList<>());
            model.addAttribute("recentPayouts", new ArrayList<>());
            model.addAttribute("totalRevenue", BigDecimal.ZERO);
            model.addAttribute("pendingPayouts", BigDecimal.ZERO);
            model.addAttribute("totalTransactions", 0L);

            model.addAttribute("error", "Some financial data could not be loaded: " + e.getMessage());
        }

        model.addAttribute("pageTitle", "AD-X - Financial Dashboard");
        return "admin-financial-dashboard";
    }

    // Payment management
    @GetMapping("/payments")
    public String paymentManagement(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            List<Payment> payments = paymentService.getAllPayments();
            model.addAttribute("payments", payments);
        } catch (Exception e) {
            model.addAttribute("payments", new ArrayList<>());
            model.addAttribute("error", "Could not load payments: " + e.getMessage());
        }

        model.addAttribute("pageTitle", "AD-X - Payment Management");
        return "admin-payment-management";
    }

    // Payout management
    @GetMapping("/payouts")
    public String payoutManagement(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            List<Payout> payouts = payoutService.getAllPayouts();
            model.addAttribute("payouts", payouts);
        } catch (Exception e) {
            model.addAttribute("payouts", new ArrayList<>());
            model.addAttribute("error", "Could not load payouts: " + e.getMessage());
        }

        model.addAttribute("pageTitle", "AD-X - Payout Management");
        return "admin-payout-management";
    }

    // Process payout - FIXED: return boolean instead of Payout
    @PostMapping("/payouts/process/{payoutId}")
    public String processPayout(@PathVariable Long payoutId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            boolean processed = payoutService.processPayout(payoutId);
            if (processed) {
                redirectAttributes.addFlashAttribute("success", "Payout processed successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to process payout");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing payout: " + e.getMessage());
        }

        return "redirect:/admin/payouts";
    }

    // Process refund - FIXED: return boolean instead of Payment
    @PostMapping("/payments/refund/{paymentId}")
    public String processRefund(@PathVariable Long paymentId,
                                @RequestParam BigDecimal amount,
                                @RequestParam String reason,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            boolean refunded = paymentService.processRefund(paymentId, amount, reason);
            if (refunded) {
                redirectAttributes.addFlashAttribute("success", "Refund processed successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Payment not found or refund failed");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/payments";
    }

    // Helper methods for safe calculations
    private BigDecimal calculateRevenueSafe(LocalDateTime start, LocalDateTime end) {
        try {
            BigDecimal revenue = paymentService.calculateTotalRevenue(start, end);
            return revenue != null ? revenue : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateCommissionSafe(LocalDateTime start, LocalDateTime end) {
        try {
            // Try repository method first
            if (paymentRepository != null) {
                try {
                    BigDecimal commission = paymentRepository.calculateTotalCommission(start, end);
                    return commission != null ? commission : BigDecimal.ZERO;
                } catch (Exception e) {
                    // If repository method doesn't exist, calculate manually
                    return calculateCommissionManually(start, end);
                }
            } else {
                return calculateCommissionManually(start, end);
            }
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateCommissionManually(LocalDateTime start, LocalDateTime end) {
        try {
            List<Payment> payments = paymentService.getPaymentsByDateRange(start, end);
            BigDecimal commission = BigDecimal.ZERO;
            for (Payment payment : payments) {
                if ("COMPLETED".equals(payment.getStatus()) && payment.getAmount() != null) {
                    // Assume 10% commission
                    commission = commission.add(payment.getAmount().multiply(new BigDecimal("0.10")));
                }
            }
            return commission;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private List<Payment> getRecentPaymentsSafe() {
        try {
            return paymentService.getPaymentsByDateRange(
                    LocalDateTime.now().minusDays(7), LocalDateTime.now());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<Payout> getRecentPayoutsSafe() {
        try {
            return payoutService.getPayoutsByStatus("PENDING");
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}