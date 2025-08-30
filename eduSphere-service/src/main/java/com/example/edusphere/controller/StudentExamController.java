package com.example.edusphere.controller;

import com.example.edusphere.entity.ExamResponse;
import com.example.edusphere.service.StudentExamService;
import com.example.common.service.UserService;
import com.example.edusphere.dto.request.ExamResponseRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.*;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class StudentExamController {

    private final StudentExamService studentExamService;
    private final UserService userService;

    public StudentExamController(StudentExamService studentExamService, UserService userService) {
        this.studentExamService = studentExamService;
        this.userService = userService;
    }

    // ===================================
    // USER ID HELPER METHOD
    // ===================================

    private String getUserIdFromAuth(Authentication auth) {
        String authName = auth.getName();

        // Check if authName is already a valid MongoDB ObjectId format
        if (authName != null && authName.matches("^[0-9a-fA-F]{24}$")) {
            return authName;
        }

        // Try to extract from custom principal
        try {
            Object principal = auth.getPrincipal();
            if (principal != null && principal.getClass().getSimpleName().equals("UserPrincipal")) {
                java.lang.reflect.Method getUserIdMethod = principal.getClass().getMethod("getUserId");
                String userId = (String) getUserIdMethod.invoke(principal);
                if (userId != null && !userId.isEmpty()) {
                    return userId;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Could not extract user ID from principal: " + e.getMessage());
        }

        // Lookup user ID by username using UserService
        try {
            var user = userService.findByUsername(authName);
            if (user != null) {
                return user.getId();
            }
            System.err.println("❌ User not found for username: " + authName);
        } catch (Exception e) {
            System.err.println("❌ Error resolving username to user ID: " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback
        System.err.println("⚠️ FALLBACK: Using username instead of user ID: " + authName);
        return authName;
    }

    // ===================================
    // EXAM LISTING AND VIEWING
    // ===================================

    /**
     * GET /api/student/courses/{courseId}/exams : Get available exams for student
     */
    @GetMapping("/courses/{courseId}/exams")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getAvailableExams(@PathVariable String courseId, Authentication auth) {
        try {

            String studentId = getUserIdFromAuth(auth);
            List<Map<String, Object>> exams = studentExamService.getAvailableExamsForStudent(studentId, courseId);
            return ResponseEntity.ok(exams);
        } catch (Exception e) {
            System.err.println("❌ Error fetching available exams: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch available exams: " + e.getMessage()));
        }
    }

    /**
     * GET /api/student/exams/{examId} : Get exam details for student
     */
    @GetMapping("/exams/{examId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getStudentExamDetails(@PathVariable String examId, Authentication auth) {
        try {

            String studentId = getUserIdFromAuth(auth);
            Map<String, Object> examDetails = studentExamService.getStudentExamDetails(examId, studentId);
            return ResponseEntity.ok(examDetails);
        } catch (RuntimeException e) {
            System.err.println("❌ Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch exam details: " + e.getMessage()));
        }
    }

    // ===================================
    // EXAM ATTEMPT MANAGEMENT
    // ===================================

    /**
     * POST /api/student/exams/{examId}/start : Start exam attempt
     */
    @PostMapping("/exams/{examId}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> startExamAttempt(@PathVariable String examId, Authentication auth) {
        try {

            String studentId = getUserIdFromAuth(auth);
            Map<String, Object> examAttempt = studentExamService.startExamAttempt(examId, studentId);
            return new ResponseEntity<>(examAttempt, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            System.err.println("❌ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to start exam: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/student/exams/{examId}/save-progress : Save exam progress
     */
    @PutMapping("/exams/{examId}/save-progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> saveExamProgress(@PathVariable String examId,
                                              @Valid @RequestBody ExamResponseRequest request,
                                              Authentication auth) {
        try {

            String studentId = getUserIdFromAuth(auth);
            request.setExamId(examId); // Ensure exam ID is set

            ExamResponse response = studentExamService.saveExamProgress(request, studentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.err.println("❌ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save progress: " + e.getMessage()));
        }
    }

    /**
     * POST /api/student/exams/{examId}/submit : Submit exam
     */
    @PostMapping("/exams/{examId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> submitExam(@PathVariable String examId,
                                        @Valid @RequestBody ExamResponseRequest request,
                                        Authentication auth) {
        try {

            String studentId = getUserIdFromAuth(auth);
            request.setExamId(examId); // Ensure exam ID is set

            Map<String, Object> result = studentExamService.submitExam(request, studentId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            System.err.println("❌ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to submit exam: " + e.getMessage()));
        }
    }

    /**
     * POST /api/student/exams/{examId}/resume : Resume active exam attempt
     */
    @PostMapping("/exams/{examId}/resume")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> resumeExamAttempt(@PathVariable String examId, Authentication auth) {
        try {

            String studentId = getUserIdFromAuth(auth);
            Map<String, Object> resumedAttempt = studentExamService.resumeExamAttempt(examId, studentId);
            return ResponseEntity.ok(resumedAttempt);
        } catch (RuntimeException e) {
            System.err.println("❌ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to resume exam: " + e.getMessage()));
        }
    }

    // ===================================
    // EXAM ELIGIBILITY AND STATUS
    // ===================================

    /**
     * GET /api/student/exams/{examId}/eligibility : Check exam eligibility
     */
    @GetMapping("/exams/{examId}/eligibility")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> checkExamEligibility(@PathVariable String examId, Authentication auth) {
        try {

            String studentId = getUserIdFromAuth(auth);
            Map<String, Object> eligibility = studentExamService.checkExamEligibility(examId, studentId);
            return ResponseEntity.ok(eligibility);
        } catch (Exception e) {
            System.err.println("❌ Error checking eligibility: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check exam eligibility: " + e.getMessage()));
        }
    }

    /**
     * GET /api/student/exams/{examId}/attempts : Get exam attempt history
     */
    @GetMapping("/exams/{examId}/attempts")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getExamAttemptHistory(@PathVariable String examId, Authentication auth) {
        try {

            String studentId = getUserIdFromAuth(auth);
            List<Map<String, Object>> attempts = studentExamService.getStudentAttemptHistory(examId, studentId);
            return ResponseEntity.ok(attempts);
        } catch (Exception e) {
            System.err.println("❌ Error fetching attempt history: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch attempt history: " + e.getMessage()));
        }
    }

    // ===================================
    // EXAM RESULTS AND FEEDBACK
    // ===================================

    /**
     * GET /api/student/exam-responses/{responseId}/results : Get exam results
     */
    @GetMapping("/exam-responses/{responseId}/results")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getExamResults(@PathVariable String responseId, Authentication auth) {
        try {

            String studentId = getUserIdFromAuth(auth);
            Map<String, Object> results = studentExamService.getStudentExamResults(responseId, studentId);
            return ResponseEntity.ok(results);
        } catch (RuntimeException e) {
            System.err.println("❌ Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch exam results: " + e.getMessage()));
        }
    }

    /**
     * GET /api/student/exam-responses/{responseId}/detailed : Get detailed exam results
     */
    @GetMapping("/exam-responses/{responseId}/detailed")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getDetailedExamResults(@PathVariable String responseId, Authentication auth) {
        try {

            String studentId = getUserIdFromAuth(auth);
            Map<String, Object> detailedResults = studentExamService.getDetailedExamResults(responseId, studentId);
            return ResponseEntity.ok(detailedResults);
        } catch (RuntimeException e) {
            System.err.println("❌ Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch detailed results: " + e.getMessage()));
        }
    }

    // ===================================
    // STUDENT STATISTICS
    // ===================================

    /**
     * GET /api/student/courses/{courseId}/exam-stats : Get student exam statistics for course
     */
    @GetMapping("/courses/{courseId}/exam-stats")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getStudentExamStats(@PathVariable String courseId, Authentication auth) {
        try {

            String studentId = getUserIdFromAuth(auth);
            Map<String, Object> stats = studentExamService.getStudentExamStats(studentId, courseId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("❌ Error fetching exam stats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch exam statistics: " + e.getMessage()));
        }
    }

    // ===================================
    // UTILITY ENDPOINTS
    // ===================================

    /**
     * GET /api/student/exams/{examId}/active-attempt : Check for active attempt
     */
    @GetMapping("/exams/{examId}/active-attempt")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> checkActiveAttempt(@PathVariable String examId, Authentication auth) {
        try {

            String studentId = getUserIdFromAuth(auth);
            Map<String, Object> activeAttempt = studentExamService.checkActiveAttempt(examId, studentId);
            return ResponseEntity.ok(activeAttempt);
        } catch (Exception e) {
            System.err.println("❌ Error checking active attempt: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check active attempt: " + e.getMessage()));
        }
    }

    /**
     * GET /api/student/dashboard/exam-summary : Get exam summary for dashboard
     */
    @GetMapping("/dashboard/exam-summary")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getExamDashboardSummary(Authentication auth) {
        try {

            String studentId = getUserIdFromAuth(auth);
            Map<String, Object> summary = studentExamService.getExamDashboardSummary(studentId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            System.err.println("❌ Error fetching dashboard summary: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dashboard summary: " + e.getMessage()));
        }
    }
}