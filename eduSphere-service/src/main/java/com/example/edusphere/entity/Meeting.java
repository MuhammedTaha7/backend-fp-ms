package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Data
@Document(collection = "meetings")
public class Meeting {

    @Id
    private String id;

    private String roomId;
    private String title;
    private String description;
    private String type; // instant, lecture, office_hours, scheduled
    private String status; // scheduled, active, ended, cancelled

    private String courseId;
    private String courseName;
    private String courseCode;

    private String createdBy; // User ID
    private String lecturerId;

    private LocalDateTime datetime; // Main scheduled date/time field in UTC
    private LocalDateTime scheduledAt; // Keep for backwards compatibility in UTC
    private LocalDateTime startTime; // UTC
    private LocalDateTime endTime; // UTC

    private Integer duration; // in minutes
    private Integer maxUsers;

    private String invitationLink;

    private Integer studentsCount = 0;

    private List<String> participants = new ArrayList<>(); // User IDs
    private List<AttendanceSession> attendanceSessions = new ArrayList<>(); // Embedded

    @CreatedDate
    private LocalDateTime createdAt; // UTC

    @LastModifiedDate
    private LocalDateTime updatedAt; // UTC

    /**
     * Generate a complete invitation link for the meeting
     * This creates a URL that participants can use to join the meeting
     */
    public void generateInvitationLink(String baseUrl) {
        if (this.roomId != null && baseUrl != null) {
            try {
                StringBuilder linkBuilder = new StringBuilder(baseUrl);
                if (!baseUrl.endsWith("/")) linkBuilder.append("/");
                linkBuilder.append("meeting?room=").append(this.roomId);

                // Add meeting title if available
                if (this.title != null && !this.title.trim().isEmpty()) {
                    linkBuilder.append("&title=")
                            .append(URLEncoder.encode(this.title, StandardCharsets.UTF_8));
                }

                // Add course information if available
                if (this.courseId != null && !this.courseId.trim().isEmpty()) {
                    linkBuilder.append("&courseId=").append(this.courseId);
                }

                // Add meeting ID for attendance tracking
                if (this.id != null) {
                    linkBuilder.append("&meetingId=").append(this.id);
                }

                // Add course code for better identification
                if (this.courseCode != null && !this.courseCode.trim().isEmpty()) {
                    linkBuilder.append("&courseCode=")
                            .append(URLEncoder.encode(this.courseCode, StandardCharsets.UTF_8));
                }

                // Placeholder for user name - will be replaced when user joins
                linkBuilder.append("&name=").append(URLEncoder.encode("Guest User", StandardCharsets.UTF_8));

                this.invitationLink = linkBuilder.toString();
            } catch (Exception e) {
                // Fallback if URL encoding fails
                this.invitationLink = baseUrl + "/meeting?room=" + this.roomId +
                        "&meetingId=" + this.id + "&name=Guest%20User";
            }
        }
    }

    /**
     * Update the invitation link (alias for generateInvitationLink)
     */
    public void updateInvitationLink(String baseUrl) {
        generateInvitationLink(baseUrl);
    }

    /**
     * Find an attendance session for a specific user
     */
    public Optional<AttendanceSession> findAttendanceSession(String userId) {
        if (attendanceSessions == null) {
            attendanceSessions = new ArrayList<>();
            return Optional.empty();
        }

        return attendanceSessions.stream()
                .filter(session -> session.getUserId().equals(userId))
                .filter(AttendanceSession::isActive) // Only return active sessions
                .filter(AttendanceSession::isValid) // FIXED: Also check validity
                .findFirst();
    }

    /**
     * Find the most recent attendance session for a user (active or inactive)
     */
    public Optional<AttendanceSession> findMostRecentAttendanceSession(String userId) {
        if (attendanceSessions == null) {
            attendanceSessions = new ArrayList<>();
            return Optional.empty();
        }

        return attendanceSessions.stream()
                .filter(session -> session.getUserId().equals(userId))
                .filter(AttendanceSession::isValid) // FIXED: Only consider valid sessions
                .max((s1, s2) -> s1.getJoinTime().compareTo(s2.getJoinTime()));
    }

