package com.example.edusphere.service;

import com.example.edusphere.entity.TaskSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskSubmissionService {

    // ====================================
    // BASIC SUBMISSION CRUD OPERATIONS
    // ====================================

    /**
     * Create a new submission
     */
    TaskSubmission createSubmission(TaskSubmission submission);

    /**
     * Update an existing submission
     */
    TaskSubmission updateSubmission(String submissionId, TaskSubmission submission);

    /**
     * Delete a submission
     */
    void deleteSubmission(String submissionId);

    /**
     * Find submission by ID
     */
    Optional<TaskSubmission> findSubmissionById(String submissionId);

    // ====================================
    // SUBMISSION RETRIEVAL METHODS
    // ====================================

    /**
     * Find submissions by task ID
     */
    List<TaskSubmission> findSubmissionsByTaskId(String taskId);

    /**
     * Find submissions by task ID with pagination
     */
    Page<TaskSubmission> findSubmissionsByTaskId(String taskId, Pageable pageable);

    /**
     * Find submissions by student ID
     */
    List<TaskSubmission> findSubmissionsByStudentId(String studentId);

    /**
     * Find submissions by course ID
     */
    List<TaskSubmission> findSubmissionsByCourseId(String courseId);

    /**
     * Find submissions by student and course
     */
    List<TaskSubmission> findSubmissionsByStudentAndCourse(String studentId, String courseId);

    /**
     * Find specific submission by task and student
     */
    Optional<TaskSubmission> findSubmissionByTaskAndStudent(String taskId, String studentId);

    /**
     * Find submissions needing grading
     */
    List<TaskSubmission> findSubmissionsNeedingGrading(String courseId);

    /**
     * Find ungraduated submissions by task
     */
    List<TaskSubmission> findUngraduatedSubmissionsByTask(String taskId);

    /**
     * Find late submissions by task
     */
    List<TaskSubmission> findLateSubmissionsByTask(String taskId);

    /**
     * Find submissions by date range
     */
    List<TaskSubmission> findSubmissionsByDateRange(String courseId, LocalDateTime start, LocalDateTime end);

    /**
     * Find recent submissions
     */
    List<TaskSubmission> findRecentSubmissions(String courseId, int days);

    // ====================================
    // GRADING OPERATIONS
    // ====================================

    /**
     * Update submission grade
     */
    TaskSubmission updateSubmissionGrade(String submissionId, Integer grade, String feedback);

    /**
     * Update submission grade with sync to grade column
     */
    TaskSubmission updateSubmissionGradeWithSync(String submissionId, Integer grade, String feedback);

    /**
     * Batch grade submissions
     */
    List<TaskSubmission> batchGradeSubmissions(List<String> submissionIds, Integer grade, String feedback);

    /**
     * Batch grade submissions with sync to grade columns
     */
    List<TaskSubmission> batchGradeSubmissionsWithSync(List<String> submissionIds, Integer grade, String feedback);

    // ====================================
    // STUDENT PERMISSION METHODS
    // ====================================

    /**
     * Check if student can submit to task
     */
    boolean canStudentSubmit(String taskId, String studentId);

    /**
     * Check if student has already submitted
     */
    boolean hasStudentSubmitted(String taskId, String studentId);

    /**
     * Get submission attempt count for student
     */
    int getSubmissionAttemptCount(String taskId, String studentId);

    /**
     * Check if student can update their submission
     */
    boolean canStudentUpdateSubmission(String submissionId, String studentId);

    /**
     * Check if student can delete their submission
     */
    boolean canStudentDeleteSubmission(String submissionId, String studentId);

    // ====================================
    // FILE MANAGEMENT
    // ====================================

    /**
     * Add file to submission
     */
    TaskSubmission addFileToSubmission(String submissionId, String fileUrl, String fileName, Long fileSize);

    /**
     * Remove file from submission
     */
    TaskSubmission removeFileFromSubmission(String submissionId, int fileIndex);

    // ====================================
    // STATISTICS AND ANALYTICS
    // ====================================

    /**
     * Count submissions by task
     */
    long countSubmissionsByTask(String taskId);

    /**
     * Count submissions by student
     */
    long countSubmissionsByStudent(String studentId);

    /**
     * Count graded submissions by task
     */
    long countGradedSubmissionsByTask(String taskId);

    /**
     * Calculate average grade for task
     */
    double calculateAverageGradeForTask(String taskId);

    /**
     * Recalculate task statistics
     */
    void recalculateTaskStatistics(String taskId);
}