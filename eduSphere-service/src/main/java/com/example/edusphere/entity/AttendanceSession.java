package com.example.edusphere.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Data
@EqualsAndHashCode(of = {"id"}) // Use ID for equality instead of userId+meetingId to allow multiple sessions
public class AttendanceSession {

    private String id;
    private String userId;
    private String userName;
    private String meetingId;

    private LocalDateTime joinTime;
    private LocalDateTime leaveTime;
    private Long durationMinutes;

    // Default constructor
    public AttendanceSession() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    // Constructor for creating new session
    public AttendanceSession(String userId, String userName, String meetingId) {
        this();
        this.userId = userId;
        this.userName = userName;
        this.meetingId = meetingId;
        this.joinTime = getCurrentUtcTime();
    }

    /**
     * Get current UTC time
     */
    private LocalDateTime getCurrentUtcTime() {
        return ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
    }

    /**
     * Set leave time and recalculate duration
     */
    public void setLeaveTime(LocalDateTime leaveTime) {
        this.leaveTime = leaveTime;
        recalculateDuration();
    }

    /**
     * FIXED: Recalculate duration with better error handling and validation
     */
    public void recalculateDuration() {
        if (this.joinTime != null && this.leaveTime != null) {
            try {
                Duration duration = Duration.between(this.joinTime, this.leaveTime);
                long totalSeconds = duration.getSeconds();

                // FIXED: Handle negative durations more gracefully
                if (totalSeconds < 0) {
                    // Log warning but don't throw error
                    System.err.println("Warning: Negative duration detected for session " + this.id +
                            ". JoinTime: " + this.joinTime + ", LeaveTime: " + this.leaveTime);

                    // Check if times are close (within 1 hour) - might be clock skew
                    if (Math.abs(totalSeconds) < 3600) { // 1 hour
                        totalSeconds = Math.abs(totalSeconds);
                    } else {
                        // Large negative duration - set to 0
                        totalSeconds = 0;
                    }
                }

                // FIXED: More precise duration calculation
                long calculatedMinutes = totalSeconds / 60;
                // Round up if >= 30 seconds, but ensure minimum meaningful session
                if (totalSeconds % 60 >= 30) {
                    calculatedMinutes++;
                }

                // FIXED: Ensure minimum duration for very short sessions
                this.durationMinutes = Math.max(0, calculatedMinutes);

            } catch (Exception e) {
                System.err.println("Error calculating duration for session " + this.id + ": " + e.getMessage());
                this.durationMinutes = 0L;
            }
        } else {
            this.durationMinutes = null;
        }
    }

    /**
     * Check if the session is currently active (user has joined but not left)
     */
    public boolean isActive() {
        return joinTime != null && leaveTime == null;
    }

    /**
     * FIXED: End the session with better time handling
     */
    public void endSession() {
        if (this.leaveTime == null && this.joinTime != null) {
            LocalDateTime endTime = getCurrentUtcTime();

            // FIXED: Ensure leave time is not before join time
            if (endTime.isBefore(this.joinTime)) {
                // Clock skew detected - use join time + 1 second as minimum
                endTime = this.joinTime.plusSeconds(1);
            }

            this.leaveTime = endTime;
            recalculateDuration();
        }
    }

