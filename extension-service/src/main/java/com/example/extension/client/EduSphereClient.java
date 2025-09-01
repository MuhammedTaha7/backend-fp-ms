package com.example.extension.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
public class EduSphereClient {

    private final RestTemplate restTemplate;
    // You should use a configuration property for this URL
    private final String EDUSPHERE_SERVICE_URL = "http://13.61.114.153:8082/api";

    @Autowired
    public EduSphereClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Map<String, Object>> getUserCourses(String userId, String userRole) {
        String url = UriComponentsBuilder.fromHttpUrl(EDUSPHERE_SERVICE_URL + "/courses/user-courses")
                .queryParam("userId", userId)
                .queryParam("userRole", userRole)
                .toUriString();

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );
        return response.getBody() != null ? response.getBody() : new ArrayList<>();
    }

    public String getCourseName(String courseId) {
        String url = EDUSPHERE_SERVICE_URL + "/courses/name/" + courseId;
        return restTemplate.getForObject(url, String.class);
    }

    // You'll need to add endpoints for tasks, meetings, and task submissions
    public List<Map<String, Object>> getTasksByCourseIds(List<String> courseIds) {
        String url = UriComponentsBuilder.fromHttpUrl(EDUSPHERE_SERVICE_URL + "/tasks/by-courses")
                .queryParam("courseIds", String.join(",", courseIds))
                .toUriString();
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        return response.getBody() != null ? response.getBody() : new ArrayList<>();
    }

    public List<Map<String, Object>> getMeetingsByCourseIds(List<String> courseIds) {
        String url = UriComponentsBuilder.fromHttpUrl(EDUSPHERE_SERVICE_URL + "/meetings/by-courses")
                .queryParam("courseIds", String.join(",", courseIds))
                .toUriString();
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        return response.getBody() != null ? response.getBody() : new ArrayList<>();
    }

    public Map<String, Object> getMeetingById(String meetingId, String email) {
        String url = UriComponentsBuilder.fromHttpUrl(EDUSPHERE_SERVICE_URL + "/meetings/" + meetingId)
                .queryParam("email", email)
                .toUriString();
        return restTemplate.getForObject(url, Map.class);
    }

    public boolean canUserAccessCourse(String userId, String userRole, String courseId) {
        String url = UriComponentsBuilder.fromHttpUrl(EDUSPHERE_SERVICE_URL + "/courses/can-access")
                .queryParam("userId", userId)
                .queryParam("userRole", userRole)
                .queryParam("courseId", courseId)
                .toUriString();

        try {
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
            return response.getBody() != null && response.getBody();
        } catch (Exception e) {
            return false;
        }
    }
}