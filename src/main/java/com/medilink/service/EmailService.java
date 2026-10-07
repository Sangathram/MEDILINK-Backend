package com.medilink.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom("virat1818ram@gmail.com");
        message.setTo(toEmail);
        message.setSubject("MEDILINK - Password Reset");

        message.setText(
                "Hello,\n\n"
                + "We received a request to reset your MEDILINK password.\n\n"
                + "Click the link below to reset your password:\n\n"
                + resetLink
                + "\n\n"
                + "This password reset link is temporary and should only be used once.\n\n"
                + "If you did not request a password reset, please ignore this email.\n\n"
                + "Regards,\n"
                + "MEDILINK Team"
        );

        mailSender.send(message);
    }
}