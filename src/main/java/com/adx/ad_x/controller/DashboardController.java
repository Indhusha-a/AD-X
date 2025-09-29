package com.adx.ad_x.controller;

import com.adx.ad_x.model.User;
import com.adx.ad_x.service.FavoriteService;
import com.adx.ad_x.service.OrderService;
import com.adx.ad_x.service.InquiryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private InquiryService inquiryService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Redirect based on user role
        if ("BUYER".equals(user.getRole())) {
            return "redirect:/buyer/dashboard";
        } else if ("SELLER".equals(user.getRole())) {
            return "redirect:/seller/dashboard";
        } else if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin/dashboard";
        } else {
            // Fallback for unknown roles - show generic dashboard
            model.addAttribute("user", user);
            model.addAttribute("pageTitle", "AD-X - Dashboard");
            return "dashboard";
        }
    }

    @GetMapping("/buyer/dashboard")
    public String buyerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Allow only BUYER role to access buyer dashboard
        if (!"BUYER".equals(user.getRole())) {
            return "redirect:/dashboard"; // Redirect to role-appropriate dashboard
        }

        // Get stats for the buyer
        Long favoriteCount = favoriteService.getFavoriteCount(user);
        Long orderCount = orderService.getUserOrderCount(user);
        Long inquiryCount = inquiryService.getUnreadInquiryCountForBuyer(user);

        // Get recent activity data
        Map<String, Object> recentActivity = getRecentActivity(user);

        model.addAttribute("user", user);
        model.addAttribute("favoriteCount", favoriteCount);
        model.addAttribute("orderCount", orderCount);
        model.addAttribute("inquiryCount", inquiryCount);
        model.addAttribute("recentActivity", recentActivity);
        model.addAttribute("pageTitle", "AD-X - Buyer Dashboard");
        return "buyer-dashboard";
    }

    @GetMapping("/seller/dashboard")
    public String sellerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Allow only SELLER role to access seller dashboard
        if (!"SELLER".equals(user.getRole())) {
            return "redirect:/dashboard"; // Redirect to role-appropriate dashboard
        }

        // For now, we'll set productCount to 0 since we don't have ProductService in IAM
        // This will be updated when we implement the Seller Interface
        Long productCount = 0L;

        model.addAttribute("user", user);
        model.addAttribute("productCount", productCount);
        model.addAttribute("pageTitle", "AD-X - Seller Dashboard");
        return "seller-dashboard";
    }

    // Generic dashboard for any role (fallback)
    @GetMapping("/generic-dashboard")
    public String genericDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "AD-X - Dashboard");
        return "dashboard";
    }

    // Helper method to get recent activity
    private Map<String, Object> getRecentActivity(User user) {
        Map<String, Object> activity = new HashMap<>();

        // Get recent orders
        activity.put("recentOrders", orderService.getUserOrders(user).stream().limit(3).toList());

        // Get recent inquiries
        activity.put("recentInquiries", inquiryService.getBuyerInquiries(user).stream().limit(3).toList());

        // Check if there's any activity
        boolean hasActivity = !((List<?>) activity.get("recentOrders")).isEmpty() ||
                !((List<?>) activity.get("recentInquiries")).isEmpty();

        activity.put("hasActivity", hasActivity);

        return activity;
    }
}