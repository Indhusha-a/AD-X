package com.adx.ad_x.controller;

import com.adx.ad_x.model.User;
import com.adx.ad_x.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    // Show profile edit form
    @GetMapping("/edit")
    public String showEditForm(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Get fresh data from database
        User freshUser = userService.getUserById(user.getId()).orElse(user);
        model.addAttribute("user", freshUser);
        model.addAttribute("pageTitle", "AD-X - Edit Profile");
        return "profile-edit";
    }

    // Process profile update
    @PostMapping("/update")
    public String processUpdate(@RequestParam String firstName,
                                @RequestParam String lastName,
                                @RequestParam String email,
                                @RequestParam(required = false) String password,
                                HttpSession session,
                                Model model) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        // Get the current user from database to preserve the role
        User currentUser = userService.getUserById(sessionUser.getId()).orElse(sessionUser);

        // Update only the fields that are allowed to change
        currentUser.setFirstName(firstName);
        currentUser.setLastName(lastName);
        currentUser.setEmail(email);

        // Only update password if provided
        if (password != null && !password.trim().isEmpty()) {
            currentUser.setPassword(password);
        }

        User updatedUser = userService.updateUser(currentUser.getId(), currentUser);
        if (updatedUser != null) {
            // Update session with new data
            session.setAttribute("user", updatedUser);
            model.addAttribute("success", "Profile updated successfully!");
            return "redirect:/dashboard"; // Redirect to dashboard after success
        } else {
            model.addAttribute("error", "Failed to update profile.");
            return "profile-edit";
        }
    }
}