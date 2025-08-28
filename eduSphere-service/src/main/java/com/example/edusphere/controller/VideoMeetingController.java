package com.example.edusphere.controller;

import com.example.edusphere.entity.Course;
import com.example.edusphere.entity.Meeting;
import com.example.edusphere.entity.AttendanceSession;
import com.example.common.entity.UserEntity;
import com.example.edusphere.service.VideoMeetingService;
import com.example.edusphere.service.impl.VideoMeetingServiceImpl;
import com.example.edusphere.repository.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Comparator;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://13.49.225.86:3000"})
public class VideoMeetingController {

    private static final Logger log = LoggerFactory.getLogger(VideoMeetingController.class);

    @Autowired
    private VideoMeetingService videoMeetingService;

    @Autowired
    private VideoMeetingServiceImpl videoMeetingServiceImpl;

    @Autowired
    private MeetingRepository meetingRepository;

    @Value("${app.base.url:http://localhost:3000}")
    private String baseUrl;

    // ===== MEETING MANAGEMENT ENDPOINTS =====

    @PostMapping("/meetings")
    public ResponseEntity<Map<String, Object>> createMeeting(@RequestBody Meeting meeting, Authentication auth) {
        try {
            UserEntity user = (UserEntity) auth.getPrincipal();
            String userId = user.getId();
            String userRole = user.getRole();

            // Validate required fields
            if (meeting.getTitle() == null || meeting.getTitle().trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Meeting title is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (meeting.getRoomId() == null || meeting.getRoomId().trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Room ID is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // For lecturers, validate course assignment
            if ("1200".equals(userRole) && meeting.getCourseId() != null) {
                boolean hasAccess = videoMeetingService.getLecturerCourses(userId)
                        .stream()
                        .anyMatch(course -> course.getId().equals(meeting.getCourseId()));

                if (!hasAccess) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("message", "You don't have access to this course");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
                }
            }

            Meeting createdMeeting = videoMeetingService.createMeeting(meeting, userId);

            // Ensure invitation link is generated
            if (createdMeeting.getInvitationLink() == null) {
                createdMeeting.generateInvitationLink(baseUrl);
                createdMeeting = videoMeetingService.updateMeeting(createdMeeting.getId(), createdMeeting);
            }

            log.info("Meeting created successfully: {} by user: {}", createdMeeting.getId(), userId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", createdMeeting.getId());
            response.put("title", createdMeeting.getTitle());
            response.put("roomId", createdMeeting.getRoomId());
            response.put("invitationLink", createdMeeting.getInvitationLink());
            response.put("status", createdMeeting.getStatus());
            response.put("datetime", createdMeeting.getDatetime());
            response.put("courseId", createdMeeting.getCourseId());
            response.put("courseName", createdMeeting.getCourseName());
            response.put("courseCode", createdMeeting.getCourseCode());
            response.put("duration", createdMeeting.getDuration());
            response.put("description", createdMeeting.getDescription());
            response.put("createdBy", createdMeeting.getCreatedBy());
            response.put("createdAt", createdMeeting.getCreatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create meeting: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/meetings/user")
    public ResponseEntity<List<Meeting>> getUserMeetings(@RequestParam Map<String, String> filters, Authentication auth) {
        try {
            UserEntity user = (UserEntity) auth.getPrincipal();
            String userId = user.getId();

            List<Meeting> meetings = videoMeetingService.getUserMeetings(userId, filters);

            // Ensure all meetings have invitation links
            meetings.forEach(meeting -> {
                if (meeting.getInvitationLink() == null && meeting.getRoomId() != null) {
                    meeting.generateInvitationLink(baseUrl);
                }
            });

            log.debug("Retrieved {} meetings for user: {}", meetings.size(), userId);
            return ResponseEntity.ok(meetings);
        } catch (Exception e) {
            log.error("Failed to get user meetings: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }
    }

    @GetMapping("/meetings/{meetingId}")
    public ResponseEntity<Map<String, Object>> getMeetingById(@PathVariable String meetingId, Authentication auth) {
        try {
            UserEntity user = (UserEntity) auth.getPrincipal();
            String userId = user.getId();
            String userRole = user.getRole();

            Optional<Meeting> meetingOpt = videoMeetingService.getMeetingById(meetingId);
            if (meetingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Meeting meeting = meetingOpt.get();

            // FIXED: More permissive access control
            boolean hasAccess = false;
            if ("1200".equals(userRole)) {
                // Lecturers can access meetings they created or are assigned to teach
                hasAccess = meeting.getCreatedBy().equals(userId) ||
                        (meeting.getLecturerId() != null && meeting.getLecturerId().equals(userId));
            } else {
                // Students can access meetings for courses they're enrolled in OR instant meetings
                if (meeting.getCourseId() != null) {
                    hasAccess = videoMeetingService.getStudentCourses(userId)
                            .stream()
                            .anyMatch(course -> course.getId().equals(meeting.getCourseId()));
                } else {
                    // Allow access to instant meetings (no course restriction)
                    hasAccess = true;
                }
            }

            if (!hasAccess) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "You don't have access to this meeting");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            // Ensure invitation link exists
            if (meeting.getInvitationLink() == null && meeting.getRoomId() != null) {
                meeting.generateInvitationLink(baseUrl);
                videoMeetingService.updateMeeting(meetingId, meeting);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", meeting.getId());
            response.put("title", meeting.getTitle());
            response.put("roomId", meeting.getRoomId());
            response.put("invitationLink", meeting.getInvitationLink());
            response.put("status", meeting.getStatus());
            response.put("datetime", meeting.getDatetime());
            response.put("courseId", meeting.getCourseId());
            response.put("courseName", meeting.getCourseName());
            response.put("courseCode", meeting.getCourseCode());
            response.put("duration", meeting.getDuration());
            response.put("description", meeting.getDescription());
            response.put("createdBy", meeting.getCreatedBy());
            response.put("lecturerId", meeting.getLecturerId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get meeting by ID: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/meetings/{meetingId}/invitation")
    public ResponseEntity<Map<String, Object>> getMeetingInvitation(@PathVariable String meetingId, Authentication auth) {
        try {
            UserEntity user = (UserEntity) auth.getPrincipal();
            String userId = user.getId();
            String userRole = user.getRole();

            return videoMeetingService.getMeetingById(meetingId)
                    .map(meeting -> {
                        // FIXED: More permissive permission check for invitations
                        boolean hasPermission = false;
                        if ("1200".equals(userRole)) {
                            hasPermission = meeting.getCreatedBy().equals(userId) ||
                                    (meeting.getLecturerId() != null && meeting.getLecturerId().equals(userId));
                        } else {
                            // Students can get invitations for their enrolled courses OR instant meetings
                            if (meeting.getCourseId() != null) {
                                hasPermission = videoMeetingService.getStudentCourses(userId)
                                        .stream()
                                        .anyMatch(course -> course.getId().equals(meeting.getCourseId()));
                            } else {
                                // Allow access to instant meetings
                                hasPermission = true;
                            }
                        }

                        if (!hasPermission) {
                            Map<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("message", "Not authorized to access this meeting invitation");
                            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
                        }

                        // Generate invitation link if not exists
                        if (meeting.getInvitationLink() == null && meeting.getRoomId() != null) {
                            meeting.generateInvitationLink(baseUrl);
                            videoMeetingService.updateMeeting(meetingId, meeting);
                        }

                        Map<String, Object> response = new HashMap<>();
                        response.put("meetingId", meetingId);
                        response.put("title", meeting.getTitle());
                        response.put("invitationLink", meeting.getInvitationLink());
                        response.put("roomId", meeting.getRoomId());
                        response.put("courseCode", meeting.getCourseCode());
                        response.put("courseName", meeting.getCourseName());
                        response.put("datetime", meeting.getDatetime());
                        response.put("duration", meeting.getDuration());
                        response.put("description", meeting.getDescription());
                        response.put("createdBy", meeting.getCreatedBy());
                        response.put("lecturerId", meeting.getLecturerId());

                        log.info("Invitation link provided for meeting: {} to user: {}", meetingId, userId);
                        return ResponseEntity.ok(response);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to get meeting invitation: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/meetings/{meetingId}/join")
    public ResponseEntity<Map<String, Object>> joinMeeting(@PathVariable String meetingId,
                                                           @RequestBody Map<String, Object> joinData,
                                                           Authentication auth) {
        try {
            if (meetingId == null || meetingId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Meeting ID is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            UserEntity user = (UserEntity) auth.getPrincipal();
            String userId = user.getId();
            String userName = joinData.get("userName") != null ?
                    joinData.get("userName").toString() : user.getName();

            if (userId == null || userId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "User ID is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Validate meeting exists and user has access
            Optional<Meeting> meetingOpt = videoMeetingService.getMeetingById(meetingId);
            if (meetingOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Meeting not found");
                return ResponseEntity.notFound().build();
            }

            Meeting meeting = meetingOpt.get();

            // FIXED: More permissive access check for joining
            boolean hasAccess = false;
            String userRole = user.getRole();

            if ("1200".equals(userRole)) {
                hasAccess = meeting.getCreatedBy().equals(userId) ||
                        (meeting.getLecturerId() != null && meeting.getLecturerId().equals(userId));
            } else {
                // For students, check course enrollment OR allow instant meetings
                if (meeting.getCourseId() != null) {
                    hasAccess = videoMeetingService.getStudentCourses(userId)
                            .stream()
                            .anyMatch(course -> course.getId().equals(meeting.getCourseId()));
                } else {
                    // Allow access to instant meetings
                    hasAccess = true;
                }
            }

            if (!hasAccess) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "You don't have access to this meeting");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            log.debug("Processing join request - Meeting: {}, User: {}", meetingId, userId);

            Map<String, Object> response = videoMeetingService.joinMeeting(meetingId, userId, userName);

            log.debug("Join successful - Response: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to join meeting: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/meetings/{meetingId}/leave")
    public ResponseEntity<Map<String, Object>> leaveMeeting(@PathVariable String meetingId,
                                                            @RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");
            String reason = (String) request.get("reason");

            if (sessionId == null || sessionId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Session ID is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.debug("Processing leave request - Meeting: {}, Session: {}, Reason: {}",
                    meetingId, sessionId, reason);

            Optional<Meeting> meetingOpt = videoMeetingService.getMeetingById(meetingId);
            if (meetingOpt.isEmpty()) {
                log.warn("Meeting not found: {}", meetingId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Meeting not found");
                return ResponseEntity.notFound().build();
            }

            Meeting meeting = meetingOpt.get();

            Optional<AttendanceSession> sessionOpt = meeting.getAttendanceSessions().stream()
                    .filter(session -> session.getId().equals(sessionId))
                    .findFirst();

            if (sessionOpt.isEmpty()) {
                log.warn("Session not found: {} in meeting: {}", sessionId, meetingId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Session not found");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            AttendanceSession session = sessionOpt.get();

            if (session.getLeaveTime() != null) {
                log.debug("Session already ended: {}", sessionId);
                Map<String, Object> response = new HashMap<>();
                response.put("status", "already_left");
                response.put("sessionId", sessionId);
                response.put("leaveTime", session.getLeaveTime().toString());
                response.put("durationMinutes", session.getDurationMinutes());
                response.put("durationSeconds", session.getCurrentDurationSeconds());
                return ResponseEntity.ok(response);
            }

            try {
                meeting.endAttendanceSessionById(sessionId);
                Meeting savedMeeting = meetingRepository.save(meeting);

                AttendanceSession endedSession = savedMeeting.getAttendanceSessions().stream()
                        .filter(s -> s.getId().equals(sessionId))
                        .findFirst()
                        .orElse(session);

                log.debug("Session ended successfully: {} - Duration: {} minutes ({} seconds)",
                        sessionId, endedSession.getDurationMinutes(), endedSession.getCurrentDurationSeconds());

                Map<String, Object> response = new HashMap<>();
                response.put("status", "left");
                response.put("sessionId", sessionId);
                response.put("leaveTime", endedSession.getLeaveTime().toString());
                response.put("durationMinutes", endedSession.getDurationMinutes());
                response.put("durationSeconds", endedSession.getCurrentDurationSeconds());
                response.put("reason", reason != null ? reason : "unknown");

                return ResponseEntity.ok(response);

            } catch (Exception saveError) {
                log.error("Error saving meeting after ending session: {}", saveError.getMessage(), saveError);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to save session end");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }

        } catch (Exception e) {
            log.error("Error processing leave request for meeting {}: {}", meetingId, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to process leave request");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/meetings/{meetingId}/check-recent-session")
    public ResponseEntity<Map<String, Object>> checkRecentSession(@PathVariable String meetingId,
                                                                  @RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");

            if (userId == null || userId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "User ID is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.debug("Checking for recent session - Meeting: {}, User: {}", meetingId, userId);

            // FIXED: More reasonable recent session window (10 minutes instead of 5)
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);

            Optional<Meeting> meetingOpt = videoMeetingService.getMeetingById(meetingId);
            if (meetingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Meeting meeting = meetingOpt.get();

            // FIXED: Better logic for recent session detection
            Optional<AttendanceSession> recentSession = meeting.getAttendanceSessions().stream()
                    .filter(session -> session.getUserId().equals(userId))
                    .filter(session -> session.getLeaveTime() != null)
                    .filter(session -> session.getLeaveTime().isAfter(cutoff))
                    .filter(session -> session.isValid()) // Only valid sessions
                    .max(Comparator.comparing(AttendanceSession::getLeaveTime));

            Map<String, Object> response = new HashMap<>();
            if (recentSession.isPresent()) {
                AttendanceSession session = recentSession.get();
                // Additional check: only allow resumption if session was meaningful (>30 seconds)
                if (session.getCurrentDurationSeconds() > 30) {
                    log.debug("Found recent session that can be resumed: {}", session.getId());
                    response.put("canResume", true);
                    response.put("sessionId", session.getId());
                    response.put("lastLeft", session.getLeaveTime().toString());
                    response.put("previousDuration", session.getCurrentDurationSeconds());
                } else {
                    log.debug("Recent session too short for resumption: {}", session.getId());
                    response.put("canResume", false);
                }
            } else {
                log.debug("No recent session found for resumption");
                response.put("canResume", false);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking recent session for meeting {}: {}", meetingId, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to check recent session");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/meetings/{meetingId}/resume-session")
    public ResponseEntity<Map<String, Object>> resumeSession(@PathVariable String meetingId,
                                                             @RequestBody Map<String, String> request) {
        try {
            String sessionId = request.get("sessionId");

            if (sessionId == null || sessionId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Session ID is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.debug("Resuming session - Meeting: {}, Session: {}", meetingId, sessionId);

            Optional<Meeting> meetingOpt = videoMeetingService.getMeetingById(meetingId);
            if (meetingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Meeting meeting = meetingOpt.get();

            Optional<AttendanceSession> sessionOpt = meeting.getAttendanceSessions().stream()
                    .filter(session -> session.getId().equals(sessionId))
                    .findFirst();

            if (sessionOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Session not found");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            AttendanceSession session = sessionOpt.get();

            // FIXED: Validate session can be resumed
            if (session.getLeaveTime() == null) {
                // Session is already active
                Map<String, Object> response = new HashMap<>();
                response.put("status", "already_active");
                response.put("id", session.getId());
                response.put("joinTime", session.getJoinTime().toString());
                response.put("userName", session.getUserName());
                response.put("isExistingSession", true);
                return ResponseEntity.ok(response);
            }

            // Check if session is too old to resume (10 minutes)
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
            if (session.getLeaveTime().isBefore(cutoff)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Session too old to resume");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Resume the session
            session.setLeaveTime(null);
            session.setDurationMinutes(null);

            Meeting savedMeeting = meetingRepository.save(meeting);

            AttendanceSession resumedSession = savedMeeting.getAttendanceSessions().stream()
                    .filter(s -> s.getId().equals(sessionId))
                    .findFirst()
                    .orElse(session);

            log.debug("Session resumed successfully: {}", resumedSession.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "resumed");
            response.put("id", resumedSession.getId());
            response.put("joinTime", resumedSession.getJoinTime().toString());
            response.put("userName", resumedSession.getUserName());
            response.put("isExistingSession", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error resuming session for meeting {}: {}", meetingId, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to resume session");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/meetings/{meetingId}/active-sessions")
    public ResponseEntity<List<Map<String, Object>>> getActiveSessions(@PathVariable String meetingId) {
        try {
            log.debug("Getting active sessions for meeting: {}", meetingId);

            Optional<Meeting> meetingOpt = videoMeetingService.getMeetingById(meetingId);
            if (meetingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Meeting meeting = meetingOpt.get();

            List<Map<String, Object>> activeSessions = meeting.getAttendanceSessions().stream()
                    .filter(session -> session.isActive() && session.isValid()) // FIXED: Added validity check
                    .map(session -> {
                        Map<String, Object> sessionMap = new HashMap<>();
                        sessionMap.put("id", session.getId());
                        sessionMap.put("userId", session.getUserId());
                        sessionMap.put("userName", session.getUserName());
                        sessionMap.put("joinTime", session.getJoinTime().toString());
                        sessionMap.put("durationSeconds", session.getCurrentDurationSeconds());
                        sessionMap.put("durationMinutes", session.getCurrentDurationMinutes());
                        return sessionMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(activeSessions);

        } catch (Exception e) {
            log.error("Error getting active sessions for meeting {}: {}", meetingId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }
    }

    @GetMapping("/meetings/{meetingId}/attendance")
    public ResponseEntity<List<AttendanceSession>> getMeetingAttendance(@PathVariable String meetingId) {
        try {
            log.debug("Getting meeting attendance for: {}", meetingId);

            List<AttendanceSession> attendance = videoMeetingService.getMeetingAttendance(meetingId);

            log.debug("Retrieved {} attendance sessions for meeting: {}", attendance.size(), meetingId);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            log.error("Failed to get meeting attendance for {}: {}", meetingId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }
    }

    // FIXED: Added heartbeat endpoint
    @PostMapping("/meetings/{meetingId}/heartbeat")
    public ResponseEntity<Map<String, Object>> sendHeartbeat(@PathVariable String meetingId,
                                                             @RequestBody Map<String, String> request) {
        try {
            String sessionId = request.get("sessionId");

            if (sessionId == null || sessionId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Session ID is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.debug("Received heartbeat for meeting: {}, session: {}", meetingId, sessionId);

            Optional<Meeting> meetingOpt = videoMeetingService.getMeetingById(meetingId);
            if (meetingOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Meeting not found");
                return ResponseEntity.notFound().build();
            }

            Meeting meeting = meetingOpt.get();
            Optional<AttendanceSession> sessionOpt = meeting.getAttendanceSessions().stream()
                    .filter(session -> session.getId().equals(sessionId))
                    .filter(AttendanceSession::isActive)
                    .findFirst();

            Map<String, Object> response = new HashMap<>();
            if (sessionOpt.isPresent()) {
                response.put("status", "success");
                response.put("sessionId", sessionId);
                response.put("currentDuration", sessionOpt.get().getCurrentDurationSeconds());
            } else {
                response.put("status", "session_not_found");
                response.put("sessionId", sessionId);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Heartbeat failed for meeting {}: {}", meetingId, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "failed");
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response); // Return 200 even for failures to prevent client errors
        }
    }

    @GetMapping("/courses/lecturer")
    public ResponseEntity<List<Course>> getLecturerCourses(Authentication auth) {
        try {
            String lecturerId = ((UserEntity) auth.getPrincipal()).getId();
            List<Course> courses = videoMeetingService.getLecturerCourses(lecturerId);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            log.error("Failed to get lecturer courses: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }
    }

    @GetMapping("/courses/student")
    public ResponseEntity<List<Course>> getStudentCourses(Authentication auth) {
        try {
            String studentId = ((UserEntity) auth.getPrincipal()).getId();
            List<Course> courses = videoMeetingService.getStudentCourses(studentId);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            log.error("Failed to get student courses: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "meeting-api");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }
}