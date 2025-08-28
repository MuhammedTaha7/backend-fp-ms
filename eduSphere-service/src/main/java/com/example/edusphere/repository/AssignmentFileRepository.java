package com.example.edusphere.repository;

import com.example.edusphere.entity.AssignmentFile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AssignmentFile entity
 */
@Repository
public interface AssignmentFileRepository extends MongoRepository<AssignmentFile, String> {

    /**
     * Find all files for a specific assignment
     */
    List<AssignmentFile> findByAssignmentIdAndStatus(String assignmentId, String status);

    /**
     * Find all active files for a specific assignment
     */
    default List<AssignmentFile> findActiveByAssignmentId(String assignmentId) {
        return findByAssignmentIdAndStatus(assignmentId, "active");
    }

    /**
     * Find all files for a specific course
     */
    List<AssignmentFile> findByCourseIdAndStatus(String courseId, String status);

    /**
     * Find all active files for a specific course
     */
    default List<AssignmentFile> findActiveByCourseId(String courseId) {
        return findByCourseIdAndStatus(courseId, "active");
    }

    /**
     * Find files uploaded by a specific user
     */
    List<AssignmentFile> findByUploadedByAndStatus(String uploadedBy, String status);

    /**
     * Find all active files uploaded by a specific user
     */
    default List<AssignmentFile> findActiveByUploadedBy(String uploadedBy) {
        return findByUploadedByAndStatus(uploadedBy, "active");
    }

    /**
     * Find files by assignment and uploader
     */
    List<AssignmentFile> findByAssignmentIdAndUploadedByAndStatus(String assignmentId, String uploadedBy, String status);

    /**
     * Find active file by assignment and uploader
     */
    default List<AssignmentFile> findActiveByAssignmentIdAndUploadedBy(String assignmentId, String uploadedBy) {
        return findByAssignmentIdAndUploadedByAndStatus(assignmentId, uploadedBy, "active");
    }

    /**
     * Find files by original filename (for duplicate checking)
     */
    List<AssignmentFile> findByAssignmentIdAndOriginalFilenameAndStatus(String assignmentId, String originalFilename, String status);

    /**
     * Find active files by original filename in assignment
     */
    default List<AssignmentFile> findActiveByAssignmentIdAndOriginalFilename(String assignmentId, String originalFilename) {
        return findByAssignmentIdAndOriginalFilenameAndStatus(assignmentId, originalFilename, "active");
    }

    /**
     * Find files by stored filename
     */
    Optional<AssignmentFile> findByStoredFilenameAndStatus(String storedFilename, String status);

    /**
     * Find active file by stored filename
     */
    default Optional<AssignmentFile> findActiveByStoredFilename(String storedFilename) {
        return findByStoredFilenameAndStatus(storedFilename, "active");
    }

    /**
     * Find files by file hash (for integrity checking)
     */
    List<AssignmentFile> findByFileHashAndStatus(String fileHash, String status);

    /**
     * Find files visible to students for an assignment
     */
    List<AssignmentFile> findByAssignmentIdAndVisibleToStudentsAndStatus(String assignmentId, Boolean visibleToStudents, String status);

    /**
     * Find active files visible to students for an assignment
     */
    default List<AssignmentFile> findActiveVisibleToStudentsByAssignmentId(String assignmentId) {
        return findByAssignmentIdAndVisibleToStudentsAndStatus(assignmentId, true, "active");
    }

    /**
     * Find files by content type
     */
    List<AssignmentFile> findByContentTypeContainingIgnoreCaseAndStatus(String contentType, String status);

    /**
     * Find large files (above certain size)
     */
    @Query("{ 'fileSize': { $gte: ?0 }, 'status': ?1 }")
    List<AssignmentFile> findByFileSizeGreaterThanEqualAndStatus(Long fileSize, String status);

    /**
     * Find files created between dates
     */
    List<AssignmentFile> findByCreatedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, String status);

    /**
     * Find files last accessed before a certain date (for cleanup)
     */
    @Query("{ 'lastAccessed': { $lt: ?0 }, 'status': ?1 }")
    List<AssignmentFile> findByLastAccessedBeforeAndStatus(LocalDateTime date, String status);

    /**
     * Find files that have never been accessed
     */
    @Query("{ 'lastAccessed': { $exists: false }, 'status': ?0 }")
    List<AssignmentFile> findNeverAccessedByStatus(String status);

    /**
     * Count files by assignment
     */
    long countByAssignmentIdAndStatus(String assignmentId, String status);

    /**
     * Count active files by assignment
     */
    default long countActiveByAssignmentId(String assignmentId) {
        return countByAssignmentIdAndStatus(assignmentId, "active");
    }

    /**
     * Count files by course
     */
    long countByCourseIdAndStatus(String courseId, String status);

    /**
     * Count active files by course
     */
    default long countActiveByCourseId(String courseId) {
        return countByCourseIdAndStatus(courseId, "active");
    }

    /**
     * Calculate total file size by assignment
     */
    @Query(value = "{ 'assignmentId': ?0, 'status': ?1 }", fields = "{ 'fileSize': 1 }")
    List<AssignmentFile> findFileSizesByAssignmentIdAndStatus(String assignmentId, String status);

    /**
     * Calculate total file size by course
     */
    @Query(value = "{ 'courseId': ?0, 'status': ?1 }", fields = "{ 'fileSize': 1 }")
    List<AssignmentFile> findFileSizesByCourseIdAndStatus(String courseId, String status);

    /**
     * Find files by extension
     */
    List<AssignmentFile> findByFileExtensionIgnoreCaseAndStatus(String extension, String status);

    /**
     * Find most downloaded files
     */
    @Query("{ 'status': ?0 }")
    List<AssignmentFile> findByStatusOrderByDownloadCountDesc(String status);

    /**
     * Find recently uploaded files
     */
    @Query("{ 'status': ?0 }")
    List<AssignmentFile> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Custom query to find files for statistics
     */
    @Query("{ 'assignmentId': ?0, 'status': 'active' }")
    List<AssignmentFile> findStatsByAssignmentId(String assignmentId);

    /**
     * Delete all files for an assignment (when assignment is deleted)
     */
    void deleteByAssignmentId(String assignmentId);

    /**
     * Check if assignment has any files
     */
    boolean existsByAssignmentIdAndStatus(String assignmentId, String status);

    /**
     * Check if assignment has any active files
     */
    default boolean hasActiveFiles(String assignmentId) {
        return existsByAssignmentIdAndStatus(assignmentId, "active");
    }
}