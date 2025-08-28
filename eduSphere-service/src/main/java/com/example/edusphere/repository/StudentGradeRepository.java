package com.example.edusphere.repository;

import com.example.edusphere.entity.StudentGrade;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentGradeRepository extends MongoRepository<StudentGrade, String> {

    /**
     * Find a single student grade record by student ID and course ID
     * Uses findFirst to handle potential duplicates gracefully
     */
    Optional<StudentGrade> findFirstByStudentIdAndCourseIdOrderByUpdatedAtDesc(String studentId, String courseId);

    /**
     * Find all grades for a course
     */
    List<StudentGrade> findByCourseId(String courseId);

    /**
     * Find all grades for a student
     */
    List<StudentGrade> findByStudentId(String studentId);

    /**
     * Delete grades by student and course
     */
    void deleteByStudentIdAndCourseId(String studentId, String courseId);

    /**
     * Check if a grade record exists
     */
    boolean existsByStudentIdAndCourseId(String studentId, String courseId);

    /**
     * Find duplicate records (for cleanup)
     */
    @Query("{ 'studentId': ?0, 'courseId': ?1 }")
    List<StudentGrade> findAllByStudentIdAndCourseId(String studentId, String courseId);

    /**
     * Find all grades with non-null final grades (for GPA calculations)
     * Added for dashboard GPA calculations
     */
    @Query("{ 'finalGrade': { $ne: null } }")
    List<StudentGrade> findAllWithFinalGrades();

    /**
     * Find grades by student IDs (useful for bulk operations)
     */
    List<StudentGrade> findByStudentIdIn(List<String> studentIds);

    /**
     * Find grades by course IDs (useful for bulk operations)
     */
    List<StudentGrade> findByCourseIdIn(List<String> courseIds);
}