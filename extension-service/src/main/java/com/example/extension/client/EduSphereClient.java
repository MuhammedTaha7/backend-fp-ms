package com.example.extension.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.common.security.JwtUtil;
import com.example.common.repository.UserRepository;
import com.example.common.entity.UserEntity;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
public class EduSphereClient {

    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${edusphere.service.url:http://13.61.114.153:8082/api}")
    private String EDUSPHERE_SERVICE_URL;

    @Autowired
    public EduSphereClient(RestTemplate restTemplate, JwtUtil jwtUtil, UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    /**
     * Create authenticated headers for service-to-service calls
     */
    private HttpHeaders createAuthenticatedHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            // Get user details to generate a service token
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                // Generate a service-to-service JWT token
                String serviceToken = jwtUtil.generateToken(user.getUsername(), user.getEmail(), user.getRole());
                headers.setBearerAuth(serviceToken);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not create authenticated headers: " + e.getMessage());
        }

        return headers;
    }

    /**
     * Make authenticated GET request
     */
    private <T> ResponseEntity<T> makeAuthenticatedGetRequest(String url, String userId,
                                                              ParameterizedTypeReference<T> responseType) {
        try {
            HttpHeaders headers = createAuthenticatedHeaders(userId);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
        } catch (HttpClientErrorException e) {
            System.err.println("❌ HTTP Error calling " + url + ": " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Error calling " + url + ": " + e.getMessage());
            throw e;
        }
    }

    public List<Map<String, Object>> getUserCourses(String userId, String userRole) {
        String url = UriComponentsBuilder.fromHttpUrl(EDUSPHERE_SERVICE_URL + "/courses/user-courses")
                .queryParam("userId", userId)
                .queryParam("userRole", userRole)
                .toUriString();

        try {
            ResponseEntity<List<Map<String, Object>>> response = makeAuthenticatedGetRequest(
                    url, userId, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("❌ Error getting user courses: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public String getCourseName(String courseId) {
        String url = EDUSPHERE_SERVICE_URL + "/courses/name/" + courseId;
        try {
            // For course name, we'll use a simplified approach since we don't have userId context here
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("❌ Error getting course name for " + courseId + ": " + e.getMessage());
            return "Unknown Course";
        }
    }

    public List<Map<String, Object>> getTasksByCourseIds(List<String> courseIds) {
        if (courseIds.isEmpty()) {
            return new ArrayList<>();
        }

        String url = UriComponentsBuilder.fromHttpUrl(EDUSPHERE_SERVICE_URL + "/tasks/by-courses")
                .queryParam("courseIds", String.join(",", courseIds))
                .toUriString();

        try {
            // We'll use the first course's context for authentication - this is a limitation
            // In a real system, you'd pass the requesting user's ID through the chain
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("❌ Error getting tasks by course IDs: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getMeetingsByCourseIds(List<String> courseIds) {
        if (courseIds.isEmpty()) {
            return new ArrayList<>();
        }

        String url = UriComponentsBuilder.fromHttpUrl(EDUSPHERE_SERVICE_URL + "/meetings/by-courses")
                .queryParam("courseIds", String.join(",", courseIds))
                .toUriString();

        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("❌ Error getting meetings by course IDs: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getMeetingById(String meetingId, String email) {
        String url = UriComponentsBuilder.fromHttpUrl(EDUSPHERE_SERVICE_URL + "/meetings/" + meetingId)
                .queryParam("email", email)
                .toUriString();

        try {
            // Try to find user by email to get proper authentication
            UserEntity user = userRepository.findByEmail(email).orElse(null);
            String userId = user != null ? user.getId() : null;

            if (userId != null) {
                ResponseEntity<Map> response = makeAuthenticatedGetRequest(
                        url, userId, new ParameterizedTypeReference<Map>() {}
                );
                return response.getBody();
            } else {
                // Fallback without authentication
                return restTemplate.getForObject(url, Map.class);
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting meeting by ID: " + e.getMessage());
            return Map.of("error", "Meeting not found");
        }
    }

    public boolean canUserAccessCourse(String userId, String userRole, String courseId) {
        String url = UriComponentsBuilder.fromHttpUrl(EDUSPHERE_SERVICE_URL + "/courses/can-access")
                .queryParam("userId", userId)
                .queryParam("userRole", userRole)
                .queryParam("courseId", courseId)
                .toUriString();

        try {
            ResponseEntity<Boolean> response = makeAuthenticatedGetRequest(
                    url, userId, new ParameterizedTypeReference<Boolean>() {}
            );
            return response.getBody() != null && response.getBody();
        } catch (Exception e) {
            System.err.println("❌ Error checking course access: " + e.getMessage());
            return false;
        }
    }
}