    /**
     * FIXED: Add or update attendance session for a user with better duplicate handling
     */
    public AttendanceSession addOrUpdateAttendanceSession(String userId, String userName) {
        if (attendanceSessions == null) {
            attendanceSessions = new ArrayList<>();
        }

        // FIXED: Check if user has an active AND valid session
        Optional<AttendanceSession> existingActiveSession = attendanceSessions.stream()
                .filter(session -> session.getUserId().equals(userId))
                .filter(AttendanceSession::isActive)
                .filter(AttendanceSession::isValid)
                .findFirst();

        if (existingActiveSession.isPresent()) {
            // User already has an active session
            return existingActiveSession.get();
        }

        // FIXED: Check for very recent sessions (within 2 minutes) that might be duplicates
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
        Optional<AttendanceSession> veryRecentSession = attendanceSessions.stream()
                .filter(session -> session.getUserId().equals(userId))
                .filter(session -> session.getJoinTime().isAfter(twoMinutesAgo))
                .filter(AttendanceSession::isValid)
                .max((s1, s2) -> s1.getJoinTime().compareTo(s2.getJoinTime()));

        if (veryRecentSession.isPresent()) {
            AttendanceSession recent = veryRecentSession.get();
            if (recent.getLeaveTime() != null) {
                // Resume the recent session instead of creating new one
                recent.rejoinSession();
                return recent;
            }
        }

        // Create new session
        AttendanceSession newSession = new AttendanceSession(userId, userName, this.id);
        attendanceSessions.add(newSession);
        return newSession;
    }

    /**
     * End attendance session for a specific user
     */
    public boolean endAttendanceSession(String userId) {
        if (attendanceSessions == null) return false;

        Optional<AttendanceSession> activeSession = attendanceSessions.stream()
                .filter(session -> session.getUserId().equals(userId) && session.isActive())
                .findFirst();

        if (activeSession.isPresent()) {
            activeSession.get().endSession();
            return true;
        }
        return false;
    }

    /**
     * FIXED: End attendance session by session ID with better validation
     */
    public boolean endAttendanceSessionById(String sessionId) {
        if (attendanceSessions == null || sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }

        Optional<AttendanceSession> session = attendanceSessions.stream()
                .filter(s -> s.getId().equals(sessionId))
                .findFirst();

        if (session.isPresent()) {
            AttendanceSession attendanceSession = session.get();
            if (attendanceSession.isActive() && attendanceSession.isValid()) {
                attendanceSession.endSession();
                return true;
            }
        }
        return false;
    }

    /**
     * FIXED: Get count of currently active and valid attendance sessions
     */
    public long getActiveAttendanceCount() {
        if (attendanceSessions == null) return 0;

        return attendanceSessions.stream()
                .filter(AttendanceSession::isActive)
                .filter(AttendanceSession::isValid)
                .count();
    }

    /**
     * Add a participant to the meeting
     */
    public void addParticipant(String userId) {
        if (participants == null) {
            participants = new ArrayList<>();
        }
        if (!participants.contains(userId)) {
            participants.add(userId);
        }
    }

    /**
     * Remove a participant from the meeting
     */
    public void removeParticipant(String userId) {
        if (participants != null) {
            participants.remove(userId);
        }
    }

    /**
     * Check if a user is a participant
     */
    public boolean isParticipant(String userId) {
        return participants != null && participants.contains(userId);
    }

