package com.example.edusphere.controller;

import com.example.common.dto.request.LoginRequest;
import com.example.common.dto.request.RegisterRequest;
import com.example.edusphere.dto.response.AuthResponse;
import com.example.common.dto.response.LoginResponse;
import com.example.common.service.UserService;
import com.example.common.security.JwtUtil;
import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest registerRequest) {
        userService.registerUser(registerRequest);
        return ResponseEntity.ok("User registered successfully!");
    }

//    @PostMapping("/login")
//    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
//        LoginResponse loginResponse = userService.authenticateUser(loginRequest);
//
//        ResponseCookie jwtCookie = ResponseCookie.from("jwtToken", loginResponse.getToken())
//                .httpOnly(true)        // ✅ safer
//                .secure(false)         // ✅ keep false for localhost, true on AWS/HTTPS
//                .path("/")             // ✅ cookie valid for all paths
//                .sameSite("None")      // ✅ required for cross-domain cookies
//                .maxAge(7 * 24 * 60 * 60)
//                .build();
//
//        response.addHeader("Set-Cookie", jwtCookie.toString());
//
//        return ResponseEntity.ok("Login Successful");
//    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        LoginResponse loginResponse = userService.authenticateUser(loginRequest);

        // ✅ Instead of setting a cookie, return the token in the response body
        return ResponseEntity.ok(loginResponse);
    }


    @GetMapping("/auth/user")
    public ResponseEntity<AuthResponse> getUserData(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        try {
            // --- FIX: Get the full UserEntity object directly from the principal ---
            UserEntity user = (UserEntity) authentication.getPrincipal();

            return ResponseEntity.ok(
                    AuthResponse.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .name(user.getName())
                            .profilePic(user.getProfilePic())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * NEW: Extension authentication endpoint - authenticate by email only
     * POST /api/auth/extension : Authenticate extension user by email
     */
    @PostMapping("/auth/extension")
    public ResponseEntity<?> authenticateExtension(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email is required"));
            }

            // Find user by email
            UserEntity user = userRepository.findByEmail(email.trim())
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            // Generate JWT token for extension with extended expiry
            String token = jwtUtil.generateExtensionToken(user.getUsername(), user.getEmail(), user.getRole());

            // Create auth response
            AuthResponse authResponse = AuthResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .name(user.getName())
                    .profilePic(user.getProfilePic())
                    .build();

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Extension authentication successful",
                    "token", token,
                    "user", authResponse
            );
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.err.println("❌ Extension auth error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected extension auth error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/auth/extension/verify : Verify extension token
     */
    @GetMapping("/auth/extension/verify")
    public ResponseEntity<?> verifyExtensionToken(@RequestHeader("Authorization") String authHeader) {
        try {

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid authorization header"));
            }

            String token = authHeader.substring(7);

            // Validate the token and extract username
            if (!jwtUtil.isTokenValidSafe(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired token"));
            }

            String username = jwtUtil.extractUsername(token);

            if (username == null || !jwtUtil.validateToken(token, username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired token"));
            }

            // Get user details
            UserEntity user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            AuthResponse authResponse = AuthResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .name(user.getName())
                    .profilePic(user.getProfilePic())
                    .build();
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "user", authResponse
            ));

        } catch (Exception e) {
            System.err.println("❌ Token verification error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token verification failed"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        // ✅ Remove JWT cookie
        Cookie jwtCookie = new Cookie("jwtToken", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false); // Allow localhost
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0); // Expire immediately
        response.addCookie(jwtCookie);

        return ResponseEntity.ok("Logged out successfully");
    }
}