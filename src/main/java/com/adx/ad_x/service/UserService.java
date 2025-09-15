package com.adx.ad_x.service;

import com.adx.ad_x.model.User;
import com.adx.ad_x.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

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

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
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
}