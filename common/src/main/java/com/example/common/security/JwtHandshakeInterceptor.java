// common/src/main/java/com/example/common/security/JwtHandshakeInterceptor.java
package com.example.common.security;

import com.example.common.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.security.core.userdetails.UserDetailsService; // ⬅️ Add this import

import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService; // ⬅️ Change the type here

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        UriComponents uriComponents = UriComponentsBuilder.fromUri(request.getURI()).build();
        String token = uriComponents.getQueryParams().getFirst("token");

        if (token != null && jwtUtil.isTokenValidSafe(token)) {
            String username = jwtUtil.extractUsername(token);
            if (username != null) {
                // Get the UserDetails object from UserDetailsService
                var userDetails = userDetailsService.loadUserByUsername(username); // ⬅️ This method is now available

                // Create and set the authentication object
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                // Store authentication in WebSocket session attributes
                attributes.put("userAuth", authentication);
                return true; // Continue with handshake
            }
        }

        // If the token is missing or invalid, deny the handshake
        response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do here
    }
}