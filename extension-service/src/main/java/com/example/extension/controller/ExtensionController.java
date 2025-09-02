package com.example.extension.controller;

import com.example.extension.dto.response.ExtensionDashboardResponse;
import com.example.extension.service.ExtensionService;
import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/extension")
@CrossOrigin(origins = {"chrome-extension://*", "http://localhost:3000", "https://localhost:3000"},
        allowCredentials = "true",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ExtensionController {

    private final ExtensionService extensionService;
    private final UserRepository userRepository;

    public ExtensionController(ExtensionService extensionService, UserRepository userRepository) {
        this.extensionService = extensionService;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/extension/dashboard : Get dashboard data for extension
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData(@RequestParam String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email parameter is required"));
            }

            UserEntity user = userRepository.findByEmail(email.trim())
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            ExtensionDashboardResponse dashboardData = extensionService.getDashboardData(user.getId(), user.getRole());

            return ResponseEntity.ok(dashboardData);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/extension/meeting/{id} : Get specific meeting details for joining
     */
    @GetMapping("/meeting/{id}")
    public ResponseEntity<?> getMeetingDetails(@PathVariable String id, @RequestParam String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email parameter is required"));
            }

            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Meeting ID is required"));
            }

            Map<String, Object> meetingDetails = extensionService.getMeetingDetails(id, email);

            return ResponseEntity.ok(meetingDetails);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/extension/tasks : Get tasks for extension
     */
    @GetMapping("/tasks")
    public ResponseEntity<?> getTasks(
            @RequestParam String email,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email parameter is required"));
            }

            UserEntity user = userRepository.findByEmail(email.trim())
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            var tasks = extensionService.getTasks(user.getId(), user.getRole(), status, priority, type, limit);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "tasks", tasks,
                    "count", tasks.size()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/extension/announcements : Get announcements for extension
     */
    @GetMapping("/announcements")
    public ResponseEntity<?> getAnnouncements(
            @RequestParam String email,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email parameter is required"));
            }

            UserEntity user = userRepository.findByEmail(email.trim())
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            var announcements = extensionService.getAnnouncements(user.getId(), user.getRole(), limit);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "announcements", announcements,
                    "count", announcements.size()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/extension/stats : Get statistics for extension
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestParam String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email parameter is required"));
            }

            UserEntity user = userRepository.findByEmail(email.trim())
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            var stats = extensionService.getUserStats(user.getId(), user.getRole());

            return ResponseEntity.ok(stats);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/extension/urgent : Get urgent items for extension
     */
    @GetMapping("/urgent")
    public ResponseEntity<?> getUrgentItems(@RequestParam String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email parameter is required"));
            }

            UserEntity user = userRepository.findByEmail(email.trim())
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            var urgentItems = extensionService.getUrgentItems(user.getId(), user.getRole());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "urgentItems", urgentItems,
                    "count", urgentItems.size()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * OPTIONS request handler for CORS preflight
     */
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptionsRequest() {
        return ResponseEntity.ok().build();
    }
}