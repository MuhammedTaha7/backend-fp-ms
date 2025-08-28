package com.example.edusphere.service.impl;

import com.example.edusphere.entity.Course;
import com.example.edusphere.entity.Meeting;
import com.example.edusphere.entity.AttendanceSession;
import com.example.common.entity.UserEntity;
import com.example.edusphere.repository.MeetingRepository;
import com.example.edusphere.repository.CourseRepository;
import com.example.common.repository.UserRepository;
import com.example.edusphere.service.VideoMeetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VideoMeetingServiceImpl implements VideoMeetingService {

    private static final Logger log = LoggerFactory.getLogger(VideoMeetingServiceImpl.class);

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.base.url:http://localhost:3000}")
    private String baseUrl;

    /**
     * Get current time in system default timezone
     */
    private LocalDateTime getCurrentLocalTime() {
        return LocalDateTime.now();
    }

    /**
     * Get current UTC time
     */
    private LocalDateTime getCurrentUtcTime() {
        return ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
    }

    /**
     * Convert UTC datetime to local datetime for frontend display
     */
    private LocalDateTime convertUtcToLocal(LocalDateTime utcDateTime) {
        if (utcDateTime == null) return null;
        return utcDateTime.plusHours(3); // Add 3 hours for Israel timezone (UTC+3)
    }

    // ===== MEETING MANAGEMENT =====

    @Override
    public Meeting createMeeting(Meeting meeting, String userId) {
        try {
            // Set creator
            meeting.setCreatedBy(userId);
            meeting.setCreatedAt(getCurrentUtcTime());

            // Handle datetime fields with UTC conversion
            if (meeting.getScheduledAt() != null && meeting.getDatetime() == null) {
                meeting.setDatetime(meeting.getScheduledAt());
            }

            if (meeting.getDatetime() != null) {
                // Convert to UTC if not already
                meeting.setDatetime(meeting.getDatetime()
                        .atZone(ZoneId.systemDefault())
                        .withZoneSameInstant(ZoneId.of("UTC"))
                        .toLocalDateTime());
            }

            // Set course information if courseId is provided
            if (meeting.getCourseId() != null && !meeting.getCourseId().trim().isEmpty()) {
                Course course = courseRepository.findById(meeting.getCourseId()).orElse(null);
                if (course != null) {
                    meeting.setLecturerId(course.getLecturerId());
                    meeting.setCourseName(course.getName());
                    meeting.setCourseCode(course.getCode());

                    // Calculate students count
                    int studentsCount = calculateCourseStudentsCount(course);
                    meeting.setStudentsCount(studentsCount);
                } else {
                    log.warn("Course not found: {}", meeting.getCourseId());
                }
            }

            // Initialize default values
            meeting.initializeDefaults();

            // Validate required fields
            if (!meeting.isValid()) {
                throw new RuntimeException("Meeting validation failed: missing required fields");
            }

            // Save meeting first to get ID
            Meeting savedMeeting = meetingRepository.save(meeting);

            // Generate invitation link after meeting is saved
            if (savedMeeting.getId() != null && savedMeeting.getRoomId() != null) {
                savedMeeting.generateInvitationLink(baseUrl);
                savedMeeting = meetingRepository.save(savedMeeting);
            }

            return savedMeeting;
        } catch (Exception e) {
            log.error("Failed to create meeting for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create meeting: " + e.getMessage());
        }
    }

    @Override
    public List<Meeting> getUserMeetings(String userId, Map<String, String> filters) {
        try {
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("User not found: {}", userId);
                return new ArrayList<>();
            }

            List<Meeting> meetings = new ArrayList<>();
            String role = user.getRole();

            if ("1200".equals(role)) {
                // Lecturer: get meetings they created or are assigned to teach
                meetings = meetingRepository.findByLecturerIdOrderByDatetimeDesc(userId);

                // Also include meetings they created directly
                List<Meeting> createdMeetings = meetingRepository.findByCreatedByOrderByDatetimeDesc(userId);
                Set<String> meetingIds = meetings.stream().map(Meeting::getId).collect(Collectors.toSet());

                createdMeetings.stream()
                        .filter(m -> !meetingIds.contains(m.getId()))
                        .forEach(meetings::add);
            } else {
                // Student: get meetings for courses they're enrolled in
                List<Course> studentCourses = courseRepository.findByEnrollments_StudentIds(userId);
                if (studentCourses.isEmpty()) {
                    return new ArrayList<>();
                }

                List<String> courseIds = studentCourses.stream()
                        .map(Course::getId)
                        .collect(Collectors.toList());

                meetings = meetingRepository.findByCourseIdIn(courseIds);
            }

            // Filter meetings: only show meetings from the last 3 hours or future meetings
            List<Meeting> activeMeetings = new ArrayList<>();
            LocalDateTime currentLocalTime = getCurrentLocalTime();
            LocalDateTime threeHoursAgo = currentLocalTime.minusHours(3);

            for (Meeting meeting : meetings) {
                if (meeting.getDatetime() == null) {
                    // No datetime - keep it (instant meetings)
                    activeMeetings.add(meeting);
                } else {
                    // Convert meeting time from UTC to local time
                    LocalDateTime meetingStartLocal = convertUtcToLocal(meeting.getDatetime());
                    int duration = meeting.getDuration() != null ? meeting.getDuration() : 60;
                    LocalDateTime meetingEndLocal = meetingStartLocal.plusMinutes(duration);

                    // Keep meetings that:
                    // 1. Started in the last 3 hours, OR
                    // 2. Are scheduled for the future, OR
                    // 3. Are still ongoing (haven't ended yet)
                    boolean shouldKeep = meetingStartLocal.isAfter(threeHoursAgo) ||
                            meetingStartLocal.isAfter(currentLocalTime) ||
                            meetingEndLocal.isAfter(currentLocalTime);

                    if (shouldKeep) {
                        activeMeetings.add(meeting);
                    }

                    // Debug output
                    System.out.println("MEETING: " + meeting.getTitle() +
                            " | Start UTC: " + meeting.getDatetime() +
                            " | Start Local: " + meetingStartLocal +
                            " | End Local: " + meetingEndLocal +
                            " | Current: " + currentLocalTime +
                            " | 3h ago: " + threeHoursAgo +
                            " | KEPT: " + shouldKeep);
                }
            }

            meetings = activeMeetings;

            // Convert meeting datetimes to local time for frontend display
            meetings.forEach(meeting -> {
                // Convert main datetime field to local time
                if (meeting.getDatetime() != null) {
                    meeting.setDatetime(convertUtcToLocal(meeting.getDatetime()));
                }

                // Convert other datetime fields to local time
                if (meeting.getScheduledAt() != null) {
                    meeting.setScheduledAt(convertUtcToLocal(meeting.getScheduledAt()));
                }

                if (meeting.getStartTime() != null) {
                    meeting.setStartTime(convertUtcToLocal(meeting.getStartTime()));
                }

                if (meeting.getEndTime() != null) {
                    meeting.setEndTime(convertUtcToLocal(meeting.getEndTime()));
                }

                if (meeting.getCreatedAt() != null) {
                    meeting.setCreatedAt(convertUtcToLocal(meeting.getCreatedAt()));
                }

                if (meeting.getUpdatedAt() != null) {
                    meeting.setUpdatedAt(convertUtcToLocal(meeting.getUpdatedAt()));
                }

                updateMeetingStudentsCount(meeting);

                if (meeting.getInvitationLink() == null && meeting.getRoomId() != null) {
                    meeting.generateInvitationLink(baseUrl);
                }
            });

            // Apply additional filters
            meetings = applyFilters(meetings, filters);

            // Sort by datetime (newest first for lecturers, soonest first for students)
            if ("1200".equals(role)) {
                meetings.sort((a, b) -> {
                    LocalDateTime dateA = a.getDatetime() != null ? a.getDatetime() : LocalDateTime.MIN;
                    LocalDateTime dateB = b.getDatetime() != null ? b.getDatetime() : LocalDateTime.MIN;
                    return dateB.compareTo(dateA); // Newest first
                });
            } else {
                meetings.sort((a, b) -> {
                    LocalDateTime dateA = a.getDatetime() != null ? a.getDatetime() : LocalDateTime.MAX;
                    LocalDateTime dateB = b.getDatetime() != null ? b.getDatetime() : LocalDateTime.MAX;
                    return dateA.compareTo(dateB); // Soonest first
                });
            }

            return meetings;
        } catch (Exception e) {
            log.error("Failed to get user meetings for {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get user meetings: " + e.getMessage());
        }
    }

    @Override
    public Optional<Meeting> getMeetingById(String meetingId) {
        try {
            Optional<Meeting> meeting = meetingRepository.findById(meetingId);
            meeting.ifPresent(m -> {
                // Convert all datetime fields to local time for frontend
                if (m.getDatetime() != null) {
                    m.setDatetime(convertUtcToLocal(m.getDatetime()));
                }

                if (m.getScheduledAt() != null) {
                    m.setScheduledAt(convertUtcToLocal(m.getScheduledAt()));
                }

                if (m.getStartTime() != null) {
                    m.setStartTime(convertUtcToLocal(m.getStartTime()));
                }

                if (m.getEndTime() != null) {
                    m.setEndTime(convertUtcToLocal(m.getEndTime()));
                }

                if (m.getCreatedAt() != null) {
                    m.setCreatedAt(convertUtcToLocal(m.getCreatedAt()));
                }

                if (m.getUpdatedAt() != null) {
                    m.setUpdatedAt(convertUtcToLocal(m.getUpdatedAt()));
                }

                updateMeetingStudentsCount(m);

                if (m.getInvitationLink() == null && m.getRoomId() != null) {
                    m.generateInvitationLink(baseUrl);
                }
            });
            return meeting;
        } catch (Exception e) {
            log.error("Failed to get meeting by ID {}: {}", meetingId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Meeting updateMeeting(String meetingId, Meeting updateData) {
        try {
            return meetingRepository.findById(meetingId)
                    .map(meeting -> {
                        boolean needsLinkUpdate = false;

                        // Update basic fields
                        if (updateData.getTitle() != null && !updateData.getTitle().trim().isEmpty()) {
                            meeting.setTitle(updateData.getTitle());
                            needsLinkUpdate = true;
                        }

                        if (updateData.getDescription() != null) {
                            meeting.setDescription(updateData.getDescription());
                        }

                        // Update datetime with UTC conversion
                        if (updateData.getDatetime() != null) {
                            meeting.setDatetime(updateData.getDatetime()
                                    .atZone(ZoneId.systemDefault())
                                    .withZoneSameInstant(ZoneId.of("UTC"))
                                    .toLocalDateTime());
                        }

                        if (updateData.getDuration() != null) meeting.setDuration(updateData.getDuration());
                        if (updateData.getStatus() != null) meeting.setStatus(updateData.getStatus());
                        if (updateData.getType() != null) meeting.setType(updateData.getType());
                        if (updateData.getMaxUsers() != null) meeting.setMaxUsers(updateData.getMaxUsers());

                        meeting.setUpdatedAt(getCurrentUtcTime());

                        // Update course association
                        if (updateData.getCourseId() != null && !updateData.getCourseId().equals(meeting.getCourseId())) {
                            meeting.setCourseId(updateData.getCourseId());

                            if (!updateData.getCourseId().trim().isEmpty()) {
                                Course course = courseRepository.findById(updateData.getCourseId()).orElse(null);
                                if (course != null) {
                                    meeting.setCourseName(course.getName());
                                    meeting.setCourseCode(course.getCode());
                                    meeting.setLecturerId(course.getLecturerId());
                                }
                            }

                            updateMeetingStudentsCount(meeting);
                            needsLinkUpdate = true;
                        }

                        // Update invitation link if needed
                        if (needsLinkUpdate || updateData.getInvitationLink() != null) {
                            meeting.updateInvitationLink(baseUrl);
                        }

                        Meeting savedMeeting = meetingRepository.save(meeting);

                        // Convert datetime to local time for frontend response
                        if (savedMeeting.getDatetime() != null) {
                            savedMeeting.setDatetime(convertUtcToLocal(savedMeeting.getDatetime()));
                        }

                        log.info("Meeting updated: {}", meetingId);
                        return savedMeeting;
                    })
                    .orElse(null);
        } catch (Exception e) {
            log.error("Failed to update meeting {}: {}", meetingId, e.getMessage(), e);
            throw new RuntimeException("Failed to update meeting: " + e.getMessage());
        }
    }

    @Override
    public void deleteMeeting(String meetingId) {
        try {
            meetingRepository.deleteById(meetingId);
            log.info("Meeting deleted: {}", meetingId);
        } catch (Exception e) {
            log.error("Failed to delete meeting {}: {}", meetingId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete meeting: " + e.getMessage());
        }
    }

    // ===== MEETING PARTICIPATION =====

    @Override
    public Map<String, Object> joinMeeting(String meetingId, String userId, String userName) {
        try {
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Meeting not found"));

            // Add user to participants list
            meeting.addParticipant(userId);

            // Create or get attendance session with better duplicate handling
            AttendanceSession session = meeting.addOrUpdateAttendanceSession(userId, userName);

            if (session == null || !session.isValid()) {
                throw new RuntimeException("Failed to create valid attendance session");
            }

            // Save meeting with updated session
            Meeting savedMeeting = meetingRepository.save(meeting);

            // Check if this is a resumed session
            boolean isExistingSession = session.getSessionAgeMinutes() > 1; // More than 1 minute old

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("id", session.getId());
            response.put("meetingId", meetingId);
            response.put("joinTime", session.getJoinTime());
            response.put("status", "joined");
            response.put("userName", userName);
            response.put("isExistingSession", isExistingSession);

            if (isExistingSession) {
                response.put("previousDuration", session.getCurrentDurationSeconds());
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to join meeting {} for user {}: {}", meetingId, userId, e.getMessage(), e);
            throw new RuntimeException("Failed to join meeting: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> leaveMeeting(String meetingId, String sessionId) {
        try {
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Meeting not found"));

            boolean sessionEnded = meeting.endAttendanceSessionById(sessionId);

            if (!sessionEnded) {
                // Try to find the session anyway and provide more info
                Optional<AttendanceSession> sessionOpt = meeting.getAttendanceSessions().stream()
                        .filter(session -> session.getId().equals(sessionId))
                        .findFirst();

                if (sessionOpt.isPresent()) {
                    AttendanceSession session = sessionOpt.get();
                    if (session.getLeaveTime() != null) {
                        // Session already ended, return existing info
                        Map<String, Object> response = new HashMap<>();
                        response.put("status", "already_left");
                        response.put("sessionId", sessionId);
                        response.put("leaveTime", session.getLeaveTime().toString());
                        response.put("durationMinutes", session.getDurationMinutes());
                        response.put("durationSeconds", session.getCurrentDurationSeconds());
                        return response;
                    }
                }

                throw new RuntimeException("Session not found or could not be ended");
            }

            // Save meeting with updated session
            Meeting savedMeeting = meetingRepository.save(meeting);

            // Get the ended session for response
            AttendanceSession endedSession = savedMeeting.getAttendanceSessions().stream()
                    .filter(session -> session.getId().equals(sessionId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Ended session not found"));

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("leaveTime", endedSession.getLeaveTime());
            response.put("durationMinutes", endedSession.getDurationMinutes());
            response.put("durationSeconds", endedSession.getCurrentDurationSeconds());
            response.put("status", "left");

            return response;
        } catch (Exception e) {
            log.error("Failed to leave meeting {} session {}: {}", meetingId, sessionId, e.getMessage(), e);
            throw new RuntimeException("Failed to leave meeting: " + e.getMessage());
        }
    }

    @Override
    public List<AttendanceSession> getMeetingAttendance(String meetingId) {
        try {
            Optional<Meeting> meeting = meetingRepository.findById(meetingId);
            if (meeting.isPresent() && meeting.get().getAttendanceSessions() != null) {
                List<AttendanceSession> sessions = meeting.get().getAttendanceSessions();

                // Filter out invalid sessions and sort by join time
                return sessions.stream()
                        .filter(AttendanceSession::isValid)
                        .filter(session -> session.isMeaningfulSession() || session.isActive()) // Include active even if short
                        .sorted((a, b) -> a.getJoinTime().compareTo(b.getJoinTime()))
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to get meeting attendance for {}: {}", meetingId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public Meeting startMeeting(String meetingId) {
        try {
            return meetingRepository.findById(meetingId)
                    .map(meeting -> {
                        meeting.setStatus("active");
                        meeting.setStartTime(getCurrentUtcTime());

                        Meeting savedMeeting = meetingRepository.save(meeting);

                        // Convert datetime to local time for response
                        if (savedMeeting.getDatetime() != null) {
                            savedMeeting.setDatetime(convertUtcToLocal(savedMeeting.getDatetime()));
                        }

                        log.info("Meeting started: {}", meetingId);
                        return savedMeeting;
                    })
                    .orElse(null);
        } catch (Exception e) {
            log.error("Failed to start meeting {}: {}", meetingId, e.getMessage(), e);
            throw new RuntimeException("Failed to start meeting: " + e.getMessage());
        }
    }

    @Override
    public Meeting endMeeting(String meetingId) {
        try {
            return meetingRepository.findById(meetingId)
                    .map(meeting -> {
                        meeting.setStatus("ended");
                        meeting.setEndTime(getCurrentUtcTime());

                        // End all active attendance sessions
                        meeting.endAllActiveAttendanceSessions();

                        // Clean up any invalid sessions
                        int removedSessions = meeting.removeInvalidSessions();

                        Meeting savedMeeting = meetingRepository.save(meeting);

                        // Convert datetime to local time for response
                        if (savedMeeting.getDatetime() != null) {
                            savedMeeting.setDatetime(convertUtcToLocal(savedMeeting.getDatetime()));
                        }

                        return savedMeeting;
                    })
                    .orElse(null);
        } catch (Exception e) {
            log.error("Failed to end meeting {}: {}", meetingId, e.getMessage(), e);
            throw new RuntimeException("Failed to end meeting: " + e.getMessage());
        }
    }

    // ===== COURSE MEETINGS =====

    @Override
    public List<Course> getLecturerCourses(String lecturerId) {
        try {
            return courseRepository.findByLecturerId(lecturerId);
        } catch (Exception e) {
            log.error("Failed to get lecturer courses for {}: {}", lecturerId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Course> getStudentCourses(String studentId) {
        try {
            return courseRepository.findByEnrollments_StudentIds(studentId);
        } catch (Exception e) {
            log.error("Failed to get student courses for {}: {}", studentId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Meeting> getCourseMeetings(String courseId) {
        try {
            List<Meeting> meetings = meetingRepository.findByCourseIdOrderByDatetimeDesc(courseId);
            meetings.forEach(meeting -> {
                // Convert all datetime fields to local time
                if (meeting.getDatetime() != null) {
                    meeting.setDatetime(convertUtcToLocal(meeting.getDatetime()));
                }

                if (meeting.getScheduledAt() != null) {
                    meeting.setScheduledAt(convertUtcToLocal(meeting.getScheduledAt()));
                }

                if (meeting.getStartTime() != null) {
                    meeting.setStartTime(convertUtcToLocal(meeting.getStartTime()));
                }

                if (meeting.getEndTime() != null) {
                    meeting.setEndTime(convertUtcToLocal(meeting.getEndTime()));
                }

                if (meeting.getCreatedAt() != null) {
                    meeting.setCreatedAt(convertUtcToLocal(meeting.getCreatedAt()));
                }

                if (meeting.getUpdatedAt() != null) {
                    meeting.setUpdatedAt(convertUtcToLocal(meeting.getUpdatedAt()));
                }

                updateMeetingStudentsCount(meeting);

                if (meeting.getInvitationLink() == null && meeting.getRoomId() != null) {
                    meeting.generateInvitationLink(baseUrl);
                }
            });
            return meetings;
        } catch (Exception e) {
            log.error("Failed to get course meetings for {}: {}", courseId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<UserEntity> getCourseStudents(String courseId) {
        try {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null || course.getEnrollments() == null) {
                return new ArrayList<>();
            }

            List<String> studentIds = course.getEnrollments().stream()
                    .flatMap(enrollment -> enrollment.getStudentIds().stream())
                    .distinct()
                    .collect(Collectors.toList());

            return userRepository.findAllById(studentIds);
        } catch (Exception e) {
            log.error("Failed to get course students for {}: {}", courseId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // ===== ANALYTICS =====

    @Override
    public Map<String, Object> getMeetingAnalytics(String meetingId) {
        try {
            Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
            if (meeting == null) return new HashMap<>();

            List<AttendanceSession> sessions = meeting.getAttendanceSessions() != null ?
                    meeting.getAttendanceSessions().stream()
                            .filter(AttendanceSession::isValid)
                            .filter(AttendanceSession::isMeaningfulSession)
                            .collect(Collectors.toList()) : new ArrayList<>();

            Map<String, Object> analytics = new HashMap<>();
            analytics.put("meetingId", meetingId);
            analytics.put("title", meeting.getTitle());
            analytics.put("totalParticipants", meeting.getParticipants() != null ? meeting.getParticipants().size() : 0);
            analytics.put("attendanceSessions", sessions.size());
            analytics.put("activeAttendance", meeting.getActiveAttendanceCount());
            analytics.put("averageAttendanceTime", calculateAverageAttendanceTime(sessions));
            analytics.put("attendanceRate", meeting.getAttendanceRate());
            analytics.put("uniqueAttendees", meeting.getUniqueParticipantsCount());

            return analytics;
        } catch (Exception e) {
            log.error("Failed to get meeting analytics for {}: {}", meetingId, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> getCourseAnalytics(String courseId, Map<String, String> filters) {
        try {
            List<Meeting> meetings = meetingRepository.findByCourseIdOrderByDatetimeDesc(courseId);
            Course course = courseRepository.findById(courseId).orElse(null);

            Map<String, Object> analytics = new HashMap<>();
            analytics.put("courseId", courseId);
            analytics.put("courseName", course != null ? course.getName() : "Unknown");
            analytics.put("totalMeetings", meetings.size());
            analytics.put("averageAttendance", calculateCourseAverageAttendance(meetings));
            analytics.put("meetingFrequency", calculateMeetingFrequency(meetings));

            return analytics;
        } catch (Exception e) {
            log.error("Failed to get course analytics for {}: {}", courseId, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> getLecturerAnalytics(String lecturerId, Map<String, String> filters) {
        try {
            List<Meeting> meetings = meetingRepository.findByLecturerIdOrderByDatetimeDesc(lecturerId);
            List<Course> courses = courseRepository.findByLecturerId(lecturerId);

            Map<String, Object> analytics = new HashMap<>();
            analytics.put("totalMeetings", meetings.size());
            analytics.put("totalCourses", courses.size());
            analytics.put("averageAttendanceRate", calculateOverallAttendanceRate(meetings));
            analytics.put("meetingsThisWeek", countMeetingsThisWeek(meetings));

            return analytics;
        } catch (Exception e) {
            log.error("Failed to get lecturer analytics for {}: {}", lecturerId, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> getStudentAnalytics(String studentId, Map<String, String> filters) {
        try {
            List<Meeting> meetings = meetingRepository.findByParticipantsContaining(studentId);

            Map<String, Object> analytics = new HashMap<>();
            analytics.put("totalMeetingsAttended", meetings.size());
            analytics.put("averageAttendanceTime", calculateStudentAverageAttendanceTime(meetings, studentId));
            analytics.put("attendanceRate", calculateStudentAttendanceRate(studentId));

            return analytics;
        } catch (Exception e) {
            log.error("Failed to get student analytics for {}: {}", studentId, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    // ===== ATTENDANCE TRACKING =====

    @Override
    public Map<String, Object> getAttendanceSummary(String userId, Map<String, String> filters) {
        try {
            List<Meeting> meetings = meetingRepository.findByParticipantsContaining(userId);

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalMeetings", meetings.size());
            summary.put("attendancePercentage", calculateStudentAttendanceRate(userId));
            summary.put("totalAttendanceTime", calculateTotalAttendanceTime(meetings, userId));

            return summary;
        } catch (Exception e) {
            log.error("Failed to get attendance summary for {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public List<AttendanceSession> getCourseAttendance(String courseId, Map<String, String> filters) {
        try {
            List<Meeting> meetings = meetingRepository.findByCourseIdOrderByDatetimeDesc(courseId);

            return meetings.stream()
                    .filter(meeting -> meeting.getAttendanceSessions() != null)
                    .flatMap(meeting -> meeting.getAttendanceSessions().stream())
                    .filter(AttendanceSession::isValid)
                    .filter(AttendanceSession::isMeaningfulSession)
                    .sorted((a, b) -> b.getJoinTime().compareTo(a.getJoinTime())) // Most recent first
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get course attendance for {}: {}", courseId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<AttendanceSession> getStudentCourseAttendance(String courseId, String studentId) {
        try {
            List<Meeting> meetings = meetingRepository.findByCourseIdOrderByDatetimeDesc(courseId);

            return meetings.stream()
                    .filter(meeting -> meeting.getAttendanceSessions() != null)
                    .flatMap(meeting -> meeting.getAttendanceSessions().stream())
                    .filter(session -> session.getUserId().equals(studentId))
                    .filter(AttendanceSession::isValid)
                    .filter(AttendanceSession::isMeaningfulSession)
                    .sorted((a, b) -> b.getJoinTime().compareTo(a.getJoinTime())) // Most recent first
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get student course attendance for course {} student {}: {}",
                    courseId, studentId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Recalculate all attendance durations with comprehensive validation and cleanup
     */
    public void recalculateAttendanceDurations(String meetingId) {
        try {
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Meeting not found"));

            List<AttendanceSession> sessions = meeting.getAttendanceSessions();
            if (sessions == null || sessions.isEmpty()) {
                return;
            }

            int updatedCount = 0;
            int invalidCount = 0;
            int duplicateCount = 0;

            // First pass - recalculate durations and identify issues
            for (AttendanceSession session : sessions) {
                try {
                    if (session.getJoinTime() != null && session.getLeaveTime() != null) {
                        Long originalDuration = session.getDurationMinutes();
                        session.recalculateDuration();

                        if (!Objects.equals(originalDuration, session.getDurationMinutes())) {
                            updatedCount++;
                        }
                    }

                    if (!session.isValid()) {
                        invalidCount++;
                    }
                } catch (Exception e) {
                    invalidCount++;
                }
            }

            // Second pass - remove duplicates and invalid sessions
            int originalSize = sessions.size();

            // Remove clearly invalid sessions
            sessions.removeIf(session -> !session.isValid());

            // Remove suspicious sessions (too short and not active)
            sessions.removeIf(session -> session.isSuspiciouslyShort());

            // Remove duplicate sessions for same user with overlapping times
            List<AttendanceSession> cleanedSessions = new ArrayList<>();
            for (AttendanceSession session : sessions) {
                boolean isDuplicate = cleanedSessions.stream()
                        .anyMatch(existing ->
                                existing.getUserId().equals(session.getUserId()) &&
                                        existing.overlapsWith(session) &&
                                        !existing.getId().equals(session.getId()) &&
                                        existing.getCurrentDurationSeconds() >= session.getCurrentDurationSeconds()
                        );

                if (!isDuplicate) {
                    cleanedSessions.add(session);
                } else {
                    duplicateCount++;
                }
            }

            meeting.getAttendanceSessions().clear();
            meeting.getAttendanceSessions().addAll(cleanedSessions);

            // Save the meeting
            meetingRepository.save(meeting);
        } catch (Exception e) {
            log.error("Failed to recalculate attendance durations for meeting {}: {}",
                    meetingId, e.getMessage(), e);
            throw new RuntimeException("Failed to recalculate attendance durations: " + e.getMessage());
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private List<Meeting> applyFilters(List<Meeting> meetings, Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) return meetings;

        return meetings.stream()
                .filter(meeting -> {
                    if (filters.containsKey("courseId") &&
                            !Objects.equals(filters.get("courseId"), meeting.getCourseId())) {
                        return false;
                    }
                    if (filters.containsKey("type") &&
                            !Objects.equals(filters.get("type"), meeting.getType())) {
                        return false;
                    }
                    if (filters.containsKey("status") &&
                            !Objects.equals(filters.get("status"), meeting.getStatus())) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private void updateMeetingStudentsCount(Meeting meeting) {
        if (meeting.getCourseId() != null) {
            Course course = courseRepository.findById(meeting.getCourseId()).orElse(null);
            if (course != null) {
                int studentsCount = calculateCourseStudentsCount(course);
                meeting.setStudentsCount(studentsCount);
            }
        }
    }

    private int calculateCourseStudentsCount(Course course) {
        if (course == null || course.getEnrollments() == null) {
            return 0;
        }

        return course.getEnrollments().stream()
                .mapToInt(enrollment -> enrollment.getStudentIds() != null ?
                        enrollment.getStudentIds().size() : 0)
                .sum();
    }

    private double calculateAverageAttendanceTime(List<AttendanceSession> sessions) {
        if (sessions.isEmpty()) return 0.0;

        return sessions.stream()
                .filter(session -> session.getDurationMinutes() != null && session.getDurationMinutes() > 0)
                .filter(AttendanceSession::isMeaningfulSession)
                .mapToLong(AttendanceSession::getDurationMinutes)
                .average()
                .orElse(0.0);
    }

    private double calculateCourseAverageAttendance(List<Meeting> meetings) {
        if (meetings.isEmpty()) return 0.0;

        return meetings.stream()
                .mapToDouble(Meeting::getAttendanceRate)
                .average()
                .orElse(0.0);
    }

    private int calculateMeetingFrequency(List<Meeting> meetings) {
        if (meetings.isEmpty()) return 0;

        LocalDateTime now = getCurrentLocalTime();
        LocalDateTime weekAgo = now.minusWeeks(1);

        return (int) meetings.stream()
                .filter(meeting -> meeting.getDatetime() != null &&
                        meeting.getDatetime().isAfter(weekAgo))
                .count();
    }

    private double calculateOverallAttendanceRate(List<Meeting> meetings) {
        if (meetings.isEmpty()) return 0.0;

        return meetings.stream()
                .mapToDouble(Meeting::getAttendanceRate)
                .average()
                .orElse(0.0);
    }

    private int countMeetingsThisWeek(List<Meeting> meetings) {
        LocalDateTime now = getCurrentLocalTime();
        LocalDateTime weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1);

        return (int) meetings.stream()
                .filter(meeting -> meeting.getDatetime() != null &&
                        meeting.getDatetime().isAfter(weekStart))
                .count();
    }

    private double calculateStudentAverageAttendanceTime(List<Meeting> meetings, String studentId) {
        List<AttendanceSession> studentSessions = meetings.stream()
                .filter(meeting -> meeting.getAttendanceSessions() != null)
                .flatMap(meeting -> meeting.getAttendanceSessions().stream())
                .filter(session -> session.getUserId().equals(studentId))
                .filter(AttendanceSession::isValid)
                .filter(AttendanceSession::isMeaningfulSession)
                .collect(Collectors.toList());

        return calculateAverageAttendanceTime(studentSessions);
    }

    private double calculateStudentAttendanceRate(String studentId) {
        try {
            List<Meeting> allMeetings = meetingRepository.findByParticipantsContaining(studentId);
            List<Meeting> attendedMeetings = allMeetings.stream()
                    .filter(meeting -> meeting.getAttendanceSessions() != null &&
                            meeting.getAttendanceSessions().stream()
                                    .anyMatch(session -> session.getUserId().equals(studentId) &&
                                            session.getJoinTime() != null &&
                                            session.isValid() &&
                                            session.isMeaningfulSession()))
                    .collect(Collectors.toList());

            if (allMeetings.isEmpty()) return 0.0;
            return (double) attendedMeetings.size() / allMeetings.size() * 100;
        } catch (Exception e) {
            log.error("Failed to calculate student attendance rate for {}: {}", studentId, e.getMessage());
            return 0.0;
        }
    }

    private long calculateTotalAttendanceTime(List<Meeting> meetings, String userId) {
        return meetings.stream()
                .filter(meeting -> meeting.getAttendanceSessions() != null)
                .flatMap(meeting -> meeting.getAttendanceSessions().stream())
                .filter(session -> session.getUserId().equals(userId))
                .filter(session -> session.getDurationMinutes() != null && session.isValid())
                .filter(AttendanceSession::isMeaningfulSession)
                .mapToLong(AttendanceSession::getDurationMinutes)
                .sum();
    }
}