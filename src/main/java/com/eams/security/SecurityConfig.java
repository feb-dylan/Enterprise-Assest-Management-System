package com.eams.security;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// CORS imports
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;


    // =========================================================
    // PASSWORD ENCODER
    // =========================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    // =========================================================
    // AUTHENTICATION MANAGER
    // =========================================================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }


    // =========================================================
    // CORS CONFIGURATION
    // =========================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // React frontend
        configuration.setAllowedOrigins(
                List.of("http://localhost:5173")
        );

        // HTTP methods allowed from React
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );

        // Allow request headers such as Authorization and Content-Type
        configuration.setAllowedHeaders(
                List.of("*")
        );

        // Allow credentials such as cookies/authentication
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
    // SECURITY FILTER CHAIN
    // =========================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // =================================================
                // CORS
                // =================================================

                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))


                // =================================================
                // CSRF
                // =================================================

                .csrf(csrf -> csrf.disable())


                // =================================================
                // SESSION
                // =================================================

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )


                // =================================================
                // AUTHORIZATION
                // =================================================

                .authorizeHttpRequests(auth -> auth

                        // =========================================
                        // PUBLIC AUTH ENDPOINTS
                        // =========================================

                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/verify-email"
                        ).permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/auth/me").authenticated()


                        // =========================================
                        // EMPLOYEE
                        // =========================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/employees"
                        ).hasRole("ADMIN")


                        // Employee can get their own employee profile
                        // IMPORTANT: this must come BEFORE /api/employees/*
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/employees/me"
                        ).hasAnyRole(
                                "ADMIN",
                                "MANAGER",
                                "EMPLOYEE",
                                "TECHNICIAN"
                        )


                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/employees",
                                "/api/employees/*",
                                "/api/employees/search"
                        ).hasAnyRole(
                                "ADMIN",
                                "MANAGER"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/employees/*"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/employees/*"
                        ).hasRole("ADMIN")


                        // =========================================
                        // DEPARTMENT
                        // =========================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/departments"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/employees/me"
                        ).hasRole("EMPLOYEE")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/departments",
                                "/api/departments/*"
                        ).hasAnyRole(
                                "ADMIN",
                                "MANAGER"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/departments/*"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/departments/*"
                        ).hasRole("ADMIN")


                        // =========================================
                        // CATEGORY
                        // =========================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/categories"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/categories",
                                "/api/categories/*"
                        ).hasAnyRole(
                                "ADMIN",
                                "MANAGER",
                                "EMPLOYEE",
                                "TECHNICIAN"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/categories/*"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/categories/*"
                        ).hasRole("ADMIN")


                        // =========================================
                        // ASSETS
                        // =========================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/assets"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/assets",
                                "/api/assets/*"
                        ).hasAnyRole(
                                "ADMIN",
                                "MANAGER",
                                "EMPLOYEE",
                                "TECHNICIAN"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/assets/*"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/assets/*"
                        ).hasRole("ADMIN")


                        // =========================================
                        // ASSET REQUESTS
                        // =========================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/requests"
                        ).hasRole("EMPLOYEE")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/requests/my/*"
                        ).hasRole("EMPLOYEE")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/requests/pending"
                        ).hasRole("MANAGER")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/requests"
                        ).hasAnyRole(
                                "ADMIN",
                                "MANAGER"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/requests/*/approve/*"
                        ).hasRole("MANAGER")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/requests/*/reject/*"
                        ).hasRole("MANAGER")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/requests/approved"
                        ).hasRole("ADMIN")
                        // GET one request by id — allowed for all authenticated roles
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/requests/*"
                        ).hasAnyRole(
                                "ADMIN",
                                "MANAGER",
                                "EMPLOYEE",
                                "TECHNICIAN"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/requests/*/assign/*"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/requests/assignments/*/return/*"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/requests/assignments/history"
                        ).hasRole("ADMIN")


                        // =========================================
                        // DAMAGE
                        // =========================================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/damage"
                        ).hasRole("EMPLOYEE")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/damage/my/*"
                        ).hasRole("EMPLOYEE")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/damage"
                        ).hasAnyRole(
                                "ADMIN",
                                "TECHNICIAN"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/damage/*"
                        ).hasRole("TECHNICIAN")


                        // =========================================
                        // MAINTENANCE
                        // =========================================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/maintenance",
                                "/api/maintenance/*"
                        ).hasAnyRole(
                                "ADMIN",
                                "TECHNICIAN"
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/maintenance"
                        ).hasAnyRole(
                                "ADMIN",
                                "TECHNICIAN"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/maintenance/*"
                        ).hasAnyRole(
                                "ADMIN",
                                "TECHNICIAN"
                        )


                        // =========================================
                        // DASHBOARD
                        // =========================================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/dashboard/admin"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/dashboard/manager"
                        ).hasRole("MANAGER")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/dashboard/employee/*"
                        ).hasRole("EMPLOYEE")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/dashboard/technician"
                        ).hasRole("TECHNICIAN")


                        // =========================================
                        // REPORTS
                        // =========================================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reports/assets"
                        ).hasAnyRole(
                                "ADMIN",
                                "MANAGER"
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reports/requests"
                        ).hasAnyRole(
                                "ADMIN",
                                "MANAGER"
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reports/maintenance"
                        ).hasAnyRole(
                                "ADMIN",
                                "TECHNICIAN"
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reports/damage"
                        ).hasAnyRole(
                                "ADMIN",
                                "TECHNICIAN"
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reports/assignments"
                        ).hasAnyRole(
                                "ADMIN",
                                "MANAGER"
                        )


                        // =========================================
                        // EVERYTHING ELSE
                        // =========================================

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