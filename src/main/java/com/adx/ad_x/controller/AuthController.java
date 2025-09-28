package com.adx.ad_x.controller;

import com.adx.ad_x.model.User;
import com.adx.ad_x.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    // Show home page
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "AD-X - Home");
        return "index";
    }

    // Show registration form
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("pageTitle", "AD-X - Register");
        return "register";
    }

    // Process registration form
    @PostMapping("/register")
    public String processRegistration(@ModelAttribute User user, Model model) {
        if (userService.emailExists(user.getEmail())) {
            model.addAttribute("error", "Email already exists");
            return "register";
        }

        userService.createUser(user);
        model.addAttribute("success", "Registration successful. Please login.");
        return "login";
    }

    // Show user login form
    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");
        }
        model.addAttribute("pageTitle", "AD-X - User Login");
        return "login";
    }

    // Show admin login form - FIXED MAPPING
    @GetMapping("/admin/login")
    public String showAdminLoginForm(@RequestParam(value = "error", required = false) String error,
                                     @RequestParam(value = "logout", required = false) String logout,
                                     Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid administrator credentials");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");
        }
        model.addAttribute("pageTitle", "AD-X - Admin Login");
        return "admin-login"; // Make sure this matches your template filename
    }

    // Process login form (for both users and admins)
    @PostMapping("/custom-login")
    public String processLogin(@RequestParam String email,
                               @RequestParam String password,
                               HttpSession session,
                               Model model) {

        Optional<User> user = userService.getUserByEmail(email);

        if (user.isPresent() && user.get().getPassword().equals(password)) {
            session.setAttribute("user", user.get());

            // Redirect based on role
            if (user.get().getRole().equals("ADMIN")) {
                return "redirect:/admin/dashboard";
            } else if (user.get().getRole().equals("SELLER")) {
                return "redirect:/seller/dashboard";
            } else if (user.get().getRole().equals("BUYER")) {
                return "redirect:/buyer/dashboard";
            } else {
                return "redirect:/dashboard";
            }
        } else {
            // Check if the login attempt was from admin portal
            if (email.contains("admin") || user.map(u -> "ADMIN".equals(u.getRole())).orElse(false)) {
                return "redirect:/admin/login?error=true";
            } else {
                return "redirect:/login?error=true";
            }
        }
    }

    // Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }
}