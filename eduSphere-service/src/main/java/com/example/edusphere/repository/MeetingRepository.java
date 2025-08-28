package com.example.edusphere.repository;

import com.example.edusphere.entity.Meeting;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Meeting Repository Interface
 * Provides data access methods for Meeting entities
 */
@Repository
public interface MeetingRepository extends MongoRepository<Meeting, String> {

    // ===== FIND BY USER =====

    /**
     * Find meetings created by a specific user
     */
    List<Meeting> findByCreatedByOrderByDatetimeDesc(String createdBy);

    /**
     * Find meetings where user is the assigned lecturer
     */
    List<Meeting> findByLecturerIdOrderByDatetimeDesc(String lecturerId);

    /**
     * Find meetings where user is in participants list
     */
    List<Meeting> findByParticipantsContaining(String userId);

    // ===== FIND BY COURSE =====

    /**
     * Find meetings for a specific course
     */
    List<Meeting> findByCourseIdOrderByDatetimeDesc(String courseId);

    /**
     * Find meetings for multiple courses
     */
    List<Meeting> findByCourseIdIn(List<String> courseIds);

    /**
     * Find meetings for a course with specific status
     */
    List<Meeting> findByCourseIdAndStatusOrderByDatetimeDesc(String courseId, String status);

    // ===== FIND BY STATUS =====

    /**
     * Find meetings by status
     */
    List<Meeting> findByStatusOrderByDatetimeDesc(String status);

    /**
     * Find active meetings
     */
    List<Meeting> findByStatusAndDatetimeBetweenOrderByDatetimeDesc(
            String status, LocalDateTime startTime, LocalDateTime endTime);

    // ===== FIND BY DATE =====

    /**
     * Find meetings scheduled after a specific date
     */
    List<Meeting> findByDatetimeAfterOrderByDatetimeAsc(LocalDateTime datetime);

    /**
     * Find meetings scheduled before a specific date
     */
    List<Meeting> findByDatetimeBeforeOrderByDatetimeDesc(LocalDateTime datetime);

    /**
     * Find meetings within a date range
     */
    List<Meeting> findByDatetimeBetweenOrderByDatetimeAsc(LocalDateTime startDate, LocalDateTime endDate);

    // ===== FIND BY TYPE =====

    /**
     * Find meetings by type
     */
    List<Meeting> findByTypeOrderByDatetimeDesc(String type);

    /**
     * Find meetings by type and status
     */
    List<Meeting> findByTypeAndStatusOrderByDatetimeDesc(String type, String status);

    // ===== CUSTOM QUERIES =====

    /**
     * Find upcoming meetings for a lecturer
     */
    @Query("{ 'lecturerId': ?0, 'datetime': { $gte: ?1 }, 'status': { $ne: 'cancelled' } }")
    List<Meeting> findUpcomingMeetingsForLecturer(String lecturerId, LocalDateTime now);

    /**
     * Find ongoing meetings
     */
    @Query("{ 'status': 'active' }")
    List<Meeting> findActiveMeetings();

    /**
     * Find meetings with active attendance sessions
     */
    @Query("{ 'attendanceSessions': { $elemMatch: { 'leaveTime': null } } }")
    List<Meeting> findMeetingsWithActiveSessions();

    /**
     * Find meetings by room ID
     */
    Optional<Meeting> findByRoomId(String roomId);

    /**
     * Find meetings created in the last N days
     */
    @Query("{ 'createdAt': { $gte: ?0 } }")
    List<Meeting> findMeetingsCreatedAfter(LocalDateTime cutoffDate);

    /**
     * Count meetings for a course
     */
    long countByCourseId(String courseId);

    /**
     * Count meetings by lecturer
     */
    long countByLecturerId(String lecturerId);

    /**
     * Count meetings by status
     */
    long countByStatus(String status);

    /**
     * Find meetings with attendance data
     */
    @Query("{ 'attendanceSessions': { $exists: true, $not: { $size: 0 } } }")
    List<Meeting> findMeetingsWithAttendance();

    /**
     * Find meetings by lecturer and date range
     */
    @Query("{ 'lecturerId': ?0, 'datetime': { $gte: ?1, $lte: ?2 } }")
    List<Meeting> findByLecturerAndDateRange(String lecturerId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find meetings by course and date range
     */
    @Query("{ 'courseId': ?0, 'datetime': { $gte: ?1, $lte: ?2 } }")
    List<Meeting> findByCourseAndDateRange(String courseId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find meetings that need to be ended (active but past end time)
     */
    @Query("{ 'status': 'active', 'endTime': { $lt: ?0 } }")
    List<Meeting> findMeetingsThatShouldBeEnded(LocalDateTime now);

    /**
     * Find scheduled meetings that should be started
     */
    @Query("{ 'status': 'scheduled', 'datetime': { $lte: ?0 } }")
    List<Meeting> findScheduledMeetingsThatShouldStart(LocalDateTime now);
}