    /**
     * Set scheduled time with backward compatibility
     */
    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
        if (this.datetime == null) {
            this.datetime = scheduledAt;
        }
    }

    /**
     * Set students count with null safety
     */
    public void setStudentsCount(Integer count) {
        this.studentsCount = count != null ? count : 0;
    }

    /**
     * FIXED: End all active attendance sessions with validation
     */
    public void endAllActiveAttendanceSessions() {
        if (attendanceSessions != null) {
            attendanceSessions.stream()
                    .filter(AttendanceSession::isActive)
                    .filter(AttendanceSession::isValid) // FIXED: Only end valid sessions
                    .forEach(AttendanceSession::endSession);
        }
    }

    /**
     * FIXED: Get total attendance time for a specific user across all valid sessions
     */
    public long getTotalAttendanceTimeForUser(String userId) {
        if (attendanceSessions == null) return 0;

        return attendanceSessions.stream()
                .filter(session -> session.getUserId().equals(userId))
                .filter(AttendanceSession::isValid) // FIXED: Only count valid sessions
                .mapToLong(AttendanceSession::getCurrentDurationMinutes)
                .sum();
    }

    /**
     * FIXED: Calculate attendance rate for the meeting with better validation
     */
    public double getAttendanceRate() {
        if (participants == null || participants.isEmpty()) return 0.0;

        if (attendanceSessions == null || attendanceSessions.isEmpty()) return 0.0;

        long attendedCount = attendanceSessions.stream()
                .filter(AttendanceSession::isValid) // FIXED: Only count valid sessions
                .filter(session -> session.getJoinTime() != null && session.isMeaningfulSession()) // FIXED: Must be meaningful
                .map(AttendanceSession::getUserId)
                .distinct()
                .count();

        return (double) attendedCount / participants.size() * 100.0;
    }

    /**
     * FIXED: Remove invalid attendance sessions with comprehensive validation
     */
    public int removeInvalidSessions() {
        if (attendanceSessions == null) return 0;

        int initialSize = attendanceSessions.size();

        // Remove sessions that are clearly invalid
        attendanceSessions.removeIf(session ->
                !session.isValid() ||
                        session.getUserId() == null ||
                        session.getUserId().trim().isEmpty() ||
                        session.getJoinTime() == null
        );

        // FIXED: Also remove duplicate sessions (same user, overlapping times)
        List<AttendanceSession> validSessions = new ArrayList<>();
        for (AttendanceSession session : attendanceSessions) {
            boolean isDuplicate = validSessions.stream()
                    .anyMatch(existing ->
                            existing.getUserId().equals(session.getUserId()) &&
                                    existing.overlapsWith(session) &&
                                    !existing.getId().equals(session.getId())
                    );

            if (!isDuplicate) {
                validSessions.add(session);
            }
        }

        attendanceSessions.clear();
        attendanceSessions.addAll(validSessions);

        return initialSize - attendanceSessions.size();
    }

    /**
     * Get meeting status display string
     */
    public String getStatusDisplay() {
        if (status == null) return "Unknown";

        switch (status.toLowerCase()) {
            case "scheduled":
                return "Scheduled";
            case "active":
                return "Active";
            case "ended":
                return "Ended";
            case "cancelled":
                return "Cancelled";
            default:
                return "Unknown";
        }
    }

    /**
     * Check if the meeting is currently active
     */
    public boolean isActive() {
        return "active".equalsIgnoreCase(status);
    }

    /**
     * Check if the meeting is scheduled for the future
     */
    public boolean isScheduled() {
        return "scheduled".equalsIgnoreCase(status);
    }

    /**
     * Check if the meeting has ended
     */
    public boolean isEnded() {
        return "ended".equalsIgnoreCase(status);
    }

    /**
     * Get formatted meeting duration
     */
    public String getFormattedDuration() {
        if (duration == null || duration <= 0) return "N/A";

        if (duration < 60) {
            return duration + " minutes";
        } else {
            int hours = duration / 60;
            int minutes = duration % 60;
            if (minutes == 0) {
                return hours + " hour" + (hours > 1 ? "s" : "");
            } else {
                return hours + "h " + minutes + "m";
            }
        }
    }

    /**
     * Get the primary display name for the meeting
     */
    public String getDisplayName() {
        if (title != null && !title.trim().isEmpty()) {
            return title;
        }

        if (courseCode != null && !courseCode.trim().isEmpty()) {
            return courseCode + " Meeting";
        }

        if (courseName != null && !courseName.trim().isEmpty()) {
            return courseName + " Meeting";
        }

        return "Meeting " + (id != null ? id.substring(0, Math.min(8, id.length())) : "");
    }

    /**
     * FIXED: Enhanced validation for meeting data
     */
    public boolean isValid() {
        return roomId != null && !roomId.trim().isEmpty() &&
                title != null && !title.trim().isEmpty() &&
                createdBy != null && !createdBy.trim().isEmpty();
    }

    /**
     * FIXED: Initialize default values with better null safety
     */
    public void initializeDefaults() {
        if (participants == null) {
            participants = new ArrayList<>();
        }
        if (attendanceSessions == null) {
            attendanceSessions = new ArrayList<>();
        }
        if (studentsCount == null) {
            studentsCount = 0;
        }
        if (status == null || status.trim().isEmpty()) {
            status = "scheduled";
        }
        if (maxUsers == null) {
            maxUsers = 100; // Default max users
        }
        if (duration == null) {
            duration = 60; // Default 1 hour
        }
        if (type == null || type.trim().isEmpty()) {
            type = "scheduled"; // Default type
        }
    }

    /**
     * FIXED: Get unique participants count based on actual attendance
     */
    public int getUniqueParticipantsCount() {
        if (attendanceSessions == null) return 0;

        return (int) attendanceSessions.stream()
                .filter(AttendanceSession::isValid)
                .map(AttendanceSession::getUserId)
                .distinct()
                .count();
    }

    /**
     * FIXED: Get sessions for a specific user, sorted by join time
     */
    public List<AttendanceSession> getSessionsForUser(String userId) {
        if (attendanceSessions == null || userId == null) {
            return new ArrayList<>();
        }

        return attendanceSessions.stream()
                .filter(session -> userId.equals(session.getUserId()))
                .filter(AttendanceSession::isValid)
                .sorted((s1, s2) -> s1.getJoinTime().compareTo(s2.getJoinTime()))
                .collect(ArrayList::new, (list, session) -> list.add(session), ArrayList::addAll);
    }

    /**
     * Custom toString for better debugging
     */
    @Override
    public String toString() {
        return "Meeting{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", roomId='" + roomId + '\'' +
                ", status='" + status + '\'' +
                ", courseCode='" + courseCode + '\'' +
                ", datetime=" + datetime +
                ", participants=" + (participants != null ? participants.size() : 0) +
                ", attendanceSessions=" + (attendanceSessions != null ? attendanceSessions.size() : 0) +
                ", activeAttendance=" + getActiveAttendanceCount() +
                '}';
    }
}