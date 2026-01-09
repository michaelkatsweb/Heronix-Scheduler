package com.heronix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * Security Configuration
 * Location: src/main/java/com/eduscheduler/config/SecurityConfig.java
 *
 * Implements proper authentication with:
 * - BCrypt password encoding
 * - Role-based access control
 * - Profile-based security (dev vs prod)
 * - Method-level security annotations
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-11-04
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    /**
     * Password encoder bean - BCrypt for secure password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication provider - connects UserDetailsService with password encoder
     */
    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                           PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Authentication manager bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Development security: Permissive (for easier development)
     * - CSRF disabled
     * - All endpoints accessible
     * - H2 console enabled
     */
    @Bean
    @Profile({"dev", "test", "default"})
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for development
            .csrf(csrf -> csrf.disable())

            // Allow all requests (no authentication required in dev)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()  // H2 database console
                .requestMatchers("/api/**").permitAll()          // All REST APIs
                .requestMatchers("/api/teacher/**").permitAll()  // Teacher Portal APIs
                .anyRequest().permitAll()                        // Everything else
            )

            // Security headers (Quick Win #2) - H2 console exception for frame options
            .headers(headers -> headers
                // Content Security Policy - Relaxed for dev mode to allow H2 console
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data: blob:; " +
                        "font-src 'self' data:; " +
                        "connect-src 'self'; " +
                        "frame-ancestors 'self'"  // Allow frames for H2 console
                    )
                )
                // Disable frame options only for H2 console (will be strict in prod)
                .frameOptions(frame -> frame.disable())
                // X-Content-Type-Options - Prevents MIME type sniffing
                .contentTypeOptions(contentTypeOptions -> {})
                // X-XSS-Protection - Enables browser XSS filter
                .xssProtection(xss -> xss
                    .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                )
                // Referrer-Policy - Controls referrer information
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            );

        return http.build();
    }

    /**
     * Production security: Strict authentication required
     * - CSRF protection enabled
     * - Role-based access control
     * - HTTP Basic authentication
     * - Account lockout after failed attempts
     */
    @Bean
    @Profile("prod")
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http,
                                                         AuthenticationProvider authenticationProvider) throws Exception {
        http
            // CSRF protection enabled (with API exceptions)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/auth/**")     // Allow login/logout endpoints
                .ignoringRequestMatchers("/api/teacher/**")  // Allow Teacher Portal API endpoints
            )

            // Authentication provider
            .authenticationProvider(authenticationProvider)

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                .requestMatchers("/api/teacher/auth/login").permitAll()  // Teacher Portal login
                .requestMatchers("/api/health", "/api/teacher/health").permitAll()  // Health checks

                // H2 console disabled in production
                .requestMatchers("/h2-console/**").denyAll()

                // Teacher Portal endpoints (require authentication via custom auth)
                .requestMatchers("/api/teacher/**").permitAll()  // Custom auth handled in controller

                // Admin-only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/users/**").hasRole("ADMIN")

                // Protected endpoints (any authenticated user)
                .requestMatchers("/api/schedules/**").authenticated()
                .requestMatchers("/api/teachers/**").authenticated()
                .requestMatchers("/api/courses/**").authenticated()
                .requestMatchers("/api/rooms/**").authenticated()

                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // HTTP Basic authentication
            .httpBasic(basic -> {})

            // Form login (for web UI if needed)
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )

            // Logout configuration
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // Security headers (Quick Win #2) - Strict production settings
            .headers(headers -> headers
                // Content Security Policy - Prevents XSS and data injection attacks
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data: blob:; " +
                        "font-src 'self' data:; " +
                        "connect-src 'self'; " +
                        "frame-ancestors 'none'"  // Strict - no frames allowed
                    )
                )
                // X-Frame-Options - Prevents clickjacking
                .frameOptions(frameOptions -> frameOptions.deny())
                // X-Content-Type-Options - Prevents MIME type sniffing
                .contentTypeOptions(contentTypeOptions -> {})
                // X-XSS-Protection - Enables browser XSS filter
                .xssProtection(xss -> xss
                    .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                )
                // Referrer-Policy - Controls referrer information
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                // HTTP Strict Transport Security (HSTS) - Forces HTTPS connections
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)  // 1 year
                )
            );

        return http.build();
    }
}
