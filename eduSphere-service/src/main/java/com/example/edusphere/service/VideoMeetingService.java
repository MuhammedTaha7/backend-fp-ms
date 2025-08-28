package com.example.edusphere.service;

import com.example.edusphere.entity.Course;
import com.example.edusphere.entity.Meeting;
import com.example.edusphere.entity.AttendanceSession;
import com.example.common.entity.UserEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Video Meeting Service Interface
 * Defines all operations for video meeting management, attendance tracking, and analytics
 */
public interface VideoMeetingService {

    // ===== MEETING MANAGEMENT =====

    /**
     * Create a new meeting
     * @param meeting Meeting data
     * @param userId Creator user ID
     * @return Created meeting
     */
    Meeting createMeeting(Meeting meeting, String userId);

    /**
     * Get meetings for a specific user (lecturer or student)
     * @param userId User ID
     * @param filters Optional filters (courseId, type, status, etc.)
     * @return List of user's meetings
     */
    List<Meeting> getUserMeetings(String userId, Map<String, String> filters);

    /**
     * Get meeting by ID
     * @param meetingId Meeting ID
     * @return Optional meeting
     */
    Optional<Meeting> getMeetingById(String meetingId);

    /**
     * Update meeting details
     * @param meetingId Meeting ID
     * @param updateData Data to update
     * @return Updated meeting
     */
    Meeting updateMeeting(String meetingId, Meeting updateData);

    /**
     * Delete a meeting
     * @param meetingId Meeting ID
     */
    void deleteMeeting(String meetingId);

    // ===== MEETING PARTICIPATION =====

    /**
     * Join a meeting (creates attendance session)
     * @param meetingId Meeting ID
     * @param userId User ID
     * @param userName User display name
     * @return Join response with session info
     */
    Map<String, Object> joinMeeting(String meetingId, String userId, String userName);

    /**
     * Leave a meeting (ends attendance session)
     * @param meetingId Meeting ID
     * @param sessionId Attendance session ID
     * @return Leave response with duration info
     */
    Map<String, Object> leaveMeeting(String meetingId, String sessionId);

    /**
     * Get meeting attendance records
     * @param meetingId Meeting ID
     * @return List of attendance sessions
     */
    List<AttendanceSession> getMeetingAttendance(String meetingId);

    /**
     * Start a meeting (for lecturers)
     * @param meetingId Meeting ID
     * @return Started meeting
     */
    Meeting startMeeting(String meetingId);

    /**
     * End a meeting (for lecturers)
     * @param meetingId Meeting ID
     * @return Ended meeting
     */
    Meeting endMeeting(String meetingId);

    // ===== COURSE MEETINGS =====

    /**
     * Get courses for a lecturer
     * @param lecturerId Lecturer ID
     * @return List of lecturer's courses
     */
    List<Course> getLecturerCourses(String lecturerId);

    /**
     * Get courses for a student
     * @param studentId Student ID
     * @return List of student's enrolled courses
     */
    List<Course> getStudentCourses(String studentId);

    /**
     * Get meetings for a specific course
     * @param courseId Course ID
     * @return List of course meetings
     */
    List<Meeting> getCourseMeetings(String courseId);

    /**
     * Get students enrolled in a course
     * @param courseId Course ID
     * @return List of course students
     */
    List<UserEntity> getCourseStudents(String courseId);

    // ===== ANALYTICS =====

    /**
     * Get analytics for a specific meeting
     * @param meetingId Meeting ID
     * @return Meeting analytics
     */
    Map<String, Object> getMeetingAnalytics(String meetingId);

    /**
     * Get analytics for a course
     * @param courseId Course ID
     * @param filters Optional filters
     * @return Course analytics
     */
    Map<String, Object> getCourseAnalytics(String courseId, Map<String, String> filters);

    /**
     * Get analytics for a lecturer
     * @param lecturerId Lecturer ID
     * @param filters Optional filters
     * @return Lecturer analytics
     */
    Map<String, Object> getLecturerAnalytics(String lecturerId, Map<String, String> filters);

    /**
     * Get analytics for a student
     * @param studentId Student ID
     * @param filters Optional filters
     * @return Student analytics
     */
    Map<String, Object> getStudentAnalytics(String studentId, Map<String, String> filters);

    // ===== ATTENDANCE TRACKING =====

    /**
     * Get attendance summary for a user
     * @param userId User ID
     * @param filters Optional filters
     * @return Attendance summary
     */
    Map<String, Object> getAttendanceSummary(String userId, Map<String, String> filters);

    /**
     * Get attendance records for a course
     * @param courseId Course ID
     * @param filters Optional filters
     * @return List of attendance sessions
     */
    List<AttendanceSession> getCourseAttendance(String courseId, Map<String, String> filters);

    /**
     * Get attendance records for a specific student in a course
     * @param courseId Course ID
     * @param studentId Student ID
     * @return List of student's attendance sessions
     */
    List<AttendanceSession> getStudentCourseAttendance(String courseId, String studentId);
}