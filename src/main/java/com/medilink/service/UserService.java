package com.medilink.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.medilink.dto.LoginResponse;
import com.medilink.entity.User;
import com.medilink.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // Register user
    public User registerUser(User user) {

        // Encrypt password before saving
        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        // Default role
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("PATIENT");
        }

        return userRepository.save(user);
    }

    // Login user and generate JWT
    public LoginResponse loginUser(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // Check whether password exists
        if (user.getPassword() == null) {
            throw new RuntimeException("User password is not set");
        }

        // Verify password
        if (!passwordEncoder.matches(
                password,
                user.getPassword())) {

            throw new RuntimeException("Invalid password");
        }

        // Generate JWT using the User object
        String token = jwtService.generateToken(user);

        // Return login response
        return new LoginResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                token
        );
    }

    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get user by ID
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}