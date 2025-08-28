package com.example.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final String SECRET_STRING = "dGhpc2lzYXNlY3JldGtleWZvcm15c3ByaW5nYm9vdGFwcGxpY2F0aW9uZm9yZWR1c3BoZXJlMjAyNA==";
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_STRING));

    // ✅ Generate JWT Token with username, email, and role
    public String generateToken(String username, String email, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("email", email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1-hour expiration
                .signWith(SECRET_KEY)
                .compact();
    }

    // ✅ NEW: Generate JWT Token for Extension (simplified - only username needed)
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim("source", "extension") // Mark as extension token
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 3600000)) // 7-day expiration for extension
                .signWith(SECRET_KEY)
                .compact();
    }

    // ✅ NEW: Generate JWT Token for Extension with extended expiry
    public String generateExtensionToken(String username, String email, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("email", email)
                .claim("role", role)
                .claim("source", "extension") // Mark as extension token
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 3600000)) // 7-day expiration
                .signWith(SECRET_KEY)
                .compact();
    }

    // ✅ Extract Username (Stored in `sub`)
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ✅ Extract Email (Stored in `email` claim)
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    // ✅ Extract Role (Stored in `role` claim)
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    // ✅ NEW: Extract Source (to identify extension tokens)
    public String extractSource(String token) {
        return extractClaim(token, claims -> claims.get("source", String.class));
    }

    // ✅ NEW: Check if token is from extension
    public boolean isExtensionToken(String token) {
        try {
            String source = extractSource(token);
            return "extension".equals(source);
        } catch (Exception e) {
            return false;
        }
    }

    // ✅ Validate Token
    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return extractedUsername.equals(username) && !isTokenExpired(token);
    }

    // ✅ NEW: Validate Token (overloaded method without username parameter)
    public boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return extractedUsername.equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    // ✅ NEW: Validate Extension Token
    public boolean isExtensionTokenValid(String token, String username) {
        try {
            if (!isExtensionToken(token)) {
                return false;
            }
            final String extractedUsername = extractUsername(token);
            return extractedUsername.equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    // ✅ Check if Token is Expired
    public boolean isTokenExpired(String token) {
        try {
            return extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    // ✅ NEW: Get token expiration date
    public Date getTokenExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ✅ NEW: Get token issued date
    public Date getTokenIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    // ✅ NEW: Get remaining token validity time in milliseconds
    public long getTokenRemainingTime(String token) {
        try {
            Date expiration = getTokenExpiration(token);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    // ✅ Extract Specific Claim
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ✅ Extract All Claims Securely
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException e) {
            throw new RuntimeException("Invalid or expired token", e);
        }
    }

    // ✅ NEW: Safe token validation without throwing exceptions
    public boolean isTokenValidSafe(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    // ✅ NEW: Extract all token information safely
    public TokenInfo extractTokenInfo(String token) {
        try {
            String username = extractUsername(token);
            String email = extractEmail(token);
            String role = extractRole(token);
            String source = extractSource(token);
            Date issuedAt = getTokenIssuedAt(token);
            Date expiration = getTokenExpiration(token);
            boolean isExpired = isTokenExpired(token);
            boolean isExtension = isExtensionToken(token);

            return new TokenInfo(username, email, role, source, issuedAt, expiration, isExpired, isExtension);
        } catch (Exception e) {
            return null;
        }
    }

    // ✅ NEW: Token information container class
    public static class TokenInfo {
        private final String username;
        private final String email;
        private final String role;
        private final String source;
        private final Date issuedAt;
        private final Date expiration;
        private final boolean isExpired;
        private final boolean isExtension;

        public TokenInfo(String username, String email, String role, String source,
                         Date issuedAt, Date expiration, boolean isExpired, boolean isExtension) {
            this.username = username;
            this.email = email;
            this.role = role;
            this.source = source;
            this.issuedAt = issuedAt;
            this.expiration = expiration;
            this.isExpired = isExpired;
            this.isExtension = isExtension;
        }

        // Getters
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getSource() { return source; }
        public Date getIssuedAt() { return issuedAt; }
        public Date getExpiration() { return expiration; }
        public boolean isExpired() { return isExpired; }
        public boolean isExtension() { return isExtension; }
    }
}