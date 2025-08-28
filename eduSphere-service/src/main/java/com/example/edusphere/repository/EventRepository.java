package com.example.edusphere.repository;

import com.example.edusphere.entity.Event;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface EventRepository extends MongoRepository<Event, String> {

    // Find all events for a specific group of students
    List<Event> findByLearningGroupId(String learningGroupId);

    // Find all events for a specific instructor
    List<Event> findByInstructorId(String instructorId);
    List<Event> findByCourseId(String courseId);


    @Query("{'startDate': {$gte: ?0}, 'endDate': {$lte: ?1}}")
    List<Event> findByDateRange(LocalDate startDate, LocalDate endDate);

    @Query("{'instructorId': ?0, 'dayOfWeek': ?1, 'startTime': {$gte: ?2}, 'endTime': {$lte: ?3}}")
    List<Event> findConflictingEventsForLecturer(String instructorId, DayOfWeek dayOfWeek,
                                                 LocalTime startTime, LocalTime endTime);

    @Query("{'learningGroupId': ?0, 'dayOfWeek': ?1, 'startTime': {$gte: ?2}, 'endTime': {$lte: ?3}}")
    List<Event> findConflictingEventsForGroup(String groupId, DayOfWeek dayOfWeek,
                                              LocalTime startTime, LocalTime endTime);

    @Query("{'type': ?0}")
    List<Event> findByType(String type);

    void deleteByCourseId(String courseId);

    void deleteByInstructorId(String instructorId);

    @Query("{'startDate': {$gte: ?0}, 'endDate': {$lte: ?1}}")
    void deleteByDateRange(LocalDate startDate, LocalDate endDate);
}