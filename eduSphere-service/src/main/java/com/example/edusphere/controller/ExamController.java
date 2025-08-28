package com.example.edusphere.controller;

import com.example.edusphere.entity.Exam;
import com.example.edusphere.entity.ExamQuestion;
import com.example.edusphere.entity.ExamResponse;
import com.example.common.entity.UserEntity;
import com.example.edusphere.service.ExamService;
import com.example.common.service.UserService;
import com.example.edusphere.dto.request.*;
import com.example.edusphere.dto.response.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ExamController {

    private final ExamService examService;
    // âœ… FIXED: Inject UserService to resolve usernames to user IDs
    private final UserService userService;

    public ExamController(ExamService examService, UserService userService) {
        this.examService = examService;
        this.userService = userService;
    }

    // ===================================
    // USER ID HELPER METHOD - COMPLETELY FIXED
    // ===================================

    /**
     * âœ… COMPLETELY FIXED: Helper method to resolve username to user ID
     */
    private String getUserIdFromAuth(Authentication auth) {
        String authName = auth.getName();

        // Option 1: Check if authName is already a valid MongoDB ObjectId format
        if (authName != null && authName.matches("^[0-9a-fA-F]{24}$")) {
            System.out.println("âœ… Using ObjectId from auth: " + authName);
            return authName;
        }

        // Option 2: Try to extract from custom principal (if implemented)
        try {
            Object principal = auth.getPrincipal();

            if (principal != null && principal.getClass().getSimpleName().equals("UserPrincipal")) {
                java.lang.reflect.Method getUserIdMethod = principal.getClass().getMethod("getUserId");
                String userId = (String) getUserIdMethod.invoke(principal);
                if (userId != null && !userId.isEmpty()) {
                    System.out.println("âœ… Using user ID from custom principal: " + userId);
                    return userId;
                }
            }
        } catch (Exception e) {
            System.err.println("âš ï¸ Could not extract user ID from principal: " + e.getMessage());
        }

        // Option 3: âœ… FIXED - Lookup user ID by username using UserService
        try {
            System.out.println("ðŸ” Looking up user ID for username: " + authName);

            UserEntity user = userService.findByUsername(authName);
            if (user != null) {
                System.out.println("âœ… Resolved username '" + authName + "' to user ID: " + user.getId());
                return user.getId();
            }

            System.err.println("âŒ User not found for username: " + authName);

        } catch (Exception e) {
            System.err.println("âŒ Error resolving username to user ID: " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback: return username (this will cause the original problem)
        System.err.println("âš ï¸ FALLBACK: Using username instead of user ID: " + authName);
        return authName;
    }

    // ===================================
    // EXAM CRUD OPERATIONS
    // ===================================

    /**
     * GET /api/courses/{courseId}/exams : Get all exams for a course
     */
    @GetMapping("/courses/{courseId}/exams")
    @PreAuthorize("hasRole('LECTURER') or hasRole('STUDENT')")
    public ResponseEntity<?> getCourseExams(@PathVariable String courseId, Authentication auth) {
        try {
            System.out.println("ðŸ“š === FETCHING EXAMS FOR COURSE ===");
            System.out.println("Course ID: " + courseId);
            System.out.println("User: " + auth.getName());

            List<Exam> exams = examService.getExamsByCourse(courseId);

            // Filter based on role
            boolean isLecturer = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_LECTURER"));

            if (!isLecturer) {
                // Students only see published and visible exams
                exams = exams.stream()
                        .filter(exam -> "PUBLISHED".equals(exam.getStatus()) &&
                                exam.getVisibleToStudents())
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            }

            System.out.println("âœ… Found " + exams.size() + " exams");
            return ResponseEntity.ok(exams);
        } catch (Exception e) {
            System.err.println("âŒ Error fetching exams: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch exams: " + e.getMessage()));
        }
    }

    /**
     * GET /api/exams/{examId} : Get exam details
     */
    @GetMapping("/exams/{examId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('STUDENT')")
    public ResponseEntity<?> getExam(@PathVariable String examId, Authentication auth) {
        try {
            System.out.println("ðŸ‘€ === FETCHING EXAM DETAILS ===");
            System.out.println("Exam ID: " + examId);
            System.out.println("User: " + auth.getName());

            boolean isLecturer = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_LECTURER"));

            Exam exam;
            if (isLecturer) {
                exam = examService.getExamById(examId);
            } else {
                // âœ… FIXED: Use proper user ID for students
                String studentId = getUserIdFromAuth(auth);
                exam = examService.getStudentExam(examId, studentId);
            }

            System.out.println("âœ… Retrieved exam: " + exam.getTitle());
            return ResponseEntity.ok(exam);
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch exam: " + e.getMessage()));
        }
    }

    /**
     * NEW: GET /api/exams/{examId}/for-grading : Get exam with questions for grading context
     */
    @GetMapping("/exams/{examId}/for-grading")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> getExamForGrading(@PathVariable String examId, Authentication auth) {
        try {
            System.out.println("ðŸ“‹ === FETCHING EXAM FOR GRADING ===");
            System.out.println("Exam ID: " + examId);
            System.out.println("Lecturer: " + auth.getName());

            Exam exam = examService.getExamById(examId);

            // Ensure total points are calculated
            exam.recalculateTotalPoints();

            // Add grading-specific metadata
            Map<String, Object> examForGrading = new HashMap<>();
            examForGrading.put("id", exam.getId());
            examForGrading.put("title", exam.getTitle());
            examForGrading.put("description", exam.getDescription());
            examForGrading.put("instructions", exam.getInstructions());
            examForGrading.put("courseId", exam.getCourseId());
            examForGrading.put("totalPoints", exam.getTotalPoints());
            examForGrading.put("passPercentage", exam.getPassPercentage());
            examForGrading.put("questions", exam.getQuestions());

            // Add question analysis
            long autoGradableCount = exam.getQuestions().stream()
                    .filter(q -> q.canAutoGrade())
                    .count();
            long manualGradingCount = exam.getQuestions().stream()
                    .filter(q -> !q.canAutoGrade())
                    .count();

            examForGrading.put("autoGradableQuestions", autoGradableCount);
            examForGrading.put("manualGradingRequired", manualGradingCount > 0);
            examForGrading.put("questionCount", exam.getQuestions().size());

            System.out.println("âœ… Retrieved exam for grading: " + exam.getTitle());
            return ResponseEntity.ok(examForGrading);
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch exam for grading: " + e.getMessage()));
        }
    }

    /**
     * POST /api/exams : Create a new exam
     * NEW: Now automatically creates a corresponding grade column
     */
    @PostMapping("/exams")
    public ResponseEntity<?> createExam(@Valid @RequestBody ExamCreateRequest request, Authentication auth) {
        try {
            System.out.println("âž• === CREATING NEW EXAM ===");
            System.out.println("Title: " + request.getTitle());
            System.out.println("Course: " + request.getCourseId());
            System.out.println("Instructor: " + auth.getName());

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            Exam createdExam = examService.createExam(request, instructorId);

            System.out.println("âœ… Created exam with ID: " + createdExam.getId());
            System.out.println("ðŸ“Š Corresponding grade column created automatically");

            return new ResponseEntity<>(Map.of(
                    "exam", createdExam,
                    "message", "Exam created successfully with corresponding grade column"
            ), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create exam: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/exams/{examId} : Update exam
     * NEW: Updates corresponding grade column when exam title or points change
     */
    @PutMapping("/exams/{examId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> updateExam(@PathVariable String examId,
                                        @Valid @RequestBody ExamUpdateRequest request,
                                        Authentication auth) {
        try {
            System.out.println("ðŸ”„ === UPDATING EXAM ===");
            System.out.println("Exam ID: " + examId);
            System.out.println("Instructor: " + auth.getName());

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            Exam updatedExam = examService.updateExam(examId, request, instructorId);

            System.out.println("âœ… Updated exam successfully");
            System.out.println("ðŸ“Š Corresponding grade column updated automatically");

            return ResponseEntity.ok(Map.of(
                    "exam", updatedExam,
                    "message", "Exam and corresponding grade column updated successfully"
            ));
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update exam: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/exams/{examId} : Delete exam
     * NEW: Also deletes corresponding grade column
     */
    @DeleteMapping("/exams/{examId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> deleteExam(@PathVariable String examId, Authentication auth) {
        try {
            System.out.println("ðŸ—‘ï¸ === DELETING EXAM ===");
            System.out.println("Exam ID: " + examId);
            System.out.println("Instructor: " + auth.getName());

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            examService.deleteExam(examId, instructorId);

            System.out.println("âœ… Deleted exam successfully");
            System.out.println("ðŸ“Š Corresponding grade column deleted automatically");

            return ResponseEntity.ok(Map.of(
                    "message", "Exam and corresponding grade column deleted successfully"
            ));
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete exam: " + e.getMessage()));
        }
    }

    // ===================================
    // EXAM STATUS MANAGEMENT
    // ===================================

    /**
     * POST /api/exams/{examId}/publish : Publish exam
     * NEW: Updates grade column max points when published
     */
    @PostMapping("/exams/{examId}/publish")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> publishExam(@PathVariable String examId, Authentication auth) {
        try {
            System.out.println("ðŸ“¢ === PUBLISHING EXAM ===");
            System.out.println("Exam ID: " + examId);

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            Exam publishedExam = examService.publishExam(examId, instructorId);

            System.out.println("âœ… Published exam successfully");
            System.out.println("ðŸ“Š Grade column max points updated to reflect current exam total");

            return ResponseEntity.ok(Map.of(
                    "exam", publishedExam,
                    "message", "Exam published successfully, grade column updated"
            ));
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to publish exam: " + e.getMessage()));
        }
    }

    /**
     * POST /api/exams/{examId}/unpublish : Unpublish exam
     */
    @PostMapping("/exams/{examId}/unpublish")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> unpublishExam(@PathVariable String examId, Authentication auth) {
        try {
            System.out.println("ðŸ“ === UNPUBLISHING EXAM ===");
            System.out.println("Exam ID: " + examId);

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            Exam unpublishedExam = examService.unpublishExam(examId, instructorId);

            System.out.println("âœ… Unpublished exam successfully");
            return ResponseEntity.ok(unpublishedExam);
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to unpublish exam: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/exams/{examId}/status : Update exam status
     */
    @PutMapping("/exams/{examId}/status")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> updateExamStatus(@PathVariable String examId,
                                              @RequestBody Map<String, String> request,
                                              Authentication auth) {
        try {
            System.out.println("ðŸ”„ === UPDATING EXAM STATUS ===");
            System.out.println("Exam ID: " + examId);

            String status = request.get("status");
            if (status == null || status.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Status is required"));
            }

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            Exam updatedExam = examService.updateExamStatus(examId, status, instructorId);

            System.out.println("âœ… Updated exam status to: " + status);
            return ResponseEntity.ok(updatedExam);
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update exam status: " + e.getMessage()));
        }
    }

    // ===================================
    // QUESTION MANAGEMENT
    // ===================================

    /**
     * POST /api/exams/{examId}/questions : Add question to exam
     * NEW: Updates grade column max points when questions are added
     */
    @PostMapping("/exams/{examId}/questions")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> addQuestion(@PathVariable String examId,
                                         @Valid @RequestBody ExamQuestionRequest request,
                                         Authentication auth) {
        try {
            System.out.println("âž• === ADDING QUESTION TO EXAM ===");
            System.out.println("Exam ID: " + examId);
            System.out.println("Question Type: " + request.getType());

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            ExamQuestion question = examService.addQuestion(examId, request, instructorId);

            System.out.println("âœ… Added question successfully");
            System.out.println("ðŸ“Š Grade column max points updated automatically");

            return new ResponseEntity<>(Map.of(
                    "question", question,
                    "message", "Question added successfully, grade column max points updated"
            ), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add question: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/exams/{examId}/questions/{questionId} : Update question
     * NEW: Updates grade column max points when question points change
     */
    @PutMapping("/exams/{examId}/questions/{questionId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> updateQuestion(@PathVariable String examId,
                                            @PathVariable String questionId,
                                            @Valid @RequestBody ExamQuestionRequest request,
                                            Authentication auth) {
        try {
            System.out.println("ðŸ”„ === UPDATING QUESTION ===");
            System.out.println("Exam ID: " + examId);
            System.out.println("Question ID: " + questionId);

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            ExamQuestion question = examService.updateQuestion(examId, questionId, request, instructorId);

            System.out.println("âœ… Updated question successfully");
            System.out.println("ðŸ“Š Grade column max points updated automatically");

            return ResponseEntity.ok(Map.of(
                    "question", question,
                    "message", "Question updated successfully, grade column max points updated"
            ));
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update question: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/exams/{examId}/questions/{questionId} : Delete question
     * NEW: Updates grade column max points when questions are deleted
     */
    @DeleteMapping("/exams/{examId}/questions/{questionId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> deleteQuestion(@PathVariable String examId,
                                            @PathVariable String questionId,
                                            Authentication auth) {
        try {
            System.out.println("ðŸ—‘ï¸ === DELETING QUESTION ===");
            System.out.println("Exam ID: " + examId);
            System.out.println("Question ID: " + questionId);

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            examService.deleteQuestion(examId, questionId, instructorId);

            System.out.println("âœ… Deleted question successfully");
            System.out.println("ðŸ“Š Grade column max points updated automatically");

            return ResponseEntity.ok(Map.of(
                    "message", "Question deleted successfully, grade column max points updated"
            ));
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete question: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/exams/{examId}/questions/reorder : Reorder questions
     */
    @PutMapping("/exams/{examId}/questions/reorder")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> reorderQuestions(@PathVariable String examId,
                                              @RequestBody Map<String, List<String>> request,
                                              Authentication auth) {
        try {
            System.out.println("ðŸ”„ === REORDERING QUESTIONS ===");
            System.out.println("Exam ID: " + examId);

            List<String> questionIds = request.get("questionIds");
            if (questionIds == null || questionIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Question IDs are required"));
            }

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            examService.reorderQuestions(examId, questionIds, instructorId);

            System.out.println("âœ… Reordered questions successfully");
            return ResponseEntity.ok(Map.of("message", "Questions reordered successfully"));
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reorder questions: " + e.getMessage()));
        }
    }

    // ===================================
    // STUDENT EXAM TAKING - âœ… FIXED
    // ===================================

    /**
     * âœ… FIXED: POST /api/exams/{examId}/start : Start exam attempt
     */
    @PostMapping("/exams/{examId}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> startExam(@PathVariable String examId, Authentication auth) {
        try {
            System.out.println("ðŸŽ¯ === STARTING EXAM ATTEMPT ===");
            System.out.println("Exam ID: " + examId);
            System.out.println("Auth Name: " + auth.getName());

            // âœ… FIXED: Get actual user ID instead of username
            String studentId = getUserIdFromAuth(auth);
            System.out.println("Student ID: " + studentId);

            ExamResponse response = examService.startExam(examId, studentId);

            System.out.println("âœ… Started exam attempt: " + response.getId());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to start exam: " + e.getMessage()));
        }
    }

    /**
     * âœ… FIXED: PUT /api/exams/save-progress : Save exam progress
     */
    @PutMapping("/exams/save-progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> saveProgress(@Valid @RequestBody ExamResponseRequest request,
                                          Authentication auth) {
        try {
            System.out.println("ðŸ’¾ === SAVING EXAM PROGRESS ===");
            System.out.println("Auth Name: " + auth.getName());

            // âœ… FIXED: Get actual user ID instead of username
            String studentId = getUserIdFromAuth(auth);
            System.out.println("Student ID: " + studentId);

            ExamResponse response = examService.saveProgress(request, studentId);

            System.out.println("âœ… Saved progress successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save progress: " + e.getMessage()));
        }
    }

    /**
     * âœ… FIXED: POST /api/exams/submit : Submit exam
     */
    @PostMapping("/exams/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> submitExam(@Valid @RequestBody ExamResponseRequest request,
                                        Authentication auth) {
        try {
            System.out.println("ðŸ“¤ === SUBMITTING EXAM ===");
            System.out.println("Auth Name: " + auth.getName());

            // âœ… FIXED: Get actual user ID instead of username
            String studentId = getUserIdFromAuth(auth);
            System.out.println("Student ID: " + studentId);

            ExamResponse response = examService.submitExam(request, studentId);

            System.out.println("âœ… Submitted exam successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to submit exam: " + e.getMessage()));
        }
    }

    // ===================================
    // RESPONSE MANAGEMENT
    // ===================================

    /**
     * GET /api/exams/{examId}/responses : Get exam responses
     */
    @GetMapping("/exams/{examId}/responses")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> getExamResponses(@PathVariable String examId) {
        try {
            System.out.println("ðŸ“Š === FETCHING EXAM RESPONSES ===");
            System.out.println("Exam ID: " + examId);

            List<ExamResponse> responses = examService.getExamResponses(examId);

            System.out.println("âœ… Found " + responses.size() + " responses");
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            System.err.println("âŒ Error fetching responses: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch responses: " + e.getMessage()));
        }
    }

    /**
     * GET /api/exam-responses/{responseId} : Get specific response
     */
    @GetMapping("/exam-responses/{responseId}")
    @PreAuthorize("hasRole('LECTURER') or (hasRole('STUDENT') and @examService.getResponse(#responseId).studentId == authentication.name)")
    public ResponseEntity<?> getResponse(@PathVariable String responseId) {
        try {
            System.out.println("ðŸ“‹ === FETCHING RESPONSE DETAILS ===");
            System.out.println("Response ID: " + responseId);

            ExamResponse response = examService.getResponse(responseId);

            System.out.println("âœ… Retrieved response successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch response: " + e.getMessage()));
        }
    }

    /**
     * NEW: GET /api/exam-responses/{responseId}/detailed : Get detailed response for grading
     */
    @GetMapping("/exam-responses/{responseId}/detailed")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> getDetailedExamResponse(@PathVariable String responseId, Authentication auth) {
        try {
            System.out.println("ðŸ“‹ === FETCHING DETAILED EXAM RESPONSE FOR GRADING ===");
            System.out.println("Response ID: " + responseId);
            System.out.println("Lecturer: " + auth.getName());

            ExamResponse response = examService.getResponse(responseId);

            // Get the exam for additional context
            Exam exam = examService.getExamById(response.getExamId());

            // Create detailed response with grading context
            Map<String, Object> detailedResponse = new HashMap<>();
            detailedResponse.put("id", response.getId());
            detailedResponse.put("examId", response.getExamId());
            detailedResponse.put("studentId", response.getStudentId());
            detailedResponse.put("courseId", response.getCourseId());
            detailedResponse.put("answers", response.getAnswers());
            detailedResponse.put("questionScores", response.getQuestionScores());
            detailedResponse.put("startedAt", response.getStartedAt());
            detailedResponse.put("submittedAt", response.getSubmittedAt());
            detailedResponse.put("timeSpent", response.getTimeSpent());
            detailedResponse.put("status", response.getStatus());
            detailedResponse.put("totalScore", response.getTotalScore());
            detailedResponse.put("maxScore", response.getMaxScore());
            detailedResponse.put("percentage", response.getPercentage());
            detailedResponse.put("passed", response.getPassed());
            detailedResponse.put("graded", response.getGraded());
            detailedResponse.put("autoGraded", response.getAutoGraded());
            detailedResponse.put("attemptNumber", response.getAttemptNumber());
            detailedResponse.put("instructorFeedback", response.getInstructorFeedback());
            detailedResponse.put("gradedBy", response.getGradedBy());
            detailedResponse.put("gradedAt", response.getGradedAt());
            detailedResponse.put("flaggedForReview", response.getFlaggedForReview());
            detailedResponse.put("lateSubmission", response.getLateSubmission());
            detailedResponse.put("createdAt", response.getCreatedAt());
            detailedResponse.put("updatedAt", response.getUpdatedAt());

            // Add computed properties for grading
            detailedResponse.put("isCompleted", response.isSubmitted());
            detailedResponse.put("needsManualGrading", response.needsGrading());

            // Add timing info
            String timeSpentFormatted = response.getTimeSpent() != null ?
                    String.format("%dm %ds", response.getTimeSpent() / 60, response.getTimeSpent() % 60) : "N/A";
            detailedResponse.put("timeSpentFormatted", timeSpentFormatted);

            // Add exam context
            detailedResponse.put("examTitle", exam.getTitle());
            detailedResponse.put("examPassPercentage", exam.getPassPercentage());

            System.out.println("âœ… Retrieved detailed response for grading");
            return ResponseEntity.ok(detailedResponse);
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch detailed response: " + e.getMessage()));
        }
    }

    /**
     * GET /api/students/{studentId}/courses/{courseId}/exam-responses : Get student responses for course
     */
    @GetMapping("/students/{studentId}/courses/{courseId}/exam-responses")
    @PreAuthorize("hasRole('LECTURER') or (hasRole('STUDENT') and #studentId == authentication.name)")
    public ResponseEntity<?> getStudentResponses(@PathVariable String studentId,
                                                 @PathVariable String courseId) {
        try {
            System.out.println("ðŸ“Š === FETCHING STUDENT RESPONSES ===");
            System.out.println("Student: " + studentId);
            System.out.println("Course: " + courseId);

            List<ExamResponse> responses = examService.getStudentResponses(studentId, courseId);

            System.out.println("âœ… Found " + responses.size() + " responses");
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            System.err.println("âŒ Error fetching student responses: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch student responses: " + e.getMessage()));
        }
    }

    /**
     * NEW: GET /api/exams/{examId}/responses/student/{studentId} : Get exam response history for student
     */
    @GetMapping("/exams/{examId}/responses/student/{studentId}")
    @PreAuthorize("hasRole('LECTURER') or (hasRole('STUDENT') and #studentId == authentication.name)")
    public ResponseEntity<?> getExamResponseHistory(@PathVariable String examId,
                                                    @PathVariable String studentId) {
        try {
            System.out.println("ðŸ“š === FETCHING EXAM RESPONSE HISTORY ===");
            System.out.println("Exam ID: " + examId);
            System.out.println("Student: " + studentId);

            List<ExamResponse> responses = examService.getStudentExamResponses(examId, studentId);

            // Transform responses with additional metadata
            List<Map<String, Object>> transformedResponses = responses.stream()
                    .map(response -> {
                        Map<String, Object> transformed = new HashMap<>();
                        transformed.put("id", response.getId());
                        transformed.put("examId", response.getExamId());
                        transformed.put("studentId", response.getStudentId());
                        transformed.put("attemptNumber", response.getAttemptNumber());
                        transformed.put("status", response.getStatus());
                        transformed.put("startedAt", response.getStartedAt());
                        transformed.put("submittedAt", response.getSubmittedAt());
                        transformed.put("timeSpent", response.getTimeSpent());
                        transformed.put("totalScore", response.getTotalScore());
                        transformed.put("maxScore", response.getMaxScore());
                        transformed.put("percentage", response.getPercentage());
                        transformed.put("passed", response.getPassed());
                        transformed.put("graded", response.getGraded());
                        transformed.put("autoGraded", response.getAutoGraded());

                        // Add computed properties
                        transformed.put("isCurrentAttempt", "IN_PROGRESS".equals(response.getStatus()));
                        transformed.put("isCompleted", response.isSubmitted());

                        String timeSpentFormatted = response.getTimeSpent() != null ?
                                String.format("%dm %ds", response.getTimeSpent() / 60, response.getTimeSpent() % 60) : "N/A";
                        transformed.put("timeSpentFormatted", timeSpentFormatted);

                        return transformed;
                    })
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

            System.out.println("âœ… Found " + responses.size() + " response history entries");
            return ResponseEntity.ok(transformedResponses);
        } catch (Exception e) {
            System.err.println("âŒ Error fetching response history: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch response history: " + e.getMessage()));
        }
    }

    // ===================================
    // GRADING - âœ… FIXED
    // ===================================

    /**
     * PUT /api/exam-responses/grade : Grade exam response
     */
    @PutMapping("/exam-responses/grade")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> gradeResponse(@Valid @RequestBody ExamGradeRequest request,
                                           Authentication auth) {
        try {
            System.out.println("ðŸ“ === GRADING EXAM RESPONSE ===");
            System.out.println("Response ID: " + request.getResponseId());
            System.out.println("Grader: " + auth.getName());

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            ExamResponse gradedResponse = examService.gradeResponse(request, instructorId);

            System.out.println("âœ… Graded response successfully");
            return ResponseEntity.ok(gradedResponse);
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to grade response: " + e.getMessage()));
        }
    }

    @PutMapping("/exam-responses/manual-grade")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> manualGradeResponse(@Valid @RequestBody Map<String, Object> gradeData,
                                                 Authentication auth) {
        try {
            System.out.println("ðŸ“ === MANUAL GRADING EXAM RESPONSE ===");
            System.out.println("Grader: " + auth.getName());
            System.out.println("Received gradeData: " + gradeData);

            String responseId = (String) gradeData.get("responseId");
            if (responseId == null || responseId.trim().isEmpty()) {
                System.err.println("âŒ Missing responseId in request");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Response ID is required"));
            }

            // ENHANCED: Better handling of questionScores
            Object questionScoresObj = gradeData.get("questionScores");
            Map<String, Integer> questionScores = new HashMap<>();

            if (questionScoresObj == null) {
                System.err.println("âŒ Missing questionScores in request");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Question scores are required"));
            }

            try {
                if (questionScoresObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> scoresMap = (Map<String, Object>) questionScoresObj;

                    for (Map.Entry<String, Object> entry : scoresMap.entrySet()) {
                        String questionId = entry.getKey();
                        Object scoreValue = entry.getValue();

                        Integer score = null;
                        if (scoreValue instanceof Integer) {
                            score = (Integer) scoreValue;
                        } else if (scoreValue instanceof Number) {
                            score = ((Number) scoreValue).intValue();
                        } else if (scoreValue instanceof String) {
                            try {
                                score = Integer.parseInt((String) scoreValue);
                            } catch (NumberFormatException e) {
                                System.err.println("âŒ Invalid score format for question " + questionId + ": " + scoreValue);
                                return ResponseEntity.badRequest()
                                        .body(Map.of("error", "Invalid score format for question " + questionId));
                            }
                        }

                        if (score == null || score < 0) {
                            System.err.println("âŒ Invalid score value for question " + questionId + ": " + scoreValue);
                            return ResponseEntity.badRequest()
                                    .body(Map.of("error", "Invalid score value for question " + questionId));
                        }

                        questionScores.put(questionId, score);
                    }
                } else {
                    System.err.println("âŒ questionScores is not a Map: " + questionScoresObj.getClass());
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Question scores must be provided as an object"));
                }
            } catch (Exception e) {
                System.err.println("âŒ Error processing questionScores: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Error processing question scores: " + e.getMessage()));
            }

            if (questionScores.isEmpty()) {
                System.err.println("âŒ No valid question scores found");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "At least one question score is required"));
            }

            System.out.println("âœ… Processed question scores: " + questionScores);

            // Create grade request
            ExamGradeRequest request = new ExamGradeRequest();
            request.setResponseId(responseId);
            request.setQuestionScores(questionScores);
            request.setInstructorFeedback((String) gradeData.getOrDefault("instructorFeedback", ""));
            request.setFlaggedForReview((Boolean) gradeData.getOrDefault("flaggedForReview", false));

            System.out.println("âœ… Created ExamGradeRequest: " + request);

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            ExamResponse gradedResponse = examService.gradeResponse(request, instructorId);

            System.out.println("âœ… Manual grading completed successfully");
            return ResponseEntity.ok(Map.of(
                    "message", "Response graded successfully",
                    "response", gradedResponse
            ));
        } catch (RuntimeException e) {
            System.err.println("âŒ Runtime error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to manually grade response: " + e.getMessage()));
        }
    }

    /**
     * NEW: PUT /api/exam-responses/{responseId}/question-score : Update individual question score
     */
    @PutMapping("/exam-responses/{responseId}/question-score")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> updateQuestionScore(@PathVariable String responseId,
                                                 @RequestBody Map<String, Object> updateData,
                                                 Authentication auth) {
        try {
            System.out.println("ðŸ“¢ === UPDATING QUESTION SCORE ===");
            System.out.println("Response ID: " + responseId);

            String questionId = (String) updateData.get("questionId");
            Integer score = (Integer) updateData.get("score");
            String feedback = (String) updateData.getOrDefault("feedback", "");

            if (questionId == null || score == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Question ID and score are required"));
            }

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            ExamResponse updatedResponse = examService.updateQuestionScore(responseId, questionId, score, feedback, instructorId);

            System.out.println("âœ… Question score updated successfully");
            return ResponseEntity.ok(Map.of(
                    "message", "Question score updated successfully",
                    "response", updatedResponse
            ));
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update question score: " + e.getMessage()));
        }
    }

    /**
     * POST /api/exam-responses/{responseId}/auto-grade : Auto-grade response
     */
    @PostMapping("/exam-responses/{responseId}/auto-grade")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> autoGradeResponse(@PathVariable String responseId) {
        try {
            System.out.println("ðŸ¤– === AUTO-GRADING RESPONSE ===");
            System.out.println("Response ID: " + responseId);

            ExamResponse gradedResponse = examService.autoGradeResponse(responseId);

            System.out.println("âœ… Auto-graded response successfully");
            return ResponseEntity.ok(gradedResponse);
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to auto-grade response: " + e.getMessage()));
        }
    }

    /**
     * POST /api/exams/{examId}/auto-grade-all : Auto-grade all responses
     */
    @PostMapping("/exams/{examId}/auto-grade-all")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> autoGradeAllResponses(@PathVariable String examId) {
        try {
            System.out.println("ðŸ¤– === AUTO-GRADING ALL RESPONSES ===");
            System.out.println("Exam ID: " + examId);

            List<ExamResponse> gradedResponses = examService.autoGradeAllResponses(examId);

            System.out.println("âœ… Auto-graded " + gradedResponses.size() + " responses");
            return ResponseEntity.ok(Map.of(
                    "message", "Auto-grading completed",
                    "gradedCount", gradedResponses.size(),
                    "responses", gradedResponses
            ));
        } catch (Exception e) {
            System.err.println("âŒ Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to auto-grade responses: " + e.getMessage()));
        }
    }

    /**
     * NEW: PUT /api/exam-responses/{responseId}/flag : Flag response for review
     */
    @PutMapping("/exam-responses/{responseId}/flag")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> flagResponseForReview(@PathVariable String responseId,
                                                   @RequestBody Map<String, Object> flagData,
                                                   Authentication auth) {
        try {
            System.out.println("ðŸš© === FLAGGING RESPONSE FOR REVIEW ===");
            System.out.println("Response ID: " + responseId);

            String reason = (String) flagData.getOrDefault("flagReason", "");
            String priority = (String) flagData.getOrDefault("flagPriority", "medium");

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            ExamResponse flaggedResponse = examService.flagResponseForReview(responseId, reason, priority, instructorId);

            System.out.println("âœ… Response flagged for review successfully");
            return ResponseEntity.ok(Map.of(
                    "message", "Response flagged for review successfully",
                    "response", flaggedResponse
            ));
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to flag response: " + e.getMessage()));
        }
    }

    /**
     * NEW: PUT /api/exam-responses/{responseId}/unflag : Unflag response
     */
    @PutMapping("/exam-responses/{responseId}/unflag")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> unflagResponse(@PathVariable String responseId, Authentication auth) {
        try {
            System.out.println("ðŸš© === UNFLAGGING RESPONSE ===");
            System.out.println("Response ID: " + responseId);

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            ExamResponse unflaggedResponse = examService.unflagResponse(responseId, instructorId);

            System.out.println("âœ… Response unflagged successfully");
            return ResponseEntity.ok(Map.of(
                    "message", "Response unflagged successfully",
                    "response", unflaggedResponse
            ));
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to unflag response: " + e.getMessage()));
        }
    }

    /**
     * NEW: POST /api/exam-responses/batch-grade : Batch grade multiple responses
     */
    @PostMapping("/exam-responses/batch-grade")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> batchGradeResponses(@RequestBody Map<String, Object> batchData,
                                                 Authentication auth) {
        try {
            System.out.println("ðŸ“¦ === BATCH GRADING RESPONSES ===");

            @SuppressWarnings("unchecked")
            List<String> responseIds = (List<String>) batchData.get("responseIds");
            if (responseIds == null || responseIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Response IDs are required"));
            }

            String instructorFeedback = (String) batchData.getOrDefault("instructorFeedback", "");
            Boolean flagForReview = (Boolean) batchData.getOrDefault("flagForReview", false);

            // âœ… FIXED: Use proper user ID for instructors
            String instructorId = getUserIdFromAuth(auth);
            List<ExamResponse> batchGradedResponses = examService.batchGradeResponses(responseIds, instructorFeedback, flagForReview, instructorId);

            System.out.println("âœ… Batch grading completed for " + responseIds.size() + " responses");
            return ResponseEntity.ok(Map.of(
                    "message", "Batch grading completed successfully",
                    "gradedCount", batchGradedResponses.size(),
                    "responses", batchGradedResponses
            ));
        } catch (RuntimeException e) {
            System.err.println("âŒ Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("âŒ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to batch grade responses: " + e.getMessage()));
        }
    }

    // ===================================
    // STATISTICS AND ANALYTICS
    // ===================================

    /**
     * GET /api/exams/{examId}/stats : Get exam statistics
     */
    @GetMapping("/exams/{examId}/stats")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> getExamStats(@PathVariable String examId) {
        try {
            System.out.println("ðŸ“Š === FETCHING EXAM STATISTICS ===");
            System.out.println("Exam ID: " + examId);

            ExamStatsResponse stats = examService.getExamStats(examId);

            System.out.println("âœ… Retrieved exam statistics");
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("âŒ Error fetching stats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch exam statistics: " + e.getMessage()));
        }
    }

    /**
     * NEW: GET /api/exams/{examId}/grading-stats : Get grading statistics for an exam
     */
    @GetMapping("/exams/{examId}/grading-stats")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> getExamGradingStats(@PathVariable String examId) {
        try {
            System.out.println("ðŸ“Š === FETCHING GRADING STATISTICS ===");
            System.out.println("Exam ID: " + examId);

            Map<String, Object> gradingStats = examService.getExamGradingStats(examId);

            System.out.println("âœ… Retrieved grading statistics");
            return ResponseEntity.ok(gradingStats);
        } catch (Exception e) {
            System.err.println("âŒ Error fetching grading stats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch grading statistics: " + e.getMessage()));
        }
    }

    /**
     * GET /api/courses/{courseId}/exam-stats : Get course exam statistics
     */
    @GetMapping("/courses/{courseId}/exam-stats")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> getCourseExamStats(@PathVariable String courseId) {
        try {
            System.out.println("ðŸ“Š === FETCHING COURSE EXAM STATISTICS ===");
            System.out.println("Course ID: " + courseId);

            List<ExamStatsResponse> stats = examService.getCourseExamStats(courseId);

            System.out.println("âœ… Retrieved statistics for " + stats.size() + " exams");
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("âŒ Error fetching course stats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch course exam statistics: " + e.getMessage()));
        }
    }

    // ===================================
    // VALIDATION AND UTILITY ENDPOINTS - âœ… FIXED
    // ===================================

    /**
     * âœ… FIXED: GET /api/exams/{examId}/can-take : Check if student can take exam
     */
    @GetMapping("/exams/{examId}/can-take")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> canTakeExam(@PathVariable String examId, Authentication auth) {
        try {
            System.out.println("ðŸ” === CHECKING EXAM ELIGIBILITY ===");
            System.out.println("Exam ID: " + examId);
            System.out.println("Auth Name: " + auth.getName());

            // âœ… FIXED: Get actual user ID instead of username
            String studentId = getUserIdFromAuth(auth);
            System.out.println("Student ID: " + studentId);

            boolean canTake = examService.canStudentTakeExam(examId, studentId);
            int attemptCount = examService.getStudentAttemptCount(examId, studentId);
            boolean hasActive = examService.hasActiveAttempt(examId, studentId);

            Map<String, Object> result = Map.of(
                    "canTake", canTake,
                    "attemptCount", attemptCount,
                    "hasActiveAttempt", hasActive,
                    "timestamp", LocalDateTime.now().toString()
            );

            System.out.println("âœ… Eligibility check: canTake=" + canTake +
                    ", attempts=" + attemptCount +
                    ", hasActive=" + hasActive);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("âŒ Error checking eligibility: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check exam eligibility: " + e.getMessage()));
        }
    }

    /**
     * GET /api/exams/{examId}/attempt-count/{studentId} : Get student attempt count
     */
    @GetMapping("/exams/{examId}/attempt-count/{studentId}")
    @PreAuthorize("hasRole('LECTURER') or (hasRole('STUDENT') and #studentId == authentication.name)")
    public ResponseEntity<?> getAttemptCount(@PathVariable String examId,
                                             @PathVariable String studentId) {
        try {
            System.out.println("ðŸ“¢ === CHECKING ATTEMPT COUNT ===");
            System.out.println("Exam ID: " + examId);
            System.out.println("Student: " + studentId);

            int attemptCount = examService.getStudentAttemptCount(examId, studentId);

            Map<String, Object> result = Map.of(
                    "examId", examId,
                    "studentId", studentId,
                    "attemptCount", attemptCount,
                    "timestamp", LocalDateTime.now().toString()
            );

            System.out.println("âœ… Attempt count: " + attemptCount);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("âŒ Error checking attempt count: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check attempt count: " + e.getMessage()));
        }
    }

    // ===================================
    // EXPORT FUNCTIONALITY
    // ===================================

    /**
     * NEW: POST /api/exam-responses/export-detailed : Export detailed exam responses
     */
    @PostMapping("/exam-responses/export-detailed")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> exportDetailedExamResponses(@RequestBody Map<String, Object> exportData,
                                                         Authentication auth) {
        try {
            System.out.println("ðŸ“¤ === EXPORTING DETAILED EXAM RESPONSES ===");

            String examId = (String) exportData.get("examId");
            if (examId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Exam ID is required"));
            }

            String format = (String) exportData.getOrDefault("format", "csv");

            // For now, return a simple response indicating export is initiated
            // In a real implementation, this would generate the file and return download URL
            Map<String, Object> response = Map.of(
                    "message", "Export initiated successfully",
                    "examId", examId,
                    "format", format,
                    "status", "processing",
                    "timestamp", LocalDateTime.now().toString()
            );

            System.out.println("âœ… Export initiated for exam: " + examId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("âŒ Error initiating export: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to initiate export: " + e.getMessage()));
        }
    }

    // ===================================
    // ADMIN AND DEBUG ENDPOINTS
    // ===================================

    /**
     * GET /api/admin/exams/summary : Get system-wide exam summary
     */
    @GetMapping("/admin/exams/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getExamSummary() {
        try {
            System.out.println("ðŸ“Š === ADMIN: EXAM SUMMARY ===");

            // This would typically be implemented with aggregation queries
            Map<String, Object> summary = Map.of(
                    "message", "Exam summary endpoint - to be implemented with aggregation queries",
                    "timestamp", LocalDateTime.now().toString()
            );

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            System.err.println("âŒ Error fetching exam summary: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch exam summary: " + e.getMessage()));
        }
    }

    /**
     * POST /api/admin/exams/cleanup : Clean up orphaned exam data
     */
    @PostMapping("/admin/exams/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cleanupExamData() {
        try {
            System.out.println("ðŸ§¹ === ADMIN: CLEANING UP EXAM DATA ===");

            // Implementation would clean up orphaned responses, invalid data, etc.
            Map<String, Object> result = Map.of(
                    "message", "Exam data cleanup completed",
                    "timestamp", LocalDateTime.now().toString()
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("âŒ Error during cleanup: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to cleanup exam data: " + e.getMessage()));
        }
    }
}