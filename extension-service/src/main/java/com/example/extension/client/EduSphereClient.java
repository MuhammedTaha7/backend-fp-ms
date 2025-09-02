// EduSphereClient.java
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
import java.util.HashMap;

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
            System.err.println("HTTP Error calling " + url + ": " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            System.err.println("Error calling " + url + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get user courses - using existing endpoints based on user role
     */
    public List<Map<String, Object>> getUserCourses(String userId, String userRole) {
        try {
            List<Map<String, Object>> courses = new ArrayList<>();

            if ("1200".equals(userRole)) {
                // Lecturer - use the lecturer courses endpoint
                String url = EDUSPHERE_SERVICE_URL + "/courses/lecturer";
                ResponseEntity<List> response = makeAuthenticatedGetRequest(
                        url, userId, new ParameterizedTypeReference<List>() {}
                );

                if (response.getBody() != null) {
                    List<Object> lecturerCourses = response.getBody();
                    courses = lecturerCourses.stream()
                            .map(this::convertCourseToMap)
                            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                }
            } else if ("1300".equals(userRole)) {
                // Student - use the student courses endpoint
                String url = EDUSPHERE_SERVICE_URL + "/courses/student";
                ResponseEntity<List> response = makeAuthenticatedGetRequest(
                        url, userId, new ParameterizedTypeReference<List>() {}
                );

                if (response.getBody() != null) {
                    List<Object> studentCourses = response.getBody();
                    courses = studentCourses.stream()
                            .map(this::convertCourseToMap)
                            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                }
            }
            // Admin role would need a different endpoint if available

            return courses;
        } catch (Exception e) {
            System.err.println("Error getting user courses: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get course name - this would need to be implemented in EduSphere or use course lookup
     */
    public String getCourseName(String courseId) {
        try {
            // Since there's no specific endpoint for course name, we'll return a placeholder
            // In a real implementation, you'd either add this endpoint to EduSphere or
            // cache course names from the getUserCourses call
            return "Course " + courseId;
        } catch (Exception e) {
            System.err.println("Error getting course name for " + courseId + ": " + e.getMessage());
            return "Unknown Course";
        }
    }

    /**
     * Get tasks by course IDs - using existing task endpoints
     */
    public List<Map<String, Object>> getTasksByCourseIds(List<String> courseIds, String userId) {
        if (courseIds.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<Map<String, Object>> allTasks = new ArrayList<>();

            // The existing endpoint is /api/tasks/by-courses which expects courseIds parameter
            String url = UriComponentsBuilder.fromHttpUrl(EDUSPHERE_SERVICE_URL + "/tasks/by-courses")
                    .queryParam("courseIds", courseIds)
                    .toUriString();

            ResponseEntity<List> response = makeAuthenticatedGetRequest(
                    url, userId, new ParameterizedTypeReference<List>() {}
            );

            if (response.getBody() != null) {
                List<Object> tasks = response.getBody();
                allTasks = tasks.stream()
                        .map(this::convertTaskToMap)
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            }

            return allTasks;
        } catch (Exception e) {
            System.err.println("Error getting tasks by course IDs: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get meetings by course IDs - using existing meeting endpoints
     */
    public List<Map<String, Object>> getMeetingsByCourseIds(List<String> courseIds, String userId) {
        if (courseIds.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<Map<String, Object>> allMeetings = new ArrayList<>();

            // Use the /meetings/user endpoint, which is authenticated
            String url = EDUSPHERE_SERVICE_URL + "/meetings/user";

            ResponseEntity<List> response = makeAuthenticatedGetRequest(
                    url, userId, new ParameterizedTypeReference<List>() {}
            );

            if (response.getBody() != null) {
                List<Object> meetings = response.getBody();
                allMeetings = meetings.stream()
                        .map(this::convertMeetingToMap)
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            }

            return allMeetings;
        } catch (Exception e) {
            System.err.println("Error getting meetings by course IDs: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get announcements for a user
     */
    public List<Map<String, Object>> getAnnouncementsForUser(String userId) {
        try {
            String url = EDUSPHERE_SERVICE_URL + "/announcements";

            ResponseEntity<List> response = makeAuthenticatedGetRequest(
                    url, userId, new ParameterizedTypeReference<List>() {}
            );

            List<Map<String, Object>> announcements = new ArrayList<>();
            if (response.getBody() != null) {
                List<Object> rawAnnouncements = response.getBody();
                announcements = rawAnnouncements.stream()
                        .map(this::convertAnnouncementToMap)
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            }
            return announcements;
        } catch (Exception e) {
            System.err.println("Error getting announcements: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get meeting by ID - using existing meeting endpoint
     */
    public Map<String, Object> getMeetingById(String meetingId, String email) {
        String url = EDUSPHERE_SERVICE_URL + "/meetings/" + meetingId;

        try {
            // Try to find user by email to get proper authentication
            UserEntity user = userRepository.findByEmail(email).orElse(null);
            String userId = user != null ? user.getId() : null;

            if (userId != null) {
                ResponseEntity<Map> response = makeAuthenticatedGetRequest(
                        url, userId, new ParameterizedTypeReference<Map>() {}
                );
                return response.getBody() != null ? response.getBody() : new HashMap<>();
            } else {
                // Fallback without authentication
                return restTemplate.getForObject(url, Map.class);
            }
        } catch (Exception e) {
            System.err.println("Error getting meeting by ID: " + e.getMessage());
            return Map.of("error", "Meeting not found");
        }
    }

    /**
     * Check if user can access course - simplified logic
     */
    public boolean canUserAccessCourse(String userId, String userRole, String courseId) {
        try {
            // Get user courses and check if courseId is in the list
            List<Map<String, Object>> userCourses = getUserCourses(userId, userRole);
            return userCourses.stream()
                    .anyMatch(course -> courseId.equals(course.get("id")));
        } catch (Exception e) {
            System.err.println("Error checking course access: " + e.getMessage());
            return false;
        }
    }

    // Helper methods to convert objects to Map format
    private Map<String, Object> convertCourseToMap(Object courseObj) {
        Map<String, Object> courseMap = new HashMap<>();

        try {
            if (courseObj instanceof Map) {
                return (Map<String, Object>) courseObj;
            }

            // If it's a Course entity, extract fields using reflection or known structure
            // This is a simplified conversion - adjust based on your Course entity structure
            courseMap.put("id", getFieldValue(courseObj, "id"));
            courseMap.put("name", getFieldValue(courseObj, "name"));
            courseMap.put("description", getFieldValue(courseObj, "description"));
            courseMap.put("createdBy", getFieldValue(courseObj, "createdBy"));
            courseMap.put("semester", getFieldValue(courseObj, "semester"));
            courseMap.put("year", getFieldValue(courseObj, "year"));
            courseMap.put("credits", getFieldValue(courseObj, "credits"));

        } catch (Exception e) {
            System.err.println("Error converting course to map: " + e.getMessage());
        }

        return courseMap;
    }

    private Map<String, Object> convertTaskToMap(Object taskObj) {
        Map<String, Object> taskMap = new HashMap<>();

        try {
            if (taskObj instanceof Map) {
                return (Map<String, Object>) taskObj;
            }

            // Convert Task entity to map
            taskMap.put("id", getFieldValue(taskObj, "id"));
            taskMap.put("title", getFieldValue(taskObj, "title"));
            taskMap.put("description", getFieldValue(taskObj, "description"));
            taskMap.put("courseId", getFieldValue(taskObj, "courseId"));
            taskMap.put("dueDate", getFieldValue(taskObj, "dueDate"));
            taskMap.put("maxPoints", getFieldValue(taskObj, "maxPoints"));
            taskMap.put("category", getFieldValue(taskObj, "category"));
            taskMap.put("status", getFieldValue(taskObj, "status"));
            taskMap.put("published", getFieldValue(taskObj, "published"));
            taskMap.put("visibleToStudents", getFieldValue(taskObj, "visibleToStudents"));
            taskMap.put("fileUrl", getFieldValue(taskObj, "fileUrl"));
            taskMap.put("fileName", getFieldValue(taskObj, "fileName"));

        } catch (Exception e) {
            System.err.println("Error converting task to map: " + e.getMessage());
        }

        return taskMap;
    }

    private Map<String, Object> convertMeetingToMap(Object meetingObj) {
        Map<String, Object> meetingMap = new HashMap<>();

        try {
            if (meetingObj instanceof Map) {
                return (Map<String, Object>) meetingObj;
            }

            // Convert Meeting entity to map
            meetingMap.put("id", getFieldValue(meetingObj, "id"));
            meetingMap.put("title", getFieldValue(meetingObj, "title"));
            meetingMap.put("description", getFieldValue(meetingObj, "description"));
            meetingMap.put("courseId", getFieldValue(meetingObj, "courseId"));
            meetingMap.put("datetime", getFieldValue(meetingObj, "datetime"));
            meetingMap.put("scheduledAt", getFieldValue(meetingObj, "scheduledAt"));
            meetingMap.put("status", getFieldValue(meetingObj, "status"));
            meetingMap.put("type", getFieldValue(meetingObj, "type"));
            meetingMap.put("location", getFieldValue(meetingObj, "location"));
            meetingMap.put("invitationLink", getFieldValue(meetingObj, "invitationLink"));
            meetingMap.put("roomId", getFieldValue(meetingObj, "roomId"));
            meetingMap.put("createdBy", getFieldValue(meetingObj, "createdBy"));
            meetingMap.put("lecturerId", getFieldValue(meetingObj, "lecturerId"));
            meetingMap.put("participants", new ArrayList<>()); // Default empty list

        } catch (Exception e) {
            System.err.println("Error converting meeting to map: " + e.getMessage());
        }

        return meetingMap;
    }

    private Map<String, Object> convertAnnouncementToMap(Object announcementObj) {
        Map<String, Object> announcementMap = new HashMap<>();
        try {
            if (announcementObj instanceof Map) {
                return (Map<String, Object>) announcementObj;
            }
            announcementMap.put("id", getFieldValue(announcementObj, "id"));
            announcementMap.put("title", getFieldValue(announcementObj, "title"));
            announcementMap.put("content", getFieldValue(announcementObj, "content"));
            announcementMap.put("creatorId", getFieldValue(announcementObj, "creatorId"));
            announcementMap.put("creatorName", getFieldValue(announcementObj, "creatorName"));
            announcementMap.put("priority", getFieldValue(announcementObj, "priority"));
            announcementMap.put("status", getFieldValue(announcementObj, "status"));
            announcementMap.put("createdAt", getFieldValue(announcementObj, "createdAt"));
            announcementMap.put("expiryDate", getFieldValue(announcementObj, "expiryDate"));
            announcementMap.put("scheduledDate", getFieldValue(announcementObj, "scheduledDate"));
            announcementMap.put("targetAudienceType", getFieldValue(announcementObj, "targetAudienceType"));
            announcementMap.put("targetDepartment", getFieldValue(announcementObj, "targetDepartment"));
            announcementMap.put("targetCourseId", getFieldValue(announcementObj, "targetCourseId"));
            announcementMap.put("targetAcademicYear", getFieldValue(announcementObj, "targetAcademicYear"));
            announcementMap.put("targetUserId", getFieldValue(announcementObj, "targetUserId"));
        } catch (Exception e) {
            System.err.println("Error converting announcement to map: " + e.getMessage());
        }
        return announcementMap;
    }

    // Helper method to get field value using reflection
    private Object getFieldValue(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            // Field not found or not accessible, return null
            return null;
        }
    }
}