package com.example.edusphere.repository;

import com.example.edusphere.entity.Assignment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Assignment documents.
 * Extends MongoRepository to provide standard CRUD operations.
 */
@Repository
public interface AssignmentRepository extends MongoRepository<Assignment, String> {

    List<Assignment> findByDueDateAfter(LocalDate date);
    List<Assignment> findByCourse(String course);

    // Finds all assignments for a given list of course names.
    List<Assignment> findByCourseIn(List<String> courseNames);
    // Finds all assignments created by a specific instructor.
    List<Assignment> findByInstructorId(String instructorId);
}