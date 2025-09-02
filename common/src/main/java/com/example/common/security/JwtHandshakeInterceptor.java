// common/src/main/java/com/example/common/security/JwtHandshakeInterceptor.java
package com.example.common.security;

import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        // Extract token from query parameter: ?token=xxx
        UriComponents uriComponents = UriComponentsBuilder.fromUri(request.getURI()).build();
        String token = uriComponents.getQueryParams().getFirst("token");

        if (token == null || token.isEmpty()) {
            response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
            return false;
        }

        if (!jwtUtil.isTokenValidSafe(token)) {
            response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
            return false;
        }

        String username = jwtUtil.extractUsername(token);
        if (username == null) {
            response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
            return false;
        }

        var userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
            return false;
        }

        UserEntity user = userOptional.get();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());

        // ✅ Critical: Spring STOMP uses "user" key to set principal
        attributes.put("user", authentication);

        // Optional: for debugging
        attributes.put("username", username);

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do
    }
}