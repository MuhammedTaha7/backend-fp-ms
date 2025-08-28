package com.example.edusphere.service;

import com.example.edusphere.entity.Exam;
import com.example.edusphere.entity.ExamQuestion;
import com.example.edusphere.entity.ExamResponse;
import com.example.edusphere.dto.request.*;
import com.example.edusphere.dto.response.*;

import java.util.List;
import java.util.Map;

public interface ExamService {

    // ===================================
    // EXAM CRUD OPERATIONS
    // ===================================

    /**
     * Get all exams for a course
     */
    List<Exam> getExamsByCourse(String courseId);

    /**
     * Get exam by ID
     */
    Exam getExamById(String examId);

    /**
     * Create a new exam
     */
    Exam createExam(ExamCreateRequest request, String instructorId);

    /**
     * Update an existing exam
     */
    Exam updateExam(String examId, ExamUpdateRequest request, String instructorId);

    /**
     * Delete an exam
     */
    void deleteExam(String examId, String instructorId);

    // ===================================
    // EXAM STATUS MANAGEMENT
    // ===================================

    /**
     * Publish an exam
     */
    Exam publishExam(String examId, String instructorId);

    /**
     * Unpublish an exam
     */
    Exam unpublishExam(String examId, String instructorId);

    /**
     * Update exam status
     */
    Exam updateExamStatus(String examId, String status, String instructorId);

    // ===================================
    // QUESTION MANAGEMENT
    // ===================================

    /**
     * Add a question to an exam
     */
    ExamQuestion addQuestion(String examId, ExamQuestionRequest request, String instructorId);

    /**
     * Update a question in an exam
     */
    ExamQuestion updateQuestion(String examId, String questionId, ExamQuestionRequest request, String instructorId);

    /**
     * Delete a question from an exam
     */
    void deleteQuestion(String examId, String questionId, String instructorId);

    /**
     * Reorder questions in an exam
     */
    void reorderQuestions(String examId, List<String> questionIds, String instructorId);

    // ===================================
    // STUDENT EXAM TAKING
    // ===================================

    /**
     * Get exam for student viewing (without sensitive data)
     */
    Exam getStudentExam(String examId, String studentId);

    /**
     * Start a new exam attempt
     */
    ExamResponse startExam(String examId, String studentId);

    /**
     * Save exam progress
     */
    ExamResponse saveProgress(ExamResponseRequest request, String studentId);

    /**
     * Submit exam
     */
    ExamResponse submitExam(ExamResponseRequest request, String studentId);

    // ===================================
    // RESPONSE MANAGEMENT
    // ===================================

    /**
     * Get all responses for an exam
     */
    List<ExamResponse> getExamResponses(String examId);

    /**
     * Get a specific response by ID
     */
    ExamResponse getResponse(String responseId);

    /**
     * Get all responses for a student in a course
     */
    List<ExamResponse> getStudentResponses(String studentId, String courseId);

    /**
     * NEW: Get responses for a specific student and exam (response history)
     */
    List<ExamResponse> getStudentExamResponses(String examId, String studentId);

    // ===================================
    // GRADING
    // ===================================

    /**
     * Grade an exam response manually
     */
    ExamResponse gradeResponse(ExamGradeRequest request, String instructorId);

    /**
     * Auto-grade a single response
     */
    ExamResponse autoGradeResponse(String responseId);

    /**
     * Auto-grade all responses for an exam
     */
    List<ExamResponse> autoGradeAllResponses(String examId);

    /**
     * NEW: Update individual question score
     */
    ExamResponse updateQuestionScore(String responseId, String questionId, Integer score, String feedback, String instructorId);

    /**
     * NEW: Flag response for review
     */
    ExamResponse flagResponseForReview(String responseId, String reason, String priority, String instructorId);

    /**
     * NEW: Unflag response
     */
    ExamResponse unflagResponse(String responseId, String instructorId);

    /**
     * NEW: Batch grade multiple responses
     */
    List<ExamResponse> batchGradeResponses(List<String> responseIds, String instructorFeedback, Boolean flagForReview, String instructorId);

    /**
     * NEW: Get grading statistics for an exam
     */
    Map<String, Object> getExamGradingStats(String examId);

    // ===================================
    // STATISTICS
    // ===================================

    /**
     * Get exam statistics
     */
    ExamStatsResponse getExamStats(String examId);

    /**
     * Get statistics for all exams in a course
     */
    List<ExamStatsResponse> getCourseExamStats(String courseId);

    // ===================================
    // VALIDATION
    // ===================================

    /**
     * Check if a student can take an exam
     */
    boolean canStudentTakeExam(String examId, String studentId);

    /**
     * Get the number of attempts a student has made for an exam
     */
    int getStudentAttemptCount(String examId, String studentId);

    /**
     * Check if a student has an active attempt for an exam
     */
    boolean hasActiveAttempt(String examId, String studentId);
}