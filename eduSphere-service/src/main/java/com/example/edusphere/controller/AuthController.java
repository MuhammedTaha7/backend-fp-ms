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

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {

        LoginResponse loginResponse = userService.authenticateUser(loginRequest);

        // ✅ Store JWT Token in HTTP-only Cookie
        Cookie jwtCookie = new Cookie("jwtToken", loginResponse.getToken());
        jwtCookie.setHttpOnly(false); // Prevent JavaScript access
        jwtCookie.setSecure(false);  // Allow use on localhost (should be true for production)
        jwtCookie.setPath("/");      // Available across the app
        jwtCookie.setMaxAge(7 * 24 * 60 * 60); // Expires in 7 days
        response.addCookie(jwtCookie);

        return ResponseEntity.ok("Login Successful");
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
            System.out.println("❌ Error getting user data: " + e.getMessage());
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
            System.out.println("🔌 === EXTENSION AUTHENTICATION ===");
            String email = request.get("email");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email is required"));
            }

            System.out.println("📧 Extension auth request for email: " + email);

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

            System.out.println("✅ Extension authentication successful for: " + user.getName());
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
            System.out.println("🔍 === VERIFYING EXTENSION TOKEN ===");

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

            System.out.println("✅ Extension token verified for: " + user.getName());
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