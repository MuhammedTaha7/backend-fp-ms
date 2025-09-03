package com.example.common.config;

import com.example.common.repository.UserRepository;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    @Autowired
    private UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public Endpoints (must be defined first)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/register", "/api/login").permitAll()
                        .requestMatchers("/api/auth/extension", "/api/auth/extension/**").permitAll()
                        .requestMatchers("/api/extension/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()

                        // Authenticated Endpoints (for all services)

                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers("/api/chatbot/**").authenticated()
                        .requestMatchers("/api/community/**").authenticated()
                        .requestMatchers("/api/friends/**").authenticated()
                        .requestMatchers("/api/jobs/**").authenticated()
                        .requestMatchers("/api/cv/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/messages/**").authenticated()
                        .requestMatchers("/api/calendar/**").authenticated()
                        .requestMatchers("/api/users/profile/**").authenticated()
                        .requestMatchers("/api/users/search").authenticated()
                        .requestMatchers("/api/students/**").authenticated()
                        .requestMatchers("/api/lecturers/**").authenticated()
                        .requestMatchers("/api/courses/**").authenticated()
                        .requestMatchers("/api/course-content/**").authenticated()
                        .requestMatchers("/api/resources/**").authenticated()
                        .requestMatchers("/api/exams/**").authenticated()
                        .requestMatchers("/api/auth/user").authenticated()

                        // EXAM RESPONSES - Specific role-based permissions
//                        .requestMatchers(HttpMethod.GET, "/api/exam-responses/**").hasAnyAuthority("ROLE_LECTURER", "ROLE_STUDENT")
//                        .requestMatchers(HttpMethod.PUT, "/api/exam-responses/manual-grade").hasAuthority("ROLE_LECTURER")
//                        .requestMatchers(HttpMethod.PUT, "/api/exam-responses/grade").hasAuthority("ROLE_LECTURER")
//                        .requestMatchers(HttpMethod.PUT, "/api/exam-responses/*/question-score").hasAuthority("ROLE_LECTURER")
//                        .requestMatchers(HttpMethod.POST, "/api/exam-responses/*/auto-grade").hasAuthority("ROLE_LECTURER")
//                        .requestMatchers(HttpMethod.PUT, "/api/exam-responses/*/flag").hasAuthority("ROLE_LECTURER")
//                        .requestMatchers(HttpMethod.PUT, "/api/exam-responses/*/unflag").hasAuthority("ROLE_LECTURER")
//                        .requestMatchers(HttpMethod.POST, "/api/exam-responses/batch-grade").hasAuthority("ROLE_LECTURER")
//                        .requestMatchers(HttpMethod.POST, "/api/exam-responses/export-detailed").hasAuthority("ROLE_LECTURER")
//                        .requestMatchers("/api/exam-responses/**").authenticated()

                        // GRADES - Role-based permissions
                        .requestMatchers(HttpMethod.POST, "/api/grades").hasAnyAuthority("ROLE_ADMIN", "ROLE_LECTURER")
                        .requestMatchers(HttpMethod.PUT, "/api/grades/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_LECTURER")
                        .requestMatchers(HttpMethod.DELETE, "/api/grades/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_LECTURER")
                        .requestMatchers("/api/grades/**").authenticated()

                        // COURSES - Admin only for modifications
                        .requestMatchers(HttpMethod.POST, "/api/courses").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/courses/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/courses/*/enroll").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/*/enrollments").hasAuthority("ROLE_ADMIN")

                        // USER MANAGEMENT - Role-based permissions
                        .requestMatchers(HttpMethod.POST, "/api/users/admin-create").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/by-ids").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/role/**").authenticated()

                        // ADMIN ENDPOINTS
                        .requestMatchers("/api/reports/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/departments/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/profile-analytics/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                        // Fallback - All other requests must be authenticated
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
        configuration.setAllowedOriginPatterns(List.of(
                "chrome-extension://*",
                "moz-extension://*",
                "http://localhost:*",
                "http://13.49.225.86:*",
                "http://13.61.114.153:*"  // Added your current server IP
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
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