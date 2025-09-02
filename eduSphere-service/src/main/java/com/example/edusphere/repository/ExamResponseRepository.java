package com.example.edusphere.repository;

import com.example.edusphere.entity.ExamResponse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamResponseRepository extends MongoRepository<ExamResponse, String> {

    // ===================================
    // BASIC QUERIES (EXISTING - KEEP THESE)
    // ===================================

    /**
     * Find all responses for an exam
     */
    List<ExamResponse> findByExamId(String examId);

    /**
     * Find all responses for an exam ordered by submission time
     */
    List<ExamResponse> findByExamIdOrderBySubmittedAtDesc(String examId);

    /**
     * Find all responses by a student
     */
    List<ExamResponse> findByStudentId(String studentId);

    /**
     * Find all responses by a student in a specific course
     */
    List<ExamResponse> findByStudentIdAndCourseId(String studentId, String courseId);

    /**
     * Find a specific response by exam and student
     */
    Optional<ExamResponse> findByExamIdAndStudentId(String examId, String studentId);

    /**
     * Find all responses for a student in an exam, ordered by attempt number
     */
    List<ExamResponse> findByExamIdAndStudentIdOrderByAttemptNumberDesc(String examId, String studentId);

    /**
     * Find responses by exam and status
     */
    List<ExamResponse> findByExamIdAndStatus(String examId, String status);

    // ===================================
    // ACTIVE RESPONSE QUERIES
    // ===================================

    /**
     * Find active (in-progress) response for a student in an exam
     */
    @Query("{ 'examId': ?0, 'studentId': ?1, 'status': 'IN_PROGRESS' }")
    Optional<ExamResponse> findActiveResponse(String examId, String studentId);

    // ===================================
    // GRADING STATUS QUERIES
    // ===================================

    /**
     * Find ungraded responses for an exam (submitted but not graded)
     */
    @Query("{ 'examId': ?0, 'graded': { $ne: true }, 'status': 'SUBMITTED' }")
    List<ExamResponse> findUngraded(String examId);

    /**
     * Find ungraded responses for a course
     */
    @Query("{ 'courseId': ?0, 'graded': { $ne: true }, 'status': 'SUBMITTED' }")
    List<ExamResponse> findUngradedByCourse(String courseId);

    /**
     *  Find responses that need grading (submitted but not graded)
     */
    @Query("{ 'examId': ?0, 'status': 'SUBMITTED', 'graded': { $ne: true } }")
    List<ExamResponse> findNeedingGrading(String examId);

    /**
     *  Find flagged responses for an exam
     */
    @Query("{ 'examId': ?0, 'flaggedForReview': true }")
    List<ExamResponse> findFlaggedResponses(String examId);

    /**
     *  Find auto-graded responses for an exam
     */
    @Query("{ 'examId': ?0, 'autoGraded': true }")
    List<ExamResponse> findAutoGradedResponses(String examId);

    /**
     *  Find manually graded responses for an exam
     */
    @Query("{ 'examId': ?0, 'graded': true, 'autoGraded': { $ne: true } }")
    List<ExamResponse> findManuallyGradedResponses(String examId);

    /**
     *  Find responses graded by a specific instructor
     */
    List<ExamResponse> findByExamIdAndGradedBy(String examId, String gradedBy);

    /**
     *  Find all submitted responses for an exam (submitted, graded, partially graded)
     */
    @Query("{ 'examId': ?0, 'status': { $in: ['SUBMITTED', 'GRADED', 'PARTIALLY_GRADED'] } }")
    List<ExamResponse> findSubmittedResponses(String examId);

    /**
     *  Find responses that passed for an exam
     */
    @Query("{ 'examId': ?0, 'passed': true }")
    List<ExamResponse> findPassedResponses(String examId);

    /**
     *  Find responses submitted after a certain date
     */
    @Query("{ 'examId': ?0, 'submittedAt': { $gte: ?1 } }")
    List<ExamResponse> findByExamIdAndSubmittedAtAfter(String examId, LocalDateTime date);

    /**
     *  Find responses within a grade range
     */
    @Query("{ 'examId': ?0, 'percentage': { $gte: ?1, $lte: ?2 } }")
    List<ExamResponse> findByExamIdAndPercentageBetween(String examId, Double minPercentage, Double maxPercentage);

    // ===================================
    // COUNT QUERIES (EXISTING - KEEP THESE)
    // ===================================

    /**
     * Count total responses for an exam
     */
    long countByExamId(String examId);

    /**
     * Count responses by status for an exam
     */
    long countByExamIdAndStatus(String examId, String status);

    /**
     * Count responses for a student in an exam
     */
    long countByExamIdAndStudentId(String examId, String studentId);

    /**
     * Count graded responses for an exam
     */
    @Query(value = "{ 'examId': ?0, 'graded': true }", count = true)
    long countGradedByExam(String examId);

    /**
     * Count passed responses for an exam
     */
    @Query(value = "{ 'examId': ?0, 'passed': true }", count = true)
    long countPassedByExam(String examId);

    /**
     *  Count flagged responses for an exam
     */
    @Query(value = "{ 'examId': ?0, 'flaggedForReview': true }", count = true)
    long countFlaggedByExamId(String examId);

    /**
     *  Count auto-graded responses for an exam
     */
    @Query(value = "{ 'examId': ?0, 'autoGraded': true }", count = true)
    long countAutoGradedByExamId(String examId);

    /**
     *  Count manually graded responses for an exam
     */
    @Query(value = "{ 'examId': ?0, 'graded': true, 'autoGraded': { $ne: true } }", count = true)
    long countManuallyGradedByExamId(String examId);

    /**
     *  Count responses needing grading for an exam
     */
    @Query(value = "{ 'examId': ?0, 'status': 'SUBMITTED', 'graded': { $ne: true } }", count = true)
    long countNeedingGradingByExamId(String examId);

    // ===================================
    // DELETE QUERIES (EXISTING - KEEP THESE)
    // ===================================

    /**
     * Delete all responses for an exam
     */
    void deleteByExamId(String examId);

    /**
     * Delete responses for a specific student and exam
     */
    void deleteByStudentIdAndExamId(String studentId, String examId);

    // ===================================
    // ADVANCED QUERIES FOR STATISTICS
    // ===================================

    /**
     *  Find responses by multiple statuses
     */
    @Query("{ 'examId': ?0, 'status': { $in: ?1 } }")
    List<ExamResponse> findByExamIdAndStatusIn(String examId, List<String> statuses);

    /**
     *  Find responses submitted within a date range
     */
    @Query("{ 'examId': ?0, 'submittedAt': { $gte: ?1, $lte: ?2 } }")
    List<ExamResponse> findByExamIdAndSubmittedAtBetween(String examId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     *  Find responses by course and status
     */
    List<ExamResponse> findByCourseIdAndStatus(String courseId, String status);

    /**
     *  Find latest response for each student in an exam
     */
    @Query("{ 'examId': ?0 }")
    List<ExamResponse> findLatestResponsePerStudentByExamId(String examId);

    /**
     *  Find responses with specific grading criteria
     */
    @Query("{ 'examId': ?0, 'graded': ?1, 'autoGraded': ?2 }")
    List<ExamResponse> findByExamIdAndGradedAndAutoGraded(String examId, Boolean graded, Boolean autoGraded);

    /**
     *  Find overdue responses (submitted after exam end time)
     */
    @Query("{ 'examId': ?0, 'lateSubmission': true }")
    List<ExamResponse> findLateSubmissionsByExamId(String examId);

    /**
     *  Count late submissions for an exam
     */
    @Query(value = "{ 'examId': ?0, 'lateSubmission': true }", count = true)
    long countLateSubmissionsByExamId(String examId);

    /**
     *  Find responses that need manual grading (essay questions, etc.)
     */
    @Query("{ 'examId': ?0, 'status': { $in: ['SUBMITTED', 'PARTIALLY_GRADED'] }, $or: [ { 'graded': { $ne: true } }, { 'flaggedForReview': true } ] }")
    List<ExamResponse> findNeedingManualGrading(String examId);

    /**
     *  Find responses by course within date range
     */
    @Query("{ 'courseId': ?0, 'submittedAt': { $gte: ?1, $lte: ?2 } }")
    List<ExamResponse> findByCourseIdAndSubmittedAtBetween(String courseId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     *  Find top scoring responses for an exam
     */
    @Query(value = "{ 'examId': ?0, 'graded': true }", sort = "{ 'percentage': -1 }")
    List<ExamResponse> findTopScoringResponsesByExamId(String examId);

    /**
     *  Find bottom scoring responses for an exam
     */
    @Query(value = "{ 'examId': ?0, 'graded': true }", sort = "{ 'percentage': 1 }")
    List<ExamResponse> findBottomScoringResponsesByExamId(String examId);
}