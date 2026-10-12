package com.medilink.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.medilink.filter.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
private String allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // =========================================================
    // PASSWORD ENCODER
    // =========================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =========================================================
    // CORS CONFIGURATION
    // =========================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
    java.util.Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(origin -> !origin.isEmpty())
        .toList()
);

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    // =========================================================
    // SECURITY CONFIGURATION
    // =========================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http

            // Enable CORS
            .cors(cors -> cors.configurationSource(
                    corsConfigurationSource()
            ))

            // REST API
            .csrf(csrf -> csrf.disable())

            // JWT is stateless
            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )

            .authorizeHttpRequests(auth -> auth

                // =================================================
                // PUBLIC
                // =================================================

                .requestMatchers(
                        "/api/test"
                ).permitAll()

                .requestMatchers(
                        HttpMethod.POST,
                        "/api/users/register"
                ).permitAll()

                .requestMatchers(
                        HttpMethod.POST,
                        "/api/users/login"
                ).permitAll()

                .requestMatchers(
                        HttpMethod.POST,
                        "/api/users/forgot-password"
                ).permitAll()

                .requestMatchers(
                        HttpMethod.POST,
                        "/api/users/reset-password"
                ).permitAll()

                .requestMatchers(
                        HttpMethod.OPTIONS,
                        "/**"
                ).permitAll()

                // =================================================
                // HEALTHCARE PRODUCTS
                // =================================================

                // Anyone can view healthcare products
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/healthcare-products",
                        "/api/healthcare-products/**"
                ).permitAll()

                // Only ADMIN + PHARMACY can create
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/healthcare-products",
                        "/api/healthcare-products/**"
                ).hasAnyRole(
                        "ADMIN",
                        "PHARMACY"
                )

                // Only ADMIN + PHARMACY can update
                .requestMatchers(
                        HttpMethod.PUT,
                        "/api/healthcare-products/**"
                ).hasAnyRole(
                        "ADMIN",
                        "PHARMACY"
                )

                // Only ADMIN + PHARMACY can delete
                .requestMatchers(
                        HttpMethod.DELETE,
                        "/api/healthcare-products/**"
                ).hasAnyRole(
                        "ADMIN",
                        "PHARMACY"
                )

                // =================================================
                // USERS
                // =================================================

                // Admin only
                .requestMatchers(
                        "/api/users/**"
                ).hasRole("ADMIN")

                // =================================================
                // MEDICINES
                // =================================================

                // Logged-in users can view medicines
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/medicines",
                        "/api/medicines/**"
                ).authenticated()

                // Admin + Pharmacy can add
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/medicines"
                ).hasAnyRole(
                        "ADMIN",
                        "PHARMACY"
                )

                // Admin + Pharmacy can update
                .requestMatchers(
                        HttpMethod.PUT,
                        "/api/medicines/**"
                ).hasAnyRole(
                        "ADMIN",
                        "PHARMACY"
                )

                // Admin + Pharmacy can delete
                .requestMatchers(
                        HttpMethod.DELETE,
                        "/api/medicines/**"
                ).hasAnyRole(
                        "ADMIN",
                        "PHARMACY"
                )

                // =================================================
                // ORDERS
                // =================================================

                // Order controller performs ownership checks.
                // Authentication is required here.
                .requestMatchers(
                        "/api/orders/**"
                ).authenticated()

                // =================================================
                // DELIVERY PARTNER
                // =================================================

                // Delivery partner dashboard
                .requestMatchers(
                        "/api/delivery-partner/**"
                ).hasRole("DELIVERY_PARTNER")

                // =================================================
// DELIVERY PARTNERS MANAGEMENT
// =================================================

// Only ADMIN can manage delivery partner accounts
.requestMatchers(
        "/api/delivery-partners",
        "/api/delivery-partners/**"
).hasRole("ADMIN")




                // IMPORTANT:
                // Only ADMIN + PHARMACY can assign delivery partners.
                .requestMatchers(
                        HttpMethod.PUT,
                        "/api/delivery/assign/**"
                ).hasAnyRole(
                        "ADMIN",
                        "PHARMACY"
                )

                // GPS/location APIs remain authenticated.
                // Controller performs additional ownership checks.
                .requestMatchers(
                        "/api/delivery/**"
                ).authenticated()

                // =================================================
                // EVERYTHING ELSE
                // =================================================

                .anyRequest().authenticated()
            )

            // =================================================
            // JWT FILTER
            // =================================================

            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}