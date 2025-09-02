// common/src/main/java/com/example/common/security/JwtRequestFilter.java
package com.example.common.security;

import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = null;
        String username = null;

        // 1. Check Authorization Header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        // 2. Check Query Param (for WebSocket and fallbacks)
        else if (request.getRequestURI().startsWith("/ws")) {
            token = request.getParameter("token");
        }

        if (token != null) {
            try {
                username = jwtUtil.extractUsername(token);
            } catch (Exception e) {
                // Invalid token
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<UserEntity> userOptional = userRepository.findByUsername(username);
                if (userOptional.isPresent()) {
                    UserEntity userDetails = userOptional.get();
                    if (jwtUtil.isTokenValid(token, userDetails.getUsername())) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        }

        chain.doFilter(request, response);
    }
}