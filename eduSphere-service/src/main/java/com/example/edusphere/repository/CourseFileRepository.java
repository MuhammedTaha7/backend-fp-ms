package com.example.edusphere.repository;

import com.example.edusphere.entity.CourseFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseFileRepository extends MongoRepository<CourseFile, String> {

    /**
     * Find all files belonging to a specific category
     */
    List<CourseFile> findByCategoryId(String categoryId);

    /**
     * Find all files belonging to a specific category with pagination
     */
    Page<CourseFile> findByCategoryId(String categoryId, Pageable pageable);

    /**
     * Delete all files belonging to a specific category
     */
    void deleteByCategoryId(String categoryId);

    /**
     * Count files in a specific category
     */
    long countByCategoryId(String categoryId);

    /**
     * Check if files exist for a specific category
     */
    boolean existsByCategoryId(String categoryId);

    /**
     * Find files by filename pattern (for search functionality)
     */
    List<CourseFile> findByCategoryIdAndFileNameContainingIgnoreCase(String categoryId, String filename);

    /**
     * Find files by filename pattern with pagination
     */
    Page<CourseFile> findByCategoryIdAndFileNameContainingIgnoreCase(String categoryId, String filename, Pageable pageable);

    /**
     * Find files by file type
     */
    List<CourseFile> findByCategoryIdAndFileType(String categoryId, String fileType);

    /**
     * Find files by file type with pagination
     */
    Page<CourseFile> findByCategoryIdAndFileType(String categoryId, String fileType, Pageable pageable);

    /**
     * Find files larger than specified size
     */
    List<CourseFile> findByCategoryIdAndSizeGreaterThan(String categoryId, long size);

    /**
     * Find files smaller than specified size
     */
    List<CourseFile> findByCategoryIdAndSizeLessThan(String categoryId, long size);

    /**
     * Find files uploaded after a specific date
     */
    List<CourseFile> findByCategoryIdAndUploadDateAfter(String categoryId, LocalDateTime date);

    /**
     * Find files uploaded before a specific date
     */
    List<CourseFile> findByCategoryIdAndUploadDateBefore(String categoryId, LocalDateTime date);

    /**
     * Find files uploaded between two dates
     */
    List<CourseFile> findByCategoryIdAndUploadDateBetween(String categoryId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find files by uploaded user
     */
    List<CourseFile> findByCategoryIdAndUploadedBy(String categoryId, String uploadedBy);

    /**
     * Find active files only
     */
    List<CourseFile> findByCategoryIdAndIsActiveTrue(String categoryId);

    /**
     * Find active files with pagination
     */
    Page<CourseFile> findByCategoryIdAndIsActiveTrue(String categoryId, Pageable pageable);

    /**
     * Find files by extension using custom query
     */
    @Query("{ 'categoryId': ?0, 'fileName': { $regex: ?1, $options: 'i' } }")
    List<CourseFile> findByCategoryIdAndFileExtension(String categoryId, String extensionPattern);

    /**
     * Find most downloaded files in a category
     */
    List<CourseFile> findTop10ByCategoryIdOrderByDownloadCountDesc(String categoryId);

    /**
     * Find recently uploaded files in a category
     */
    List<CourseFile> findTop10ByCategoryIdOrderByUploadDateDesc(String categoryId);

    /**
     * Find files by stored filename (for internal use)
     */
    Optional<CourseFile> findByStoredFileName(String storedFileName);

    /**
     * Count files by file type in a category
     */
    long countByCategoryIdAndFileType(String categoryId, String fileType);

    /**
     * Sum total size of files in a category
     */
    @Query(value = "{ 'categoryId': ?0 }", fields = "{ 'size': 1 }")
    List<CourseFile> findSizesByCategoryId(String categoryId);
}