// Add this method to your TaskSubmissionRepository.java

package com.example.edusphere.repository;

import com.example.edusphere.entity.TaskSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskSubmissionRepository extends MongoRepository<TaskSubmission, String> {

    // Existing methods...
    List<TaskSubmission> findByTaskIdOrderBySubmittedAtDesc(String taskId);
    Page<TaskSubmission> findByTaskIdOrderBySubmittedAtDesc(String taskId, Pageable pageable);
    List<TaskSubmission> findByCourseIdOrderBySubmittedAtDesc(String courseId);
    List<TaskSubmission> findByStudentIdOrderBySubmittedAtDesc(String studentId);
    Optional<TaskSubmission> findByTaskIdAndStudentId(String taskId, String studentId);
    long countByTaskIdAndStudentId(String taskId, String studentId);
    boolean existsByTaskIdAndStudentId(String taskId, String studentId);
    long countByTaskId(String taskId);
    List<TaskSubmission> findByTaskId(String taskId);
    List<TaskSubmission> findByTaskIdAndIsLateTrue(String taskId);
    List<TaskSubmission> findByCourseIdAndSubmittedAtBetween(String courseId, LocalDateTime start, LocalDateTime end);

    // ✅ ADD THIS NEW METHOD
    /**
     * Find submissions that don't have a courseId set
     * This is needed to fix existing submissions in the database
     */
    @Query("{'course_id': null}")
    List<TaskSubmission> findByCourseIdIsNull();

    // Custom queries for grading
    @Query("{'task_id': ?0, 'grade': null}")
    List<TaskSubmission> findUngraduatedSubmissionsByTask(String taskId);

    @Query("{'task_id': ?0, 'grade': {$ne: null}}")
    List<TaskSubmission> findGradedSubmissionsByTask(String taskId);

    @Query("{'task_id': ?0, 'grade': {$ne: null}}")
    long countGradedSubmissionsByTask(String taskId);

    @Query("{'course_id': ?0, $or: [{'status': 'submitted'}, {'grade': null}]}")
    List<TaskSubmission> findSubmissionsNeedingAttention(String courseId);
}