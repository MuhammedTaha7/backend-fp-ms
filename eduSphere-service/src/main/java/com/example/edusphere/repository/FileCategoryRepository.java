package com.example.edusphere.repository;

import com.example.edusphere.entity.FileCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileCategoryRepository extends MongoRepository<FileCategory, String> {

    /**
     * Find categories by course ID and academic year
     */
    List<FileCategory> findByCourseIdAndAcademicYear(String courseId, int academicYear);

    /**
     * Find categories by course ID only
     */
    List<FileCategory> findByCourseId(String courseId);

    /**
     * Check if category exists with specific name in course and year
     */
    boolean existsByCourseIdAndAcademicYearAndName(String courseId, int academicYear, String name);

    /**
     * Check if category exists with specific name, excluding a particular category ID
     */
    boolean existsByCourseIdAndAcademicYearAndNameAndIdNot(String courseId, int academicYear, String name, String excludeId);

    /**
     * Count categories in a specific course and year
     */
    long countByCourseIdAndAcademicYear(String courseId, int academicYear);

    /**
     * Delete all categories for a specific course
     */
    void deleteByCourseId(String courseId);

    /**
     * Delete all categories for a specific course and year
     */
    void deleteByCourseIdAndAcademicYear(String courseId, int academicYear);

    /**
     * Find categories by name pattern (for search)
     */
    List<FileCategory> findByCourseIdAndAcademicYearAndNameContainingIgnoreCase(String courseId, int academicYear, String namePattern);

    /**
     * Find category by course, year and name
     */
    Optional<FileCategory> findByCourseIdAndAcademicYearAndName(String courseId, int academicYear, String name);

    /**
     * Check if any categories exist for a course
     */
    boolean existsByCourseId(String courseId);

    /**
     * Find categories ordered by display order
     */
    List<FileCategory> findByCourseIdAndAcademicYearOrderByDisplayOrderAsc(String courseId, int academicYear);

    /**
     * Find active categories only
     */
    List<FileCategory> findByCourseIdAndAcademicYearAndIsActiveTrue(String courseId, int academicYear);
}