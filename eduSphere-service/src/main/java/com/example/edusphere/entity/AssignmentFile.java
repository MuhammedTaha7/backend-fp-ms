package com.example.edusphere.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * AssignmentFile entity for storing assignment file attachments
 */
@Data
@Document(collection = "assignment_files")
public class AssignmentFile {

    @Id
    private String id;

    @Field("assignment_id")
    private String assignmentId;

    @Field("course_id")
    private String courseId;

    @Field("original_filename")
    private String originalFilename;

    @Field("stored_filename")
    private String storedFilename; // Unique filename stored on server

    @Field("file_path")
    private String filePath; // Full path where file is stored

    @Field("file_size")
    private Long fileSize; // File size in bytes

    @Field("content_type")
    private String contentType; // MIME type

    @Field("file_extension")
    private String fileExtension;

    // File metadata
    private String description;

    @Field("file_hash")
    private String fileHash; // MD5 hash for integrity checking

    // Upload information
    @Field("uploaded_by")
    private String uploadedBy; // Lecturer who uploaded the file

    @Field("upload_context")
    private String uploadContext = "assignment_attachment"; // Context of upload

    // Access control
    @Field("is_public")
    private Boolean isPublic = true; // Assignment files are usually public to enrolled students

    @Field("visible_to_students")
    private Boolean visibleToStudents = true;

    // File status
    private String status = "active"; // "active", "deleted", "archived"

    // Usage tracking
    @Field("download_count")
    private Integer downloadCount = 0;

    @Field("last_accessed")
    private LocalDateTime lastAccessed;

    @Field("view_count")
    private Integer viewCount = 0;

    // Storage information
    @Field("storage_location")
    private String storageLocation = "local"; // "local", "cloud", etc.

    @Field("backup_status")
    private String backupStatus; // "none", "backed_up", "failed"

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isImage() {
        if (contentType == null) return false;
        return contentType.startsWith("image/");
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

    public String getFileIcon() {
        if (contentType == null) return "📄";

        if (contentType.startsWith("image/")) return "🖼️";
        if ("application/pdf".equals(contentType)) return "📕";
        if (contentType.contains("document") || contentType.contains("word")) return "📘";
        if (contentType.contains("spreadsheet") || contentType.contains("excel")) return "📊";
        if (contentType.contains("presentation") || contentType.contains("powerpoint")) return "📽️";
        if (contentType.startsWith("video/")) return "🎥";
        if (contentType.startsWith("audio/")) return "🎵";
        if (contentType.contains("zip") || contentType.contains("rar") || contentType.contains("archive")) return "📦";
        if (contentType.startsWith("text/")) return "📝";

        return "📄";
    }

    public String getFormattedSize() {
        if (fileSize == null || fileSize == 0) return "0 Bytes";

        long bytes = fileSize;
        if (bytes < 0) return "Invalid size";

        int k = 1024;
        String[] sizes = {"Bytes", "KB", "MB", "GB", "TB"};
        int i = (int) Math.floor(Math.log(bytes) / Math.log(k));

        if (i >= sizes.length) return "File too large";
        if (i < 0) return "0 Bytes";

        double size = bytes / Math.pow(k, i);
        return String.format("%.1f %s", size, sizes[i]);
    }

    public void incrementDownloadCount() {
        if (downloadCount == null) {
            downloadCount = 1;
        } else {
            downloadCount++;
        }
        lastAccessed = LocalDateTime.now();
    }

    public void incrementViewCount() {
        if (viewCount == null) {
            viewCount = 1;
        } else {
            viewCount++;
        }
        lastAccessed = LocalDateTime.now();
    }

    public boolean canBeAccessedBy(String userId, String userRole) {
        // Assignment files are generally accessible to:
        // 1. The uploader (lecturer)
        // 2. Admins
        // 3. Students enrolled in the course (if visible to students)

        // Admin can access everything
        if ("1100".equals(userRole)) {
            return true;
        }

        // Uploader can always access
        if (userId.equals(uploadedBy)) {
            return true;
        }

        // Check if file is visible to students and user is a student
        if ("1300".equals(userRole) && Boolean.TRUE.equals(visibleToStudents)) {
            // Would need to check if student is enrolled in the course
            return true;
        }

        // Lecturers can access files in courses they manage
        if ("1200".equals(userRole)) {
            // Would need to check if lecturer manages the course
            return true;
        }

        return false;
    }

    public boolean canBeDeletedBy(String userId, String userRole) {
        // Admin can delete everything
        if ("1100".equals(userRole)) {
            return true;
        }

        // Uploader can delete their files
        if (userId.equals(uploadedBy)) {
            return true;
        }

        // Course lecturer can delete assignment files
        if ("1200".equals(userRole)) {
            // Would need to check if lecturer manages the course
            return true;
        }

        return false;
    }

    public boolean isActive() {
        return "active".equals(status);
    }

    public boolean isDeleted() {
        return "deleted".equals(status);
    }

    public String getFileUrl() {
        return "/api/assignment-files/" + id + "/download";
    }

    public String getViewUrl() {
        return "/api/assignment-files/" + id + "/view";
    }
}