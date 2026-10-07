package com.medilink.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.medilink.dto.LoginRequest;
import com.medilink.dto.LoginResponse;
import com.medilink.entity.User;
import com.medilink.service.PasswordResetService;
import com.medilink.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    public UserController(
            UserService userService,
            PasswordResetService passwordResetService) {

        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    // Register a new user
    @PostMapping("/register")
    public User register(@RequestBody User user) {
        return userService.registerUser(user);
    }

    // Login user
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return userService.loginUser(
                request.getEmail(),
                request.getPassword()
        );
    }

    // Forgot password - send reset email
    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }

        passwordResetService.sendResetEmail(email.trim());

        Map<String, String> response = new HashMap<>();
        response.put("message",
                "Password reset link has been sent to your email");

        return response;
    }

    // Reset password
    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(
            @RequestBody Map<String, String> request) {

        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("Reset token is required");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("New password is required");
        }

        if (newPassword.length() < 6) {
            throw new RuntimeException(
                    "Password must contain at least 6 characters");
        }

        passwordResetService.resetPassword(
                token.trim(),
                newPassword
        );

        Map<String, String> response = new HashMap<>();
        response.put("message",
                "Password has been reset successfully");

        return response;
    }

    // Get all users - ADMIN only
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // Get user by ID - ADMIN only
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}