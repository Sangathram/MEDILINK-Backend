package com.medilink.filter;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.medilink.entity.User;
import com.medilink.repository.UserRepository;
import com.medilink.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No JWT token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {

            // Get email from JWT
            String email = jwtService.extractEmail(token);

            System.out.println("JWT EMAIL = " + email);

            if (email != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                // Find user in database
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                // Get user's role
                String role = user.getRole();

                System.out.println("JWT ROLE = " + role);

                // Add ROLE_ prefix for Spring Security
                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority("ROLE_" + role);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                Collections.singletonList(authority)
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

                System.out.println("JWT AUTHENTICATION SET");
            }

        } catch (Exception e) {

            System.out.println("JWT ERROR = " + e.getMessage());

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}