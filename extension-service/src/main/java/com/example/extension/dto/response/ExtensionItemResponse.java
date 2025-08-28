package com.example.extension.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Individual item response for extension (task, meeting, or announcement)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtensionItemResponse {
    private String id;
    private String name;
    private String description;
    private String type; // "task", "meeting", or "announcement"
    private String dueDate; // ISO date string
    private String status; // "pending", "in-progress", "completed", "overdue"
    private String priority; // "urgent", "warning", "safe"
    private String course;

    // Additional fields for tasks
    private String category;
    private Integer maxPoints;
    private String fileUrl; // Also used for meeting invitation links
    private String fileName;
    private Boolean hasSubmission;
    private Integer submissionGrade;

    // Additional fields for announcements
    private String announcementType;
    private String location;
    private Boolean isImportant;

    // Additional fields for meetings
    private String roomId;
    private String invitationLink; // Direct field for invitation link (alternative to fileUrl)

    // Convenience method to get invitation link for meetings
    public String getMeetingInvitationLink() {
        if ("meeting".equals(this.type)) {
            // Return invitationLink if available, otherwise fileUrl
            if (this.invitationLink != null && !this.invitationLink.trim().isEmpty()) {
                return this.invitationLink;
            }
            return this.fileUrl;
        }
        return null;
    }

    // Convenience method to set invitation link for meetings
    public void setMeetingInvitationLink(String invitationLink) {
        if ("meeting".equals(this.type)) {
            this.invitationLink = invitationLink;
            // Also set fileUrl for backward compatibility
            this.fileUrl = invitationLink;
        }
    }

    // Check if this item has a valid meeting link
    public boolean hasMeetingLink() {
        return "meeting".equals(this.type) &&
                ((this.invitationLink != null && !this.invitationLink.trim().isEmpty()) ||
                        (this.fileUrl != null && !this.fileUrl.trim().isEmpty()));
    }
}