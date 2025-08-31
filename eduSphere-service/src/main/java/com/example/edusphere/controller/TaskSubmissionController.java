package com.example.edusphere.controller;

import com.example.edusphere.entity.TaskSubmission;
import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import com.example.edusphere.service.TaskSubmissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/tasksubmissions")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class TaskSubmissionController {

    private final TaskSubmissionService taskSubmissionService;
    private final UserRepository userRepository;

    public TaskSubmissionController(TaskSubmissionService taskSubmissionService, UserRepository userRepository) {
        this.taskSubmissionService = taskSubmissionService;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/tasksubmissions/course/{courseId} : Get all submissions for a course
     */
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> getSubmissionsByCourse(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {

            List<TaskSubmission> submissions;

            if (taskId != null && !taskId.trim().isEmpty()) {
                submissions = taskSubmissionService.findSubmissionsByTaskId(taskId);
            } else if (status != null && !status.trim().isEmpty()) {
                submissions = taskSubmissionService.findSubmissionsByCourseId(courseId)
                        .stream()
                        .filter(sub -> status.equals(sub.getStatus()))
                        .toList();
            } else {
                submissions = taskSubmissionService.findSubmissionsByCourseId(courseId);
            }

            return ResponseEntity.ok(submissions);

        } catch (Exception e) {
            System.err.println("Ã¢ÂÅ’ Error fetching submissions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasksubmissions/{submissionId} : Get a specific submission
     */
    @GetMapping("/{submissionId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> getSubmissionById(
            @PathVariable String submissionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {

            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            Optional<TaskSubmission> submission = taskSubmissionService.findSubmissionById(submissionId);

            if (submission.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            TaskSubmission sub = submission.get();

            // Check access permissions
            if ("1300".equals(currentUser.getRole()) && !currentUser.getId().equals(sub.getStudentId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You can only view your own submissions"));
            }
            return ResponseEntity.ok(sub);

        } catch (Exception e) {
            System.err.println("Ã¢ÂÅ’ Error fetching submission: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * POST /api/tasksubmissions : Create a new submission (with file upload support)
     */
    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> createSubmissionWithFiles(
            @RequestParam("taskId") String taskId,
            @RequestParam(value = "content", required = false, defaultValue = "") String content,
            @RequestParam(value = "notes", required = false, defaultValue = "") String notes,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {

            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check if student can submit
            if (!taskSubmissionService.canStudentSubmit(taskId, currentUser.getId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "You cannot submit to this task"));
            }

            // Create submission entity
            TaskSubmission submission = new TaskSubmission();
            submission.setTaskId(taskId);
            submission.setStudentId(currentUser.getId());
            submission.setContent(content);
            submission.setNotes(notes);
            submission.setSubmittedAt(LocalDateTime.now());

            // Handle file uploads
            if (files != null && files.length > 0) {
                List<String> fileUrls = new ArrayList<>();
                List<String> fileNames = new ArrayList<>();
                List<Long> fileSizes = new ArrayList<>();

                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        try {
                            // In a real application, you would save the file to your storage system
                            // For demonstration, we'll simulate file storage
                            String fileUrl = saveFileToStorage(file); // This method needs to be implemented
                            String fileName = file.getOriginalFilename();
                            Long fileSize = file.getSize();

                            fileUrls.add(fileUrl);
                            fileNames.add(fileName);
                            fileSizes.add(fileSize);
                        } catch (Exception e) {
                            System.err.println("Ã¢ÂÅ’ Error processing file: " + file.getOriginalFilename() + " - " + e.getMessage());
                            // Continue processing other files
                        }
                    }
                }

                submission.setFileUrls(fileUrls);
                submission.setFileNames(fileNames);
                submission.setFileSizes(fileSizes);
            }

            TaskSubmission createdSubmission = taskSubmissionService.createSubmission(submission);

            return new ResponseEntity<>(createdSubmission, HttpStatus.CREATED);

        } catch (Exception e) {
            System.err.println("Ã¢ÂÅ’ Error creating submission: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * POST /api/tasksubmissions/simple : Create a simple submission (JSON only)
     */
    @PostMapping("/simple")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> createSimpleSubmission(
            @RequestBody TaskSubmission submission,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {

            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Set the student ID to current user
            submission.setStudentId(currentUser.getId());
            submission.setSubmittedAt(LocalDateTime.now());

            // Check if student can submit
            if (!taskSubmissionService.canStudentSubmit(submission.getTaskId(), currentUser.getId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "You cannot submit to this task"));
            }

            TaskSubmission createdSubmission = taskSubmissionService.createSubmission(submission);

            return new ResponseEntity<>(createdSubmission, HttpStatus.CREATED);

        } catch (Exception e) {
            System.err.println("Ã¢ÂÅ’ Error creating simple submission: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/tasksubmissions/{submissionId} : Update submission (mainly for grading by lecturers)
     */
    @PutMapping("/{submissionId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateSubmission(
            @PathVariable String submissionId,
            @RequestBody Map<String, Object> updates,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {

            // Handle grade update specifically with sync
            if (updates.containsKey("grade")) {
                Object gradeObj = updates.get("grade");
                String feedback = (String) updates.getOrDefault("feedback", "");

                Integer grade = null;
                if (gradeObj != null) {
                    if (gradeObj instanceof Integer) {
                        grade = (Integer) gradeObj;
                    } else if (gradeObj instanceof Double) {
                        grade = ((Double) gradeObj).intValue();
                    } else if (gradeObj instanceof String) {
                        try {
                            grade = Integer.parseInt((String) gradeObj);
                        } catch (NumberFormatException e) {
                            return ResponseEntity.badRequest()
                                    .body(Map.of("error", "Invalid grade format"));
                        }
                    }
                }

                // Use sync method when grading
                TaskSubmission updatedSubmission = taskSubmissionService.updateSubmissionGradeWithSync(
                        submissionId, grade, feedback);
                return ResponseEntity.ok(updatedSubmission);
            }

            // Handle general updates
            TaskSubmission updateData = new TaskSubmission();
            if (updates.containsKey("content")) {
                updateData.setContent((String) updates.get("content"));
            }
            if (updates.containsKey("notes")) {
                updateData.setNotes((String) updates.get("notes"));
            }
            if (updates.containsKey("status")) {
                updateData.setStatus((String) updates.get("status"));
            }
            if (updates.containsKey("feedback")) {
                updateData.setFeedback((String) updates.get("feedback"));
            }

            TaskSubmission updatedSubmission = taskSubmissionService.updateSubmission(submissionId, updateData);

            return ResponseEntity.ok(updatedSubmission);

        } catch (Exception e) {
            System.err.println("Ã¢ÂÅ’ Error updating submission: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/tasksubmissions/{submissionId}/student : Update submission by student (before grading)
     */
    @PutMapping("/{submissionId}/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> updateSubmissionByStudent(
            @PathVariable String submissionId,
            @RequestBody Map<String, Object> updates,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {

            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Get the submission to check ownership and if it can be updated
            Optional<TaskSubmission> submissionOpt = taskSubmissionService.findSubmissionById(submissionId);
            if (submissionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            TaskSubmission existingSubmission = submissionOpt.get();

            // Check if student owns this submission
            if (!currentUser.getId().equals(existingSubmission.getStudentId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You can only update your own submissions"));
            }

            // Check if submission can still be updated (not yet graded)
            if (existingSubmission.getGrade() != null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cannot update submission that has already been graded"));
            }

            // Check if task still allows updates (before due date, etc.)
            if (!taskSubmissionService.canStudentUpdateSubmission(submissionId, currentUser.getId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Submission can no longer be updated"));
            }

            // Handle updates that students are allowed to make
            TaskSubmission updateData = new TaskSubmission();
            if (updates.containsKey("content")) {
                updateData.setContent((String) updates.get("content"));
            }
            if (updates.containsKey("notes")) {
                updateData.setNotes((String) updates.get("notes"));
            }

            TaskSubmission updatedSubmission = taskSubmissionService.updateSubmission(submissionId, updateData);

            return ResponseEntity.ok(updatedSubmission);

        } catch (Exception e) {
            System.err.println("Ã¢ÂÅ’ Error updating student submission: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/tasksubmissions/{submissionId}/grade : Grade a specific submission with sync
     */
    @PutMapping("/{submissionId}/grade")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> gradeSubmission(
            @PathVariable String submissionId,
            @RequestBody Map<String, Object> gradeRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {

            // Parse grade
            Object gradeObj = gradeRequest.get("grade");
            String feedback = (String) gradeRequest.getOrDefault("feedback", "");
            String comments = (String) gradeRequest.getOrDefault("comments", "");

            Integer grade = null;
            if (gradeObj != null) {
                if (gradeObj instanceof Integer) {
                    grade = (Integer) gradeObj;
                } else if (gradeObj instanceof Double) {
                    grade = ((Double) gradeObj).intValue();
                } else if (gradeObj instanceof String) {
                    try {
                        grade = Integer.parseInt((String) gradeObj);
                    } catch (NumberFormatException e) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Invalid grade format: " + gradeObj));
                    }
                }
            }

            // Validate grade range
            if (grade != null && (grade < 0 || grade > 100)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Grade must be between 0 and 100"));
            }

            // Combine feedback and comments
            String finalFeedback = feedback;
            if (comments != null && !comments.trim().isEmpty()) {
                finalFeedback = finalFeedback + (finalFeedback.isEmpty() ? "" : "\n") + comments;
            }

            // Grade the submission with sync to grade column
            TaskSubmission gradedSubmission = taskSubmissionService.updateSubmissionGradeWithSync(
                    submissionId, grade, finalFeedback);
            return ResponseEntity.ok(Map.of(
                    "message", "Submission graded successfully",
                    "submission", gradedSubmission,
                    "grade", grade,
                    "feedback", finalFeedback
            ));

        } catch (Exception e) {
            System.err.println("Ã¢ÂÅ’ Error grading submission: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/tasksubmissions/{submissionId} : Delete a submission (by student or instructor)
     */
    @DeleteMapping("/{submissionId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> deleteSubmission(
            @PathVariable String submissionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {

            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Get the submission to check ownership and permissions
            Optional<TaskSubmission> submissionOpt = taskSubmissionService.findSubmissionById(submissionId);
            if (submissionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            TaskSubmission submission = submissionOpt.get();

            // Check permissions
            if ("1300".equals(currentUser.getRole())) {
                // Students can only delete their own ungraded submissions
                if (!currentUser.getId().equals(submission.getStudentId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Access denied: You can only delete your own submissions"));
                }

                if (submission.getGrade() != null) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Cannot delete submission that has already been graded"));
                }

                // Check if student can still delete (task settings, due date, etc.)
                if (!taskSubmissionService.canStudentDeleteSubmission(submissionId, currentUser.getId())) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Submission can no longer be deleted"));
                }
            }

            taskSubmissionService.deleteSubmission(submissionId);

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            System.err.println("Ã¢ÂÅ’ Error deleting submission: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasksubmissions/task/{taskId} : Get submissions for a specific task
     */
    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> getSubmissionsByTask(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {

            List<TaskSubmission> submissions = taskSubmissionService.findSubmissionsByTaskId(taskId);

            return ResponseEntity.ok(submissions);

        } catch (Exception e) {
            System.err.println("Ã¢ÂÅ’ Error fetching submissions by task: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasksubmissions/student/{studentId} : Get submissions for a specific student
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> getSubmissionsByStudent(
            @PathVariable String studentId,
            @RequestParam(required = false) String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {

            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Students can only view their own submissions
            if ("1300".equals(currentUser.getRole()) && !currentUser.getId().equals(studentId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You can only view your own submissions"));
            }

            List<TaskSubmission> submissions;
            if (courseId != null && !courseId.trim().isEmpty()) {
                submissions = taskSubmissionService.findSubmissionsByStudentAndCourse(studentId, courseId);
            } else {
                submissions = taskSubmissionService.findSubmissionsByStudentId(studentId);
            }
            return ResponseEntity.ok(submissions);

        } catch (Exception e) {
            System.err.println("Ã¢ÂÅ’ Error fetching submissions by student: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasksubmissions/student/{studentId}/task/{taskId} : Get specific student's submission for a task
     */
    @GetMapping("/student/{studentId}/task/{taskId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> getSubmissionByStudentAndTask(
            @PathVariable String studentId,
            @PathVariable String taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {

            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Students can only view their own submissions
            if ("1300".equals(currentUser.getRole()) && !currentUser.getId().equals(studentId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You can only view your own submissions"));
            }

            Optional<TaskSubmission> submission = taskSubmissionService.findSubmissionByTaskAndStudent(taskId, studentId);

            if (submission.isPresent()) {
                return ResponseEntity.ok(submission.get());
            } else {
                return ResponseEntity.ok(Map.of(
                        "message", "No submission found for this task",
                        "hasSubmission", false,
                        "taskId", taskId,
                        "studentId", studentId
                ));
            }

        } catch (Exception e) {
            System.err.println("Ã¢ÂÅ’ Error fetching submission: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Simulate file storage - replace with actual file storage implementation
     */
    private String saveFileToStorage(MultipartFile file) throws Exception {
        // In a real application, you would:
        // 1. Save to cloud storage (AWS S3, Azure Blob, Google Cloud Storage)
        // 2. Save to local file system with proper directory structure
        // 3. Generate a unique file name to avoid conflicts
        // 4. Return the accessible URL or file path

        // For demonstration purposes, return a simulated URL
        String fileName = file.getOriginalFilename();
        String fileExtension = fileName.substring(fileName.lastIndexOf('.'));
        String uniqueFileName = System.currentTimeMillis() + "_" + fileName.replaceAll("[^a-zA-Z0-9.]", "_");
        String simulatedUrl = "/uploads/submissions/" + uniqueFileName;

        // TODO: Implement actual file storage logic here
        return simulatedUrl;
    }
    // Add this method to your TaskSubmissionController.java
// Insert this method after the existing deleteSubmission method

    /**
     * GET /api/tasksubmissions/{submissionId}/can-delete : Check if student can delete submission
     */
    @GetMapping("/{submissionId}/can-delete")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> canDeleteSubmission(
            @PathVariable String submissionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {

            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            boolean canDelete = taskSubmissionService.canStudentDeleteSubmission(submissionId, currentUser.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("canDelete", canDelete);
            result.put("submissionId", submissionId);

            if (!canDelete) {
                // Get the submission to provide more specific reason
                Optional<TaskSubmission> submissionOpt = taskSubmissionService.findSubmissionById(submissionId);
                if (submissionOpt.isPresent()) {
                    TaskSubmission submission = submissionOpt.get();
                    if (submission.getGrade() != null) {
                        result.put("reason", "Submission has already been graded and cannot be deleted");
                    } else if (!currentUser.getId().equals(submission.getStudentId())) {
                        result.put("reason", "You can only delete your own submissions");
                    } else {
                        result.put("reason", "Submission can no longer be deleted (time limit exceeded)");
                    }
                } else {
                    result.put("reason", "Submission not found");
                }
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("❌ Error checking delete permission: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}