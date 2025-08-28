package com.example.edusphere.service;

import com.example.edusphere.entity.ExamResponse;
import com.example.edusphere.dto.request.ExamResponseRequest;

import java.util.List;
import java.util.Map;

public interface StudentExamService {

    // ===================================
    // EXAM LISTING AND VIEWING
    // ===================================

    /**
     * Get available exams for a student in a specific course
     * @param studentId The student's ID
     * @param courseId The course ID
     * @return List of available exams with student-specific information
     */
    List<Map<String, Object>> getAvailableExamsForStudent(String studentId, String courseId);

    /**
     * Get detailed exam information for a student
     * @param examId The exam ID
     * @param studentId The student's ID
     * @return Exam details with student-specific information
     */
    Map<String, Object> getStudentExamDetails(String examId, String studentId);

    // ===================================
    // EXAM ATTEMPT MANAGEMENT
    // ===================================

    /**
     * Start a new exam attempt for a student
     * @param examId The exam ID
     * @param studentId The student's ID
     * @return Exam attempt information
     */
    Map<String, Object> startExamAttempt(String examId, String studentId);

    /**
     * Save exam progress for an active attempt
     * @param request The exam response request with answers and progress
     * @param studentId The student's ID
     * @return Updated exam response
     */
    ExamResponse saveExamProgress(ExamResponseRequest request, String studentId);

    /**
     * Submit an exam attempt
     * @param request The exam response request with final answers
     * @param studentId The student's ID
     * @return Submission result with auto-grading results if available
     */
    Map<String, Object> submitExam(ExamResponseRequest request, String studentId);

    /**
     * Resume an active exam attempt
     * @param examId The exam ID
     * @param studentId The student's ID
     * @return Resume information with current state
     */
    Map<String, Object> resumeExamAttempt(String examId, String studentId);

    // ===================================
    // EXAM ELIGIBILITY AND STATUS
    // ===================================

    /**
     * Check if a student is eligible to take an exam
     * @param examId The exam ID
     * @param studentId The student's ID
     * @return Eligibility information
     */
    Map<String, Object> checkExamEligibility(String examId, String studentId);

    /**
     * Get student's attempt history for an exam
     * @param examId The exam ID
     * @param studentId The student's ID
     * @return List of attempt summaries
     */
    List<Map<String, Object>> getStudentAttemptHistory(String examId, String studentId);

    /**
     * Check for active exam attempt
     * @param examId The exam ID
     * @param studentId The student's ID
     * @return Active attempt information
     */
    Map<String, Object> checkActiveAttempt(String examId, String studentId);

    // ===================================
    // EXAM RESULTS AND FEEDBACK
    // ===================================

    /**
     * Get exam results for a student
     * @param responseId The exam response ID
     * @param studentId The student's ID
     * @return Exam results if available
     */
    Map<String, Object> getStudentExamResults(String responseId, String studentId);

    /**
     * Get detailed exam results with question breakdown
     * @param responseId The exam response ID
     * @param studentId The student's ID
     * @return Detailed exam results
     */
    Map<String, Object> getDetailedExamResults(String responseId, String studentId);

    // ===================================
    // STUDENT STATISTICS
    // ===================================

    /**
     * Get exam statistics for a student in a course
     * @param studentId The student's ID
     * @param courseId The course ID
     * @return Exam statistics
     */
    Map<String, Object> getStudentExamStats(String studentId, String courseId);

    /**
     * Get exam summary for student dashboard
     * @param studentId The student's ID
     * @return Dashboard exam summary
     */
    Map<String, Object> getExamDashboardSummary(String studentId);
}