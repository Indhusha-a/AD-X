package com.adx.ad_x.controller;

import com.adx.ad_x.model.*;
import com.adx.ad_x.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private NotificationService notificationService;

    // Check if user is admin
    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null && "ADMIN".equals(user.getRole());
    }

    // Admin dashboard - UPDATED WITH NOTIFICATION COUNT
    @GetMapping("/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        User admin = (User) session.getAttribute("user");
        List<User> users = userService.getAllUsers();

        // Get unread notification count - ADDED
        long unreadCount = notificationService.getUnreadCount(admin);

        model.addAttribute("users", users);
        model.addAttribute("unreadCount", unreadCount); // ADDED
        model.addAttribute("pageTitle", "AD-X - Admin Dashboard");
        return "admin-dashboard";
    }

    // Show edit user form - UPDATED WITH NOTIFICATION COUNT
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        User admin = (User) session.getAttribute("user");
        Optional<User> user = userService.getUserById(id);
        if (user.isPresent()) {
            // Get unread notification count - ADDED
            long unreadCount = notificationService.getUnreadCount(admin);

            model.addAttribute("user", user.get());
            model.addAttribute("unreadCount", unreadCount); // ADDED
            model.addAttribute("pageTitle", "AD-X - Edit User");
            return "admin-edit-user";
        }
        return "redirect:/admin/dashboard";
    }

    // Process user edit - NO CHANGES NEEDED
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

    // Delete user - NO CHANGES NEEDED
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        userService.deleteUser(id);
        return "redirect:/admin/dashboard";
    }

    // Financial dashboard - UPDATED WITH NOTIFICATION COUNT
    @GetMapping("/financial")
    public String financialDashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        User admin = (User) session.getAttribute("user");

        // Get payment statistics
        PaymentService.PaymentStatistics stats = paymentService.getPaymentStatistics();

        // Calculate revenue metrics
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        BigDecimal todayRevenue = paymentService.calculatePlatformRevenue(todayStart, todayEnd);
        BigDecimal monthRevenue = paymentService.calculatePlatformRevenue(monthStart, todayEnd);
        BigDecimal totalCommission = paymentService.calculatePlatformCommission(monthStart, todayEnd);

        // Get recent payments and payouts
        List<Payment> recentPayments = paymentService.getPaymentsByDateRange(
                LocalDateTime.now().minusDays(7), LocalDateTime.now());
        List<Payout> recentPayouts = payoutService.getPayoutsByStatus("PENDING");

        // Get unread notification count - ADDED
        long unreadCount = notificationService.getUnreadCount(admin);

        model.addAttribute("stats", stats);
        model.addAttribute("todayRevenue", todayRevenue);
        model.addAttribute("monthRevenue", monthRevenue);
        model.addAttribute("totalCommission", totalCommission);
        model.addAttribute("recentPayments", recentPayments);
        model.addAttribute("recentPayouts", recentPayouts);
        model.addAttribute("unreadCount", unreadCount); // ADDED
        model.addAttribute("pageTitle", "AD-X - Financial Dashboard");
        return "admin-financial-dashboard";
    }

    // Payment management - UPDATED WITH NOTIFICATION COUNT
    @GetMapping("/payments")
    public String paymentManagement(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        User admin = (User) session.getAttribute("user");
        List<Payment> payments = paymentService.getPaymentsByStatus("COMPLETED");

        // Get unread notification count - ADDED
        long unreadCount = notificationService.getUnreadCount(admin);

        model.addAttribute("payments", payments);
        model.addAttribute("unreadCount", unreadCount); // ADDED
        model.addAttribute("pageTitle", "AD-X - Payment Management");
        return "admin-payment-management";
    }

    // Payout management - UPDATED WITH NOTIFICATION COUNT
    @GetMapping("/payouts")
    public String payoutManagement(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        User admin = (User) session.getAttribute("user");
        List<Payout> payouts = payoutService.getAllPayouts();

        // Get unread notification count - ADDED
        long unreadCount = notificationService.getUnreadCount(admin);

        model.addAttribute("payouts", payouts);
        model.addAttribute("unreadCount", unreadCount); // ADDED
        model.addAttribute("pageTitle", "AD-X - Payout Management");
        return "admin-payout-management";
    }

    // Process payout - NO CHANGES NEEDED
    @PostMapping("/payouts/process/{payoutId}")
    public String processPayout(@PathVariable Long payoutId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        Payout payout = payoutService.processPayout(payoutId);
        if (payout != null) {
            redirectAttributes.addFlashAttribute("success", "Payout processed successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to process payout");
        }

        return "redirect:/admin/payouts";
    }

    // Process refund - NO CHANGES NEEDED
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
            Payment payment = paymentService.processRefund(paymentId, amount, reason);
            if (payment != null) {
                redirectAttributes.addFlashAttribute("success", "Refund processed successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Payment not found");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/payments";
    }
}