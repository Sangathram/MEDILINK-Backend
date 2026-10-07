package com.medilink.service;

import com.medilink.entity.PasswordResetToken;
import com.medilink.entity.User;
import com.medilink.repository.PasswordResetTokenRepository;
import com.medilink.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    // Send password reset email
    public void sendResetEmail(String email) {

        // Check whether the email belongs to an existing user
        userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("No account found with this email"));

        // Delete any previous reset token for this email
        tokenRepository.findByEmail(email)
                .ifPresent(tokenRepository::delete);

        // Generate a secure random token
        String token = UUID.randomUUID().toString();

        // Token will expire after 15 minutes
        LocalDateTime expiryDate =
                LocalDateTime.now().plusMinutes(15);

        // Create reset token
        PasswordResetToken resetToken =
                new PasswordResetToken(token, email, expiryDate);

        // Save token in MySQL
        tokenRepository.save(resetToken);

        // Create password reset link
        String resetLink =
                "http://localhost:5173/reset-password?token=" + token;

        // Send reset email
        emailService.sendPasswordResetEmail(email, resetLink);
    }

    // Reset the user's password
    public void resetPassword(String token, String newPassword) {

        // Find the reset token
        PasswordResetToken resetToken =
                tokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException("Invalid reset token"));

        // Check whether token was already used
        if (resetToken.isUsed()) {
            throw new RuntimeException(
                    "This reset link has already been used");
        }

        // Check whether token has expired
        if (resetToken.getExpiryDate()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "This reset link has expired");
        }

        // Find the user associated with the token
        User user =
                userRepository.findByEmail(resetToken.getEmail())
                        .orElseThrow(() ->
                                new RuntimeException("User not found"));

        // Encrypt the new password
        user.setPassword(
                passwordEncoder.encode(newPassword));

        // Save the updated password
        userRepository.save(user);

        // Mark the reset token as used
        resetToken.setUsed(true);

        // Save the updated token
        tokenRepository.save(resetToken);
    }
}