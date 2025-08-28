package com.example.edusphere.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for assignment file operations
 */
@Data
public class AssignmentFileResponse {

    private String id;
    private String assignmentId;
    private String courseId;
    private String originalFilename;
    private Long fileSize;
    private String contentType;
    private String fileExtension;
    private String description;
    private String uploadedBy;
    private String uploaderName; // Name of the person who uploaded
    private Boolean visibleToStudents;
    private Integer downloadCount;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastAccessed;

    // Helper fields
    private String fileIcon;
    private String formattedSize;
    private String fileUrl;
    private String viewUrl;
    private Boolean canView;
    private Boolean canDownload;
    private Boolean canDelete;

    // Assignment information (optional)
    private String assignmentTitle;
    private String courseName;

    // Additional metadata
    private String uploadContext;
    private String status;

    // Constructor
    public AssignmentFileResponse() {
        this.canView = true;
        this.canDownload = true;
        this.canDelete = false;
        this.downloadCount = 0;
        this.viewCount = 0;
        this.visibleToStudents = true;
    }

    // Helper methods
    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    public boolean isPdf() {
        return "application/pdf".equals(contentType);
    }

    public boolean isViewableInBrowser() {
        if (contentType == null) return false;
        return contentType.startsWith("image/") ||
                "application/pdf".equals(contentType) ||
                "text/plain".equals(contentType) ||
                contentType.startsWith("text/");
    }

    public String getFileTypeCategory() {
        if (contentType == null) return "unknown";

        if (contentType.startsWith("image/")) return "image";
        if (contentType.startsWith("video/")) return "video";
        if (contentType.startsWith("audio/")) return "audio";
        if ("application/pdf".equals(contentType)) return "pdf";
        if (contentType.contains("document") || contentType.contains("word")) return "document";
        if (contentType.contains("spreadsheet") || contentType.contains("excel")) return "spreadsheet";
        if (contentType.contains("presentation") || contentType.contains("powerpoint")) return "presentation";
        if (contentType.contains("zip") || contentType.contains("rar")) return "archive";
        if (contentType.startsWith("text/")) return "text";

        return "other";
    }

    public boolean isLargeFile() {
        return fileSize != null && fileSize > 5 * 1024 * 1024; // > 5MB
    }

    public boolean isRecentlyUploaded() {
        if (createdAt == null) return false;
        return createdAt.isAfter(LocalDateTime.now().minusDays(7));
    }

    public boolean isPopular() {
        return downloadCount != null && downloadCount > 10;
    }
}