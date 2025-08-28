package com.example.edusphere.service;

import com.example.edusphere.dto.response.AssignmentFileResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for assignment file operations
 */
public interface AssignmentFileService {

    /**
     * Upload a file for an assignment
     * @param file The file to upload
     * @param assignmentId The assignment ID
     * @param courseId The course ID
     * @param uploadedBy The user ID uploading the file
     * @param description Optional description for the file
     * @return AssignmentFileResponse with file information
     */
    AssignmentFileResponse uploadAssignmentFile(MultipartFile file, String assignmentId, String courseId,
                                                String uploadedBy, String description);

    /**
     * Get assignment file information by ID
     * @param fileId The file ID
     * @return AssignmentFileResponse with file information
     */
    AssignmentFileResponse getAssignmentFileInfo(String fileId);

    /**
     * Get assignment file as Resource for download/view
     * @param fileId The file ID
     * @return Resource containing the file data
     */
    Resource getAssignmentFileAsResource(String fileId);

    /**
     * Delete an assignment file
     * @param fileId The file ID to delete
     * @param userId The user requesting deletion
     * @param userRole The user's role
     */
    void deleteAssignmentFile(String fileId, String userId, String userRole);

    /**
     * Get all files for a specific assignment
     * @param assignmentId The assignment ID
     * @param userId The requesting user ID
     * @param userRole The requesting user role
     * @return List of assignment files
     */
    List<AssignmentFileResponse> getFilesByAssignment(String assignmentId, String userId, String userRole);

    /**
     * Get all files for a specific course
     * @param courseId The course ID
     * @param userId The requesting user ID
     * @param userRole The requesting user role
     * @return List of assignment files for the course
     */
    List<AssignmentFileResponse> getFilesByCourse(String courseId, String userId, String userRole);

    /**
     * Get files uploaded by a specific user
     * @param uploadedBy The user ID
     * @param courseId Optional course ID filter
     * @param userRole The requesting user's role
     * @return List of files uploaded by the user
     */
    List<AssignmentFileResponse> getFilesByUploader(String uploadedBy, String courseId, String userRole);

    /**
     * Check if user can access an assignment file
     * @param fileId The file ID
     * @param userId The user ID
     * @param userRole The user role
     * @return true if user can access the file
     */
    boolean canUserAccessFile(String fileId, String userId, String userRole);

    /**
     * Check if user can delete an assignment file
     * @param fileId The file ID
     * @param userId The user ID
     * @param userRole The user role
     * @return true if user can delete the file
     */
    boolean canUserDeleteFile(String fileId, String userId, String userRole);

    /**
     * Update assignment file metadata
     * @param fileId The file ID
     * @param description New description
     * @param visibleToStudents Whether file should be visible to students
     * @param userId The user making the update
     * @param userRole The user's role
     * @return Updated AssignmentFileResponse
     */
    AssignmentFileResponse updateAssignmentFileMetadata(String fileId, String description,
                                                        Boolean visibleToStudents, String userId, String userRole);

    /**
     * Get content type for a file
     * @param fileName The file name
     * @return Content type string
     */
    String getContentType(String fileName);

    /**
     * Validate file before upload
     * @param file The file to validate
     */
    void validateAssignmentFile(MultipartFile file);

    /**
     * Get file statistics for an assignment
     * @param assignmentId The assignment ID
     * @return Assignment file statistics
     */
    AssignmentFileStats getAssignmentFileStats(String assignmentId);

    /**
     * Get file statistics for a course
     * @param courseId The course ID
     * @return Course file statistics
     */
    CourseFileStats getCourseFileStats(String courseId);

    /**
     * Delete all files for an assignment (when assignment is deleted)
     * @param assignmentId The assignment ID
     * @param userId The user requesting deletion
     * @param userRole The user's role
     */
    void deleteAllFilesForAssignment(String assignmentId, String userId, String userRole);

    /**
     * Check if assignment has any files
     * @param assignmentId The assignment ID
     * @return true if assignment has files
     */
    boolean assignmentHasFiles(String assignmentId);

    /**
     * Clean up orphaned files (files without valid assignments)
     * @return Number of files cleaned up
     */
    int cleanupOrphanedFiles();

    /**
     * Assignment file statistics class
     */
    class AssignmentFileStats {
        private String assignmentId;
        private long totalFiles;
        private long totalSize;
        private long totalSizeInMB;
        private String formattedTotalSize;
        private int totalDownloads;
        private int totalViews;
        private String mostDownloadedFile;
        private String largestFile;

        public AssignmentFileStats(String assignmentId, long totalFiles, long totalSize,
                                   int totalDownloads, int totalViews) {
            this.assignmentId = assignmentId;
            this.totalFiles = totalFiles;
            this.totalSize = totalSize;
            this.totalSizeInMB = totalSize / (1024 * 1024);
            this.formattedTotalSize = formatSize(totalSize);
            this.totalDownloads = totalDownloads;
            this.totalViews = totalViews;
        }

        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp - 1) + "";
            return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
        }

        // Getters
        public String getAssignmentId() { return assignmentId; }
        public long getTotalFiles() { return totalFiles; }
        public long getTotalSize() { return totalSize; }
        public long getTotalSizeInMB() { return totalSizeInMB; }
        public String getFormattedTotalSize() { return formattedTotalSize; }
        public int getTotalDownloads() { return totalDownloads; }
        public int getTotalViews() { return totalViews; }
        public String getMostDownloadedFile() { return mostDownloadedFile; }
        public String getLargestFile() { return largestFile; }

        public void setMostDownloadedFile(String mostDownloadedFile) { this.mostDownloadedFile = mostDownloadedFile; }
        public void setLargestFile(String largestFile) { this.largestFile = largestFile; }
    }

    /**
     * Course file statistics class
     */
    class CourseFileStats {
        private String courseId;
        private long totalFiles;
        private long totalSize;
        private long totalSizeInMB;
        private String formattedTotalSize;
        private int totalDownloads;
        private int totalViews;
        private int totalAssignmentsWithFiles;

        public CourseFileStats(String courseId, long totalFiles, long totalSize,
                               int totalDownloads, int totalViews, int totalAssignmentsWithFiles) {
            this.courseId = courseId;
            this.totalFiles = totalFiles;
            this.totalSize = totalSize;
            this.totalSizeInMB = totalSize / (1024 * 1024);
            this.formattedTotalSize = formatSize(totalSize);
            this.totalDownloads = totalDownloads;
            this.totalViews = totalViews;
            this.totalAssignmentsWithFiles = totalAssignmentsWithFiles;
        }

        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp - 1) + "";
            return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
        }

        // Getters
        public String getCourseId() { return courseId; }
        public long getTotalFiles() { return totalFiles; }
        public long getTotalSize() { return totalSize; }
        public long getTotalSizeInMB() { return totalSizeInMB; }
        public String getFormattedTotalSize() { return formattedTotalSize; }
        public int getTotalDownloads() { return totalDownloads; }
        public int getTotalViews() { return totalViews; }
        public int getTotalAssignmentsWithFiles() { return totalAssignmentsWithFiles; }
    }
}