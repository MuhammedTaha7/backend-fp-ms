package com.example.edusphere.repository;

import com.example.edusphere.entity.Submission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Submission documents.
 * Extends MongoRepository to provide standard CRUD operations.
 */
@Repository
public interface SubmissionRepository extends MongoRepository<Submission, String> {

    /**
     * Finds all submissions for a specific course.
     * This will be used to get the data for the course-specific charts.
     * @param courseId The ID of the course.
     * @return A list of all submissions for that course.
     */
    List<Submission> findByCourseId(String courseId);

    /**
     * Finds all submissions made by a specific student.
     * @param studentId The ID of the student.
     * @return A list of all submissions by that student.
     */
    List<Submission> findByStudentId(String studentId);

    List<Submission> findByCourseIdAndSubmittedAtBetween(String courseId, LocalDateTime startOfYear, LocalDateTime endOfYear);
}
