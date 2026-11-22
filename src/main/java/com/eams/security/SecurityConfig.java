package com.eams.security;

import com.eams.config.AppProperties;
import com.eams.exception.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;

/**
 * URL-level access rules.
 *
 * Rules are evaluated top to bottom; the first match wins.
 * The final rule is denyAll(): an endpoint that is not listed here
 * is refused, so a new endpoint can never be "accidentally public".
 *
 * Ownership rules ("an employee may only see their own request")
 * are enforced in the services via CurrentUserService.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String MANAGER = "MANAGER";
    private static final String EMPLOYEE = "EMPLOYEE";
    private static final String TECHNICIAN = "TECHNICIAN";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AppProperties properties;

    private final JsonMapper jsonMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.cors().allowedOrigins());
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // JSON 401 / 403 responses in the same format as the rest of the API
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                writeError(response, HttpStatus.UNAUTHORIZED,
                                        "Authentication required or session expired. Please log in again.",
                                        request.getRequestURI()))
                        .accessDeniedHandler((request, response, e) ->
                                writeError(response, HttpStatus.FORBIDDEN,
                                        "You do not have permission to perform this action",
                                        request.getRequestURI())))

                .authorizeHttpRequests(auth -> auth

                        // ================= PUBLIC =================
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/verify-email",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // ================= AUTH (self) =================
                        .requestMatchers(
                                "/api/auth/me",
                                "/api/auth/change-password"
                        ).authenticated()

                        // ================= USERS =================
                        .requestMatchers(HttpMethod.GET, "/api/users/technicians")
                                .hasAnyRole(ADMIN, TECHNICIAN)
                        .requestMatchers("/api/users", "/api/users/**").hasRole(ADMIN)

                        // ================= EMPLOYEES =================
                        .requestMatchers(HttpMethod.GET, "/api/employees/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/employees/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/employees/me/profile").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/employees", "/api/employees/**")
                                .hasAnyRole(ADMIN, MANAGER)
                        .requestMatchers(HttpMethod.POST, "/api/employees").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/employees/*").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/employees/*").hasRole(ADMIN)

                        // ================= DEPARTMENTS =================
                        .requestMatchers(HttpMethod.GET, "/api/departments/*/employees")
                                .hasAnyRole(ADMIN, MANAGER)
                        .requestMatchers(HttpMethod.GET, "/api/departments", "/api/departments/*")
                                .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/departments").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/departments/*").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/departments/*").hasRole(ADMIN)

                        // ================= CATEGORIES =================
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**")
                                .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/categories").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/categories/*").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/*").hasRole(ADMIN)

                        // ================= ASSETS =================
                        .requestMatchers(HttpMethod.GET, "/api/assets/labels/pdf").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/assets/warranty-expiring")
                                .hasAnyRole(ADMIN, MANAGER)
                        .requestMatchers(HttpMethod.GET, "/api/assets/*/timeline")
                                .hasAnyRole(ADMIN, MANAGER, TECHNICIAN)
                        .requestMatchers(HttpMethod.GET, "/api/assets", "/api/assets/**")
                                .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/assets", "/api/assets/*/image")
                                .hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/assets/*", "/api/assets/*/retire")
                                .hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/assets/*").hasRole(ADMIN)

                        // ================= REQUESTS / ASSIGNMENTS =================
                        // Employee self-service (ownership checked in service)
                        .requestMatchers(HttpMethod.POST, "/api/requests").hasRole(EMPLOYEE)
                        .requestMatchers(HttpMethod.GET,
                                "/api/requests/my", "/api/requests/my/*",
                                "/api/requests/assignments/my", "/api/requests/assignments/my/*")
                                .hasRole(EMPLOYEE)
                        .requestMatchers(HttpMethod.PUT,
                                "/api/requests/assignments/*/return-request",
                                "/api/requests/assignments/*/return-request/*")
                                .hasRole(EMPLOYEE)

                        // Manager approval
                        .requestMatchers(HttpMethod.GET, "/api/requests/pending")
                                .hasAnyRole(MANAGER, ADMIN)
                        .requestMatchers(HttpMethod.PUT,
                                "/api/requests/*/approve", "/api/requests/*/approve/*",
                                "/api/requests/*/reject", "/api/requests/*/reject/*")
                                .hasRole(MANAGER)

                        // Admin assignment and returns
                        .requestMatchers(HttpMethod.GET, "/api/requests/approved").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT,
                                "/api/requests/*/assign", "/api/requests/*/assign/*",
                                "/api/requests/assignments/*/return",
                                "/api/requests/assignments/*/return/*")
                                .hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/requests/assignments/history")
                                .hasAnyRole(ADMIN, MANAGER)
                        .requestMatchers(HttpMethod.GET, "/api/requests/assignments/*")
                                .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/requests").hasAnyRole(ADMIN, MANAGER)
                        .requestMatchers(HttpMethod.GET, "/api/requests/*").authenticated()

                        // ================= DAMAGE =================
                        .requestMatchers(HttpMethod.POST, "/api/damage").hasRole(EMPLOYEE)
                        .requestMatchers(HttpMethod.POST, "/api/damage/*/photos")
                                .hasAnyRole(EMPLOYEE, ADMIN, TECHNICIAN)
                        .requestMatchers(HttpMethod.POST, "/api/damage/*/ai-triage")
                                .hasAnyRole(ADMIN, TECHNICIAN)
                        .requestMatchers(HttpMethod.GET, "/api/damage/my", "/api/damage/my/*")
                                .hasRole(EMPLOYEE)
                        .requestMatchers(HttpMethod.GET, "/api/damage", "/api/damage/asset/*")
                                .hasAnyRole(ADMIN, TECHNICIAN, MANAGER)
                        .requestMatchers(HttpMethod.GET, "/api/damage/*").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/damage/*").hasAnyRole(TECHNICIAN, ADMIN)

                        // ================= MAINTENANCE =================
                        .requestMatchers(HttpMethod.GET, "/api/maintenance/my").hasRole(TECHNICIAN)
                        .requestMatchers("/api/maintenance", "/api/maintenance/**")
                                .hasAnyRole(ADMIN, TECHNICIAN)

                        // ================= DASHBOARDS =================
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/admin").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/manager")
                                .hasAnyRole(MANAGER, ADMIN)
                        .requestMatchers(HttpMethod.GET,
                                "/api/dashboard/employee", "/api/dashboard/employee/*")
                                .hasRole(EMPLOYEE)
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/technician")
                                .hasAnyRole(TECHNICIAN, ADMIN)

                        // ================= REPORTS =================
                        .requestMatchers(HttpMethod.GET,
                                "/api/reports/maintenance", "/api/reports/damage")
                                .hasAnyRole(ADMIN, MANAGER, TECHNICIAN)
                        .requestMatchers(HttpMethod.GET, "/api/reports/**")
                                .hasAnyRole(ADMIN, MANAGER)

                        // ================= INSIGHTS / AI =================
                        .requestMatchers(HttpMethod.GET, "/api/insights/**").hasAnyRole(ADMIN, MANAGER)
                        .requestMatchers(HttpMethod.POST, "/api/assistant/ask").hasAnyRole(ADMIN, MANAGER)
                        .requestMatchers(HttpMethod.GET, "/api/assistant/status").authenticated()

                        // ================= NOTIFICATIONS =================
                        .requestMatchers("/api/notifications", "/api/notifications/**").authenticated()

                        // ================= STOCK-TAKE AUDITS =================
                        .requestMatchers(HttpMethod.POST, "/api/audits/*/close").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/audits").hasRole(ADMIN)
                        .requestMatchers("/api/audits", "/api/audits/**").hasAnyRole(ADMIN, TECHNICIAN)
                        .requestMatchers(HttpMethod.GET, "/api/audit-log").hasRole(ADMIN)

                        // ================= DEFAULT =================
                        .anyRequest().denyAll()
                )

                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeError(HttpServletResponse response, HttpStatus status,
                            String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(),
                GlobalExceptionHandler.body(status, message, path));
    }
}
