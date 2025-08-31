package com.example.common.config;

import com.example.common.security.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 🔧 FIX 1: Allow OPTIONS requests for preflight (MUST BE FIRST)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public endpoints
                        .requestMatchers("/api/register", "/api/login").permitAll()

                        // 🔧 FIX: Allow all authenticated users to manage friends and community resources
                        .requestMatchers("/api/community/**").authenticated()
                        .requestMatchers("/api/friends/**").authenticated()
                        .requestMatchers("/api/jobs/**").authenticated()
                        .requestMatchers("/api/cv/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/chat/**").authenticated()

                        // 🔧 FIX 2: Extension auth endpoints (public)
                        .requestMatchers("/api/auth/extension", "/api/auth/extension/**").permitAll()

                        // 🔧 FIX 3: Extension endpoints (public) - REMOVE DUPLICATE AUTHENTICATED RULE
                        .requestMatchers("/api/extension/**").permitAll()

                        .requestMatchers("/api/auth/user").authenticated()

                        // REPORT GENERATION - Admin only
                        .requestMatchers("/api/reports/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/chat").authenticated()

                        // Admin-only endpoints for courses
                        .requestMatchers(HttpMethod.POST, "/api/courses").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/courses/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/courses/*/enroll").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/*/enrollments").hasAuthority("ROLE_ADMIN")

                        // Admin-only user and department management
                        .requestMatchers(HttpMethod.POST, "/api/users/admin-create").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAuthority("ROLE_ADMIN")

                        // Allow all authenticated users to fetch user lists by role
                        .requestMatchers(HttpMethod.GET, "/api/users/role/**").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/users/by-ids").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/departments/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/profile-analytics/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/exams/**").authenticated()

                        // Profile and search endpoints accessible to all authenticated users
                        .requestMatchers("/api/users/profile/**").authenticated()
                        .requestMatchers("/api/users/search").authenticated()

                        // Add these new rules for the specific profile endpoints
                        .requestMatchers(HttpMethod.GET, "/api/students/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/lecturers/**").authenticated()

                        // Read access to courses
                        .requestMatchers(HttpMethod.GET, "/api/courses/**").authenticated()
                        .requestMatchers("/api/messages/**").authenticated()
                        .requestMatchers("/api/calendar/**").authenticated()

                        .requestMatchers("/api/cv/**").authenticated()
                        .requestMatchers("/api/resources/**").authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/grades").hasAnyAuthority("ROLE_ADMIN", "ROLE_LECTURER")
                        .requestMatchers(HttpMethod.PUT, "/api/grades/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_LECTURER")
                        .requestMatchers(HttpMethod.DELETE, "/api/grades/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_LECTURER")
                        .requestMatchers(HttpMethod.GET, "/api/grades/**").authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http, BCryptPasswordEncoder bCryptPasswordEncoder)
            throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 🔧 CRITICAL FIX: Use List.of() and avoid duplicate origin entries
        configuration.setAllowedOriginPatterns(List.of(
                "chrome-extension://*",
                "moz-extension://*",
                "http://localhost:*",
                "http://13.49.225.86:*"
        ));

        // Allow all headers and methods
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));

        // 🔧 IMPORTANT: Set credentials to true for authenticated requests
        configuration.setAllowCredentials(true);

        // 🔧 FIX: Add preflight max age to cache preflight requests (1 hour)
        configuration.setMaxAge(3600L);

        // 🔧 FIX: Expose headers that extensions might need
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}