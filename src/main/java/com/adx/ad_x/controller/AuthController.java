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

    // Show login form
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
        model.addAttribute("pageTitle", "AD-X - Login");
        return "login";
    }

    // Process login form
    @PostMapping("/custom-login")
    public String processLogin(@RequestParam String email,
                               @RequestParam String password,
                               HttpSession session,
                               Model model) {

        Optional<User> user = userService.getUserByEmail(email);

        if (user.isPresent() && user.get().getPassword().equals(password)) {
            session.setAttribute("user", user.get());

            if (user.get().getRole().equals("ADMIN")) {
                return "redirect:/admin/dashboard";
            } else {
                return "redirect:/dashboard";
            }
        } else {
            return "redirect:/login?error=true";
        }
    }

    // Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }
}