    /**
     * FIXED: Get current duration with better error handling
     */
    public long getCurrentDurationMinutes() {
        if (joinTime == null) return 0;

        try {
            LocalDateTime endTime = leaveTime != null ? leaveTime : getCurrentUtcTime();

            // FIXED: Ensure end time is not before join time
            if (endTime.isBefore(joinTime)) {
                return 0;
            }

            Duration duration = Duration.between(joinTime, endTime);
            long totalSeconds = duration.getSeconds();

            // FIXED: Handle edge cases better
            if (totalSeconds < 0) return 0;

            // Calculate minutes, round up if >= 30 seconds
            long minutes = totalSeconds / 60;
            if (totalSeconds % 60 >= 30) {
                minutes++;
            }
            return Math.max(0, minutes);
        } catch (Exception e) {
            System.err.println("Error calculating current duration for session " + this.id + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * FIXED: Get current duration in seconds with better validation
     */
    public long getCurrentDurationSeconds() {
        if (joinTime == null) return 0;

        try {
            LocalDateTime endTime = leaveTime != null ? leaveTime : getCurrentUtcTime();

            // FIXED: Ensure end time is not before join time
            if (endTime.isBefore(joinTime)) {
                return 0;
            }

            Duration duration = Duration.between(joinTime, endTime);
            return Math.max(0, duration.getSeconds());
        } catch (Exception e) {
            System.err.println("Error calculating current duration seconds for session " + this.id + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get stored duration in minutes (only available for completed sessions)
     */
    public Long getDurationMinutes() {
        return this.durationMinutes;
    }

    /**
     * Get formatted duration string for display
     */
    public String getFormattedDuration() {
        long minutes = getCurrentDurationMinutes();
        if (minutes <= 0) return "0m";

        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, remainingMinutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    /**
     * Get formatted current duration for active sessions
     */
    public String getFormattedCurrentDuration() {
        if (isActive()) {
            return getFormattedDuration() + " (ongoing)";
        } else {
            return getFormattedDuration();
        }
    }

    /**
     * FIXED: Enhanced validation for session data
     */
    public boolean isValid() {
        // Must have join time
        if (joinTime == null) return false;

        // Must have valid user info
        if (userId == null || userId.trim().isEmpty()) return false;
        if (userName == null || userName.trim().isEmpty()) return false;
        if (meetingId == null || meetingId.trim().isEmpty()) return false;

        // FIXED: If has leave time, it must be after or equal to join time
        if (leaveTime != null && leaveTime.isBefore(joinTime)) {
            return false;
        }

        // If has stored duration, it should be non-negative
        if (durationMinutes != null && durationMinutes < 0) {
            return false;
        }

        // FIXED: More lenient duration validation
        if (leaveTime != null && durationMinutes != null) {
            long calculated = getCurrentDurationMinutes();
            long stored = durationMinutes;

            // Allow 2-minute difference to account for rounding and timing issues
            if (Math.abs(calculated - stored) > 2) {
                // Auto-correct if difference is significant
                this.durationMinutes = calculated;
            }
        }

        // FIXED: Check for reasonable duration limits (not more than 24 hours)
        long currentDuration = getCurrentDurationMinutes();
        if (currentDuration > 1440) { // 24 hours
            return false;
        }

        return true;
    }

    /**
     * FIXED: Check if this is a meaningful session with more lenient threshold
     */
    public boolean isMeaningfulSession() {
        if (!isValid()) return false;

        // FIXED: Consider sessions meaningful if they are at least 30 seconds
        return getCurrentDurationSeconds() >= 30;
    }

    /**
     * Get session status for display
     */
    public String getStatus() {
        if (!isValid()) return "Invalid";
        if (isActive()) return "Active";
        if (!isMeaningfulSession()) return "Too Short";
        return "Completed";
    }

    /**
     * Get session type based on duration
     */
    public String getSessionType() {
        long minutes = getCurrentDurationMinutes();
        if (minutes < 1) return "Brief";
        if (minutes < 5) return "Short";
        if (minutes < 30) return "Medium";
        if (minutes < 60) return "Long";
        return "Extended";
    }

    /**
     * FIXED: Check if session overlaps with another session with buffer
     */
    public boolean overlapsWith(AttendanceSession other) {
        if (other == null || this.joinTime == null || other.getJoinTime() == null) {
            return false;
        }

        LocalDateTime thisStart = this.joinTime;
        LocalDateTime thisEnd = this.leaveTime != null ? this.leaveTime : getCurrentUtcTime();
        LocalDateTime otherStart = other.getJoinTime();
        LocalDateTime otherEnd = other.getLeaveTime() != null ? other.getLeaveTime() : getCurrentUtcTime();

        // FIXED: Add 30-second buffer to account for timing differences
        LocalDateTime thisStartBuffer = thisStart.minusSeconds(30);
        LocalDateTime thisEndBuffer = thisEnd.plusSeconds(30);

        return thisStartBuffer.isBefore(otherEnd) && otherStart.isBefore(thisEndBuffer);
    }

    /**
     * Create a summary of the session
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Session ").append(id, 0, Math.min(8, id.length()));
        summary.append(" - ").append(userName);
        summary.append(" (").append(getFormattedCurrentDuration()).append(")");
        if (isActive()) {
            summary.append(" - ACTIVE");
        }
        return summary.toString();
    }

    /**
     * Get time remaining if this is a timed session (for future use)
     */
    public long getTimeRemainingMinutes(int maxDurationMinutes) {
        if (!isActive()) return 0;

        long elapsed = getCurrentDurationMinutes();
        return Math.max(0, maxDurationMinutes - elapsed);
    }

    /**
     * Check if session exceeds maximum duration
     */
    public boolean exceedsMaxDuration(int maxDurationMinutes) {
        return getCurrentDurationMinutes() > maxDurationMinutes;
    }

    /**
     * FIXED: Force end session with validation
     */
    public void forceEndSession(LocalDateTime forceLeaveTime) {
        if (this.joinTime != null && forceLeaveTime != null) {
            // FIXED: Ensure forced leave time is not before join time
            if (forceLeaveTime.isBefore(this.joinTime)) {
                forceLeaveTime = this.joinTime.plusSeconds(1);
            }

            this.leaveTime = forceLeaveTime;
            recalculateDuration();
        }
    }

    /**
     * FIXED: Extend session by rejoining with validation
     */
    public void rejoinSession() {
        if (this.joinTime != null) {
            this.leaveTime = null;
            this.durationMinutes = null;
        }
    }

    /**
     * Get attendance percentage for a specific meeting duration
     */
    public double getAttendancePercentage(int meetingDurationMinutes) {
        if (meetingDurationMinutes <= 0) return 0.0;

        long attendedMinutes = getCurrentDurationMinutes();
        return Math.min(100.0, (double) attendedMinutes / meetingDurationMinutes * 100.0);
    }

    /**
     * FIXED: Check if session is suspiciously short (might be connection issue)
     */
    public boolean isSuspiciouslyShort() {
        return isValid() && !isActive() && getCurrentDurationSeconds() < 10;
    }

    /**
     * FIXED: Check if session is very recent (within last 5 minutes)
     */
    public boolean isVeryRecent() {
        if (joinTime == null) return false;

        LocalDateTime fiveMinutesAgo = getCurrentUtcTime().minusMinutes(5);
        return joinTime.isAfter(fiveMinutesAgo);
    }

    /**
     * FIXED: Get session age in minutes
     */
    public long getSessionAgeMinutes() {
        if (joinTime == null) return 0;

        LocalDateTime now = getCurrentUtcTime();
        Duration age = Duration.between(joinTime, now);
        return Math.max(0, age.toMinutes());
    }

    /**
     * FIXED: Check if this session can be safely resumed
     */
    public boolean canBeResumed() {
        if (isActive()) return false; // Already active
        if (!isValid()) return false; // Invalid session
        if (leaveTime == null) return false; // Never left

        // Only allow resumption within 10 minutes
        LocalDateTime tenMinutesAgo = getCurrentUtcTime().minusMinutes(10);
        return leaveTime.isAfter(tenMinutesAgo) && isMeaningfulSession();
    }

    /**
     * Custom toString for debugging
     */
    @Override
    public String toString() {
        return "AttendanceSession{" +
                "id='" + (id != null ? id.substring(0, Math.min(8, id.length())) : "null") + '\'' +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", meetingId='" + (meetingId != null ? meetingId.substring(0, Math.min(8, meetingId.length())) : "null") + '\'' +
                ", joinTime=" + joinTime +
                ", leaveTime=" + leaveTime +
                ", durationMinutes=" + durationMinutes +
                ", currentDuration=" + getCurrentDurationMinutes() + "m" +
                ", status='" + getStatus() + '\'' +
                ", isActive=" + isActive() +
                ", isValid=" + isValid() +
                ", isMeaningful=" + isMeaningfulSession() +
                '}';
    }

    /**
     * Create a detailed report of the session
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Attendance Session Report ===\n");
        report.append("Session ID: ").append(id).append("\n");
        report.append("User: ").append(userName).append(" (").append(userId).append(")\n");
        report.append("Meeting ID: ").append(meetingId).append("\n");
        report.append("Join Time: ").append(joinTime).append("\n");
        report.append("Leave Time: ").append(leaveTime != null ? leaveTime : "Still Active").append("\n");
        report.append("Duration: ").append(getFormattedCurrentDuration()).append("\n");
        report.append("Duration (seconds): ").append(getCurrentDurationSeconds()).append("\n");
        report.append("Status: ").append(getStatus()).append("\n");
        report.append("Session Type: ").append(getSessionType()).append("\n");
        report.append("Valid: ").append(isValid()).append("\n");
        report.append("Meaningful: ").append(isMeaningfulSession()).append("\n");
        report.append("Can Resume: ").append(canBeResumed()).append("\n");
        report.append("Session Age: ").append(getSessionAgeMinutes()).append(" minutes\n");
        return report.toString();
    }
}