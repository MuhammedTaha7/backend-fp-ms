package com.example.edusphere.repository;

import com.example.edusphere.entity.Exam;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExamRepository extends MongoRepository<Exam, String> {

    List<Exam> findByCourseId(String courseId);

    List<Exam> findByCourseIdOrderByCreatedAtDesc(String courseId);

    List<Exam> findByInstructorId(String instructorId);

    List<Exam> findByCourseIdAndStatus(String courseId, String status);

    @Query("{ 'courseId': ?0, 'visibleToStudents': true, 'status': 'PUBLISHED' }")
    List<Exam> findVisibleExamsByCourse(String courseId);

    @Query("{ 'courseId': ?0, 'status': 'PUBLISHED', 'startTime': { $lte: ?1 }, 'endTime': { $gte: ?1 } }")
    List<Exam> findActiveExamsByCourse(String courseId, LocalDateTime now);

    @Query("{ 'courseId': ?0, 'status': 'PUBLISHED', 'startTime': { $gt: ?1 } }")
    List<Exam> findUpcomingExamsByCourse(String courseId, LocalDateTime now);

    @Query("{ 'courseId': ?0, 'endTime': { $lt: ?1 } }")
    List<Exam> findCompletedExamsByCourse(String courseId, LocalDateTime now);

    long countByCourseId(String courseId);

    long countByCourseIdAndStatus(String courseId, String status);

    boolean existsByIdAndInstructorId(String examId, String instructorId);

    @Query("{ 'courseId': ?0, 'title': { $regex: ?1, $options: 'i' } }")
    List<Exam> findByCourseIdAndTitleContainingIgnoreCase(String courseId, String title);

    List<Exam> findByCourseIdOrderByStartTimeAsc(String courseId);
}