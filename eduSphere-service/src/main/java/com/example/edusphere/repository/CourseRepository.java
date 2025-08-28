package com.example.edusphere.repository;

import com.example.edusphere.entity.Course;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Course documents.
 * Extends MongoRepository to provide standard CRUD operations.
 */
@Repository
public interface CourseRepository extends MongoRepository<Course, String> {
    List<Course> findByDepartment(String department);

    // Finds all courses where a student is enrolled in any academic year.
    List<Course> findByEnrollments_StudentIds(String studentId);

    // Finds all courses taught by a specific lecturer
    List<Course> findByLecturerId(String lecturerId);

    // Finds all courses taught by a specific lecturer in a specific department
    List<Course> findByLecturerIdAndDepartment(String lecturerId, String department);

    List<Course> findByIdIn(List<String> courseIds);
}