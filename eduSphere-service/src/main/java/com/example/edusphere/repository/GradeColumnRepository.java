package com.example.edusphere.repository;

import com.example.edusphere.entity.GradeColumn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeColumnRepository extends MongoRepository<GradeColumn, String> {

    // Find by course
    List<GradeColumn> findByCourseId(String courseId);
    List<GradeColumn> findByCourseIdAndIsActiveTrue(String courseId);
    List<GradeColumn> findByCourseIdOrderByDisplayOrderAsc(String courseId);
    List<GradeColumn> findByCourseIdOrderByDisplayOrderDesc(String courseId);

    // Find linked assignment columns
    Optional<GradeColumn> findByCourseIdAndLinkedAssignmentId(String courseId, String linkedAssignmentId);
    List<GradeColumn> findAllByCourseIdAndLinkedAssignmentId(String courseId, String linkedAssignmentId);
    List<GradeColumn> findByLinkedAssignmentIdIsNotNull();
    List<GradeColumn> findByCourseIdAndLinkedAssignmentIdIsNotNull(String courseId);

    // Find auto-created columns
    List<GradeColumn> findByAutoCreatedTrue();
    List<GradeColumn> findByCourseIdAndAutoCreatedTrue(String courseId);

    // Find by type
    List<GradeColumn> findByCourseIdAndType(String courseId, String type);

    // Find by creation method
    List<GradeColumn> findByCourseIdAndCreatedBy(String courseId, String createdBy);

    // Custom queries for validation
    @Query("{ 'courseId': ?0, 'isActive': true }")
    List<GradeColumn> findActiveByCourse(String courseId);

    @Query("{ 'courseId': ?0, 'linkedAssignmentId': { $exists: true, $ne: null } }")
    List<GradeColumn> findLinkedColumnsByCourse(String courseId);

    @Query("{ 'courseId': ?0, 'linkedAssignmentId': { $exists: false } }")
    List<GradeColumn> findManualColumnsByCourse(String courseId);

    // Check if column exists for assignment
    boolean existsByCourseIdAndLinkedAssignmentId(String courseId, String linkedAssignmentId);

    // Find orphaned columns (linked to non-existent assignments)
    @Query("{ 'linkedAssignmentId': { $exists: true, $ne: null }, 'linkedAssignmentId': { $nin: ?0 } }")
    List<GradeColumn> findOrphanedLinkedColumns(List<String> existingAssignmentIds);

    // Delete by linked assignment
    void deleteByLinkedAssignmentId(String linkedAssignmentId);
    void deleteByCourseIdAndLinkedAssignmentId(String courseId, String linkedAssignmentId);

    // Count methods
    long countByCourseId(String courseId);
    long countByCourseIdAndIsActiveTrue(String courseId);
    long countByCourseIdAndAutoCreatedTrue(String courseId);

}