package com.example.edusphere.service.impl;

import com.example.edusphere.dto.response.AssignmentFileResponse;
import com.example.edusphere.entity.AssignmentFile;
import com.example.edusphere.entity.Course;
import com.example.edusphere.entity.Task;
import com.example.edusphere.repository.AssignmentFileRepository;
import com.example.edusphere.repository.CourseRepository;
import com.example.edusphere.repository.TaskRepository;
import com.example.edusphere.service.AssignmentFileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssignmentFileServiceImpl implements AssignmentFileService {

    private final AssignmentFileRepository assignmentFileRepository;
    private final CourseRepository courseRepository;
    private final TaskRepository taskRepository;

    // File storage configuration
    @Value("${app.file-storage.upload-dir:./uploads/assignments}")
    private String uploadDir;

    @Value("${app.file-storage.max-file-size:10485760}") // 10MB
    private long maxFileSize;

    @Value("${app.file-storage.allowed-extensions:pdf,doc,docx,txt,zip,jpg,jpeg,png,gif,ppt,pptx,xls,xlsx}")
    private String allowedExtensions;

    public AssignmentFileServiceImpl(AssignmentFileRepository assignmentFileRepository,
                                     CourseRepository courseRepository,
                                     TaskRepository taskRepository) {
        this.assignmentFileRepository = assignmentFileRepository;
        this.courseRepository = courseRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public AssignmentFileResponse uploadAssignmentFile(MultipartFile file, String assignmentId, String courseId,
                                                       String uploadedBy, String description) {
        try {
            System.out.println("📁 === UPLOADING ASSIGNMENT FILE ===");
            System.out.println("Original filename: " + file.getOriginalFilename());
            System.out.println("Size: " + file.getSize());
            System.out.println("Assignment ID: " + assignmentId);
            System.out.println("Course ID: " + courseId);
            System.out.println("Uploaded by: " + uploadedBy);

            // Validate file
            validateAssignmentFile(file);

            // Verify assignment exists
            Task assignment = taskRepository.findById(assignmentId)
                    .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));

            // Verify course exists
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

            // Check if user can upload to this assignment
            if (!canUserUploadToAssignment(assignmentId, uploadedBy)) {
                throw new RuntimeException("Access denied: You don't have permission to upload files to this assignment");
            }

            // Generate unique file name
            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = getFileExtension(originalFileName);
            String storedFileName = generateUniqueFileName(originalFileName);

            // Create upload directory structure
            Path uploadPath = createUploadPath(courseId, assignmentId);
            Files.createDirectories(uploadPath);

            // Full file path
            Path filePath = uploadPath.resolve(storedFileName);

            // Copy file to target location
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Calculate file hash for integrity
            String fileHash = calculateFileHash(file);

            // Create assignment file entity
            AssignmentFile assignmentFile = new AssignmentFile();
            assignmentFile.setAssignmentId(assignmentId);
            assignmentFile.setCourseId(courseId);
            assignmentFile.setOriginalFilename(originalFileName);
            assignmentFile.setStoredFilename(storedFileName);
            assignmentFile.setFilePath(filePath.toString());
            assignmentFile.setFileSize(file.getSize());
            assignmentFile.setContentType(file.getContentType());
            assignmentFile.setFileExtension(fileExtension);
            assignmentFile.setDescription(description);
            assignmentFile.setFileHash(fileHash);
            assignmentFile.setUploadedBy(uploadedBy);
            assignmentFile.setUploadContext("assignment_attachment");
            assignmentFile.setIsPublic(true);
            assignmentFile.setVisibleToStudents(true);
            assignmentFile.setStatus("active");
            assignmentFile.setStorageLocation("local");
            assignmentFile.setDownloadCount(0);
            assignmentFile.setViewCount(0);

            // Save to database
            AssignmentFile savedFile = assignmentFileRepository.save(assignmentFile);

            // Update assignment with file information
            updateAssignmentFileInfo(assignmentId, savedFile);

            System.out.println("✅ Assignment file uploaded successfully with ID: " + savedFile.getId());
            System.out.println("✅ File stored at: " + filePath.toString());

            return convertToResponse(savedFile);

        } catch (IOException e) {
            System.err.println("❌ IO error uploading assignment file: " + e.getMessage());
            throw new RuntimeException("Failed to store assignment file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Unexpected error uploading assignment file: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to upload assignment file: " + e.getMessage());
        }
    }

    @Override
    public AssignmentFileResponse getAssignmentFileInfo(String fileId) {
        try {
            AssignmentFile assignmentFile = assignmentFileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("Assignment file not found: " + fileId));

            if (!"active".equals(assignmentFile.getStatus())) {
                throw new RuntimeException("Assignment file is not available: " + fileId);
            }

            return convertToResponse(assignmentFile);

        } catch (Exception e) {
            System.err.println("❌ Error getting assignment file info: " + e.getMessage());
            throw new RuntimeException("Failed to get assignment file information: " + e.getMessage());
        }
    }

    @Override
    public Resource getAssignmentFileAsResource(String fileId) {
        try {
            AssignmentFile assignmentFile = assignmentFileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("Assignment file not found: " + fileId));

            if (!"active".equals(assignmentFile.getStatus())) {
                throw new RuntimeException("Assignment file is not available: " + fileId);
            }

            Path filePath = Paths.get(assignmentFile.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Update download count and last accessed
                assignmentFile.incrementDownloadCount();
                assignmentFileRepository.save(assignmentFile);

                System.out.println("✅ Assignment file resource loaded: " + assignmentFile.getOriginalFilename());
                return resource;
            } else {
                throw new RuntimeException("Assignment file not found or not readable: " + assignmentFile.getOriginalFilename());
            }

        } catch (MalformedURLException e) {
            System.err.println("❌ Malformed URL for assignment file: " + e.getMessage());
            throw new RuntimeException("Failed to load assignment file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error loading assignment file resource: " + e.getMessage());
            throw new RuntimeException("Failed to load assignment file: " + e.getMessage());
        }
    }

    @Override
    public void deleteAssignmentFile(String fileId, String userId, String userRole) {
        try {
            System.out.println("🗑️ === DELETING ASSIGNMENT FILE ===");
            System.out.println("File ID: " + fileId);
            System.out.println("User ID: " + userId);
            System.out.println("User Role: " + userRole);

            AssignmentFile assignmentFile = assignmentFileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("Assignment file not found: " + fileId));

            // Check permission
            if (!canUserDeleteFile(fileId, userId, userRole)) {
                throw new RuntimeException("Access denied: You don't have permission to delete this file");
            }

            // Delete physical file
            Path filePath = Paths.get(assignmentFile.getFilePath());
            try {
                Files.deleteIfExists(filePath);
                System.out.println("✅ Physical assignment file deleted: " + filePath.toString());
            } catch (IOException e) {
                System.err.println("⚠️ Could not delete physical assignment file: " + e.getMessage());
                // Continue with database deletion
            }

            // Mark as deleted in database (soft delete)
            assignmentFile.setStatus("deleted");
            assignmentFile.setUpdatedAt(LocalDateTime.now());
            assignmentFileRepository.save(assignmentFile);

            // Update assignment to remove file reference
            removeFileFromAssignment(assignmentFile.getAssignmentId(), fileId);

            System.out.println("✅ Assignment file marked as deleted in database");

        } catch (Exception e) {
            System.err.println("❌ Error deleting assignment file: " + e.getMessage());
            throw new RuntimeException("Failed to delete assignment file: " + e.getMessage());
        }
    }

    @Override
    public List<AssignmentFileResponse> getFilesByAssignment(String assignmentId, String userId, String userRole) {
        try {
            System.out.println("📋 === GETTING FILES BY ASSIGNMENT ===");
            System.out.println("Assignment ID: " + assignmentId);
            System.out.println("User ID: " + userId);
            System.out.println("User Role: " + userRole);

            // Verify assignment exists and user has access
            Task assignment = taskRepository.findById(assignmentId)
                    .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));

            if (!canUserAccessAssignment(assignmentId, userId, userRole)) {
                throw new RuntimeException("Access denied: You don't have permission to view files for this assignment");
            }

            List<AssignmentFile> assignmentFiles;

            // Students can only see files marked as visible to students
            if ("1300".equals(userRole)) {
                assignmentFiles = assignmentFileRepository.findByAssignmentIdAndVisibleToStudentsAndStatus(assignmentId, true, "active");
            } else {
                // Lecturers and admins can see all files
                assignmentFiles = assignmentFileRepository.findByAssignmentIdAndStatus(assignmentId, "active");
            }

            List<AssignmentFileResponse> responses = assignmentFiles.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            System.out.println("✅ Found " + responses.size() + " files for assignment");
            return responses;

        } catch (Exception e) {
            System.err.println("❌ Error getting files by assignment: " + e.getMessage());
            throw new RuntimeException("Failed to get assignment files: " + e.getMessage());
        }
    }

    @Override
    public List<AssignmentFileResponse> getFilesByCourse(String courseId, String userId, String userRole) {
        try {
            System.out.println("📚 === GETTING FILES BY COURSE ===");
            System.out.println("Course ID: " + courseId);
            System.out.println("User ID: " + userId);
            System.out.println("User Role: " + userRole);

            // Verify course exists and user has access
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

            if (!canUserAccessCourse(courseId, userId, userRole)) {
                throw new RuntimeException("Access denied: You don't have permission to view files for this course");
            }

            List<AssignmentFile> assignmentFiles = assignmentFileRepository.findByCourseIdAndStatus(courseId, "active");

            // Filter based on user role and visibility
            List<AssignmentFile> filteredFiles = assignmentFiles.stream()
                    .filter(file -> {
                        if ("1300".equals(userRole)) {
                            // Students can only see files visible to students
                            return Boolean.TRUE.equals(file.getVisibleToStudents());
                        }
                        return true; // Lecturers and admins can see all files
                    })
                    .collect(Collectors.toList());

            List<AssignmentFileResponse> responses = filteredFiles.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            System.out.println("✅ Found " + responses.size() + " files for course");
            return responses;

        } catch (Exception e) {
            System.err.println("❌ Error getting files by course: " + e.getMessage());
            throw new RuntimeException("Failed to get course files: " + e.getMessage());
        }
    }

    @Override
    public List<AssignmentFileResponse> getFilesByUploader(String uploadedBy, String courseId, String userRole) {
        try {
            List<AssignmentFile> assignmentFiles;

            if (courseId != null) {
                // Filter by course
                assignmentFiles = assignmentFileRepository.findByCourseIdAndStatus(courseId, "active").stream()
                        .filter(file -> uploadedBy.equals(file.getUploadedBy()))
                        .collect(Collectors.toList());
            } else {
                assignmentFiles = assignmentFileRepository.findByUploadedByAndStatus(uploadedBy, "active");
            }

            // Additional filtering based on user role if needed
            if ("1300".equals(userRole)) {
                // Students can only see their own files that are visible to students
                assignmentFiles = assignmentFiles.stream()
                        .filter(file -> Boolean.TRUE.equals(file.getVisibleToStudents()))
                        .collect(Collectors.toList());
            }

            return assignmentFiles.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("❌ Error getting files by uploader: " + e.getMessage());
            throw new RuntimeException("Failed to get files by uploader: " + e.getMessage());
        }
    }

    @Override
    public boolean canUserAccessFile(String fileId, String userId, String userRole) {
        try {
            AssignmentFile assignmentFile = assignmentFileRepository.findById(fileId).orElse(null);
            if (assignmentFile == null || !"active".equals(assignmentFile.getStatus())) {
                return false;
            }

            // Check basic file permissions
            if (assignmentFile.canBeAccessedBy(userId, userRole)) {
                return true;
            }

            // Additional checks based on course enrollment for students
            if ("1300".equals(userRole)) {
                return isStudentEnrolledInCourse(userId, assignmentFile.getCourseId()) &&
                        Boolean.TRUE.equals(assignmentFile.getVisibleToStudents());
            }

            // Additional checks for lecturers
            if ("1200".equals(userRole)) {
                return isLecturerOfCourse(userId, assignmentFile.getCourseId());
            }

            return false;

        } catch (Exception e) {
            System.err.println("❌ Error checking file access: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean canUserDeleteFile(String fileId, String userId, String userRole) {
        try {
            AssignmentFile assignmentFile = assignmentFileRepository.findById(fileId).orElse(null);
            if (assignmentFile == null) {
                return false;
            }

            // Check basic deletion permissions
            if (assignmentFile.canBeDeletedBy(userId, userRole)) {
                return true;
            }

            // Additional checks for lecturers managing the course
            if ("1200".equals(userRole)) {
                return isLecturerOfCourse(userId, assignmentFile.getCourseId());
            }

            return false;

        } catch (Exception e) {
            System.err.println("❌ Error checking file deletion permission: " + e.getMessage());
            return false;
        }
    }

    @Override
    public AssignmentFileResponse updateAssignmentFileMetadata(String fileId, String description,
                                                               Boolean visibleToStudents, String userId, String userRole) {
        try {
            System.out.println("📝 === UPDATING ASSIGNMENT FILE METADATA ===");
            System.out.println("File ID: " + fileId);

            AssignmentFile assignmentFile = assignmentFileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("Assignment file not found: " + fileId));

            // Check permission to update
            if (!canUserDeleteFile(fileId, userId, userRole)) { // Using delete permission as proxy for modify
                throw new RuntimeException("Access denied: You don't have permission to modify this file");
            }

            // Update metadata
            if (description != null) {
                assignmentFile.setDescription(description);
            }
            if (visibleToStudents != null) {
                assignmentFile.setVisibleToStudents(visibleToStudents);
            }

            assignmentFile.setUpdatedAt(LocalDateTime.now());
            AssignmentFile updatedFile = assignmentFileRepository.save(assignmentFile);

            System.out.println("✅ Assignment file metadata updated successfully");
            return convertToResponse(updatedFile);

        } catch (Exception e) {
            System.err.println("❌ Error updating assignment file metadata: " + e.getMessage());
            throw new RuntimeException("Failed to update assignment file metadata: " + e.getMessage());
        }
    }

    @Override
    public String getContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";

        String extension = getFileExtension(fileName).toLowerCase();

        Map<String, String> contentTypes = new HashMap<>();
        contentTypes.put("pdf", "application/pdf");
        contentTypes.put("doc", "application/msword");
        contentTypes.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        contentTypes.put("txt", "text/plain");
        contentTypes.put("zip", "application/zip");
        contentTypes.put("jpg", "image/jpeg");
        contentTypes.put("jpeg", "image/jpeg");
        contentTypes.put("png", "image/png");
        contentTypes.put("gif", "image/gif");
        contentTypes.put("ppt", "application/vnd.ms-powerpoint");
        contentTypes.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        contentTypes.put("xls", "application/vnd.ms-excel");
        contentTypes.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return contentTypes.getOrDefault(extension, "application/octet-stream");
    }

    @Override
    public void validateAssignmentFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("No file provided");
        }

        // Check file size
        if (file.getSize() > maxFileSize) {
            long maxSizeMB = maxFileSize / (1024 * 1024);
            long fileSizeMB = file.getSize() / (1024 * 1024);
            throw new RuntimeException(String.format("File size exceeds %dMB limit (file size: %dMB)", maxSizeMB, fileSizeMB));
        }

        // Check file extension
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new RuntimeException("Invalid file name");
        }

        String fileExtension = getFileExtension(fileName).toLowerCase();
        List<String> allowedExtList = Arrays.asList(allowedExtensions.split(","));

        if (!allowedExtList.contains(fileExtension)) {
            throw new RuntimeException(String.format("File type '%s' not allowed. Allowed types: %s",
                    fileExtension, allowedExtensions));
        }

        // Additional validations can be added here
        System.out.println("✅ Assignment file validation passed for: " + fileName);
    }

    @Override
    public AssignmentFileStats getAssignmentFileStats(String assignmentId) {
        try {
            List<AssignmentFile> files = assignmentFileRepository.findByAssignmentIdAndStatus(assignmentId, "active");

            long totalSize = files.stream().mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0).sum();
            int totalDownloads = files.stream().mapToInt(f -> f.getDownloadCount() != null ? f.getDownloadCount() : 0).sum();
            int totalViews = files.stream().mapToInt(f -> f.getViewCount() != null ? f.getViewCount() : 0).sum();

            AssignmentFileStats stats = new AssignmentFileStats(assignmentId, files.size(), totalSize, totalDownloads, totalViews);

            // Find most downloaded file
            files.stream()
                    .max(Comparator.comparingInt(f -> f.getDownloadCount() != null ? f.getDownloadCount() : 0))
                    .ifPresent(file -> stats.setMostDownloadedFile(file.getOriginalFilename()));

            // Find largest file
            files.stream()
                    .max(Comparator.comparingLong(f -> f.getFileSize() != null ? f.getFileSize() : 0))
                    .ifPresent(file -> stats.setLargestFile(file.getOriginalFilename()));

            return stats;

        } catch (Exception e) {
            System.err.println("❌ Error getting assignment file stats: " + e.getMessage());
            throw new RuntimeException("Failed to get assignment file statistics: " + e.getMessage());
        }
    }

    @Override
    public CourseFileStats getCourseFileStats(String courseId) {
        try {
            List<AssignmentFile> files = assignmentFileRepository.findByCourseIdAndStatus(courseId, "active");

            long totalSize = files.stream().mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0).sum();
            int totalDownloads = files.stream().mapToInt(f -> f.getDownloadCount() != null ? f.getDownloadCount() : 0).sum();
            int totalViews = files.stream().mapToInt(f -> f.getViewCount() != null ? f.getViewCount() : 0).sum();

            // Count unique assignments with files
            int totalAssignmentsWithFiles = (int) files.stream()
                    .map(AssignmentFile::getAssignmentId)
                    .distinct()
                    .count();

            return new CourseFileStats(courseId, files.size(), totalSize, totalDownloads, totalViews, totalAssignmentsWithFiles);

        } catch (Exception e) {
            System.err.println("❌ Error getting course file stats: " + e.getMessage());
            throw new RuntimeException("Failed to get course file statistics: " + e.getMessage());
        }
    }

    @Override
    public void deleteAllFilesForAssignment(String assignmentId, String userId, String userRole) {
        try {
            System.out.println("🗑️ === DELETING ALL FILES FOR ASSIGNMENT ===");
            System.out.println("Assignment ID: " + assignmentId);

            List<AssignmentFile> files = assignmentFileRepository.findByAssignmentIdAndStatus(assignmentId, "active");

            for (AssignmentFile file : files) {
                if (canUserDeleteFile(file.getId(), userId, userRole)) {
                    deleteAssignmentFile(file.getId(), userId, userRole);
                }
            }

            System.out.println("✅ All files deleted for assignment: " + assignmentId);

        } catch (Exception e) {
            System.err.println("❌ Error deleting all files for assignment: " + e.getMessage());
            throw new RuntimeException("Failed to delete all files for assignment: " + e.getMessage());
        }
    }

    @Override
    public boolean assignmentHasFiles(String assignmentId) {
        return assignmentFileRepository.existsByAssignmentIdAndStatus(assignmentId, "active");
    }

    @Override
    public int cleanupOrphanedFiles() {
        try {
            System.out.println("🧹 === CLEANING UP ORPHANED FILES ===");

            List<AssignmentFile> allFiles = assignmentFileRepository.findByStatusOrderByCreatedAtDesc("active");
            int cleanedCount = 0;

            for (AssignmentFile file : allFiles) {
                // Check if assignment still exists
                if (!taskRepository.existsById(file.getAssignmentId())) {
                    System.out.println("🗑️ Cleaning up orphaned file: " + file.getOriginalFilename());
                    file.setStatus("deleted");
                    assignmentFileRepository.save(file);
                    cleanedCount++;
                }
            }

            System.out.println("✅ Cleaned up " + cleanedCount + " orphaned files");
            return cleanedCount;

        } catch (Exception e) {
            System.err.println("❌ Error cleaning up orphaned files: " + e.getMessage());
            return 0;
        }
    }

    // Helper methods
    private AssignmentFileResponse convertToResponse(AssignmentFile assignmentFile) {
        AssignmentFileResponse response = new AssignmentFileResponse();
        response.setId(assignmentFile.getId());
        response.setAssignmentId(assignmentFile.getAssignmentId());
        response.setCourseId(assignmentFile.getCourseId());
        response.setOriginalFilename(assignmentFile.getOriginalFilename());
        response.setFileSize(assignmentFile.getFileSize());
        response.setContentType(assignmentFile.getContentType());
        response.setFileExtension(assignmentFile.getFileExtension());
        response.setDescription(assignmentFile.getDescription());
        response.setUploadedBy(assignmentFile.getUploadedBy());
        response.setVisibleToStudents(assignmentFile.getVisibleToStudents());
        response.setDownloadCount(assignmentFile.getDownloadCount());
        response.setViewCount(assignmentFile.getViewCount());
        response.setCreatedAt(assignmentFile.getCreatedAt());
        response.setUpdatedAt(assignmentFile.getUpdatedAt());
        response.setFileIcon(assignmentFile.getFileIcon());
        response.setFormattedSize(assignmentFile.getFormattedSize());
        response.setFileUrl("/api/assignment-files/" + assignmentFile.getId() + "/download");
        response.setViewUrl("/api/assignment-files/" + assignmentFile.getId() + "/view");
        response.setCanView(assignmentFile.isViewableInBrowser());

        return response;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private String generateUniqueFileName(String originalFileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomString = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(originalFileName);
        String nameWithoutExtension = originalFileName.substring(0, originalFileName.lastIndexOf("."));

        return String.format("%s_%s_%s.%s", nameWithoutExtension, timestamp, randomString, extension);
    }

    private Path createUploadPath(String courseId, String assignmentId) {
        return Paths.get(uploadDir, courseId, assignmentId);
    }

    private String calculateFileHash(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            System.err.println("⚠️ Could not calculate file hash: " + e.getMessage());
            return null;
        }
    }

    private void updateAssignmentFileInfo(String assignmentId, AssignmentFile savedFile) {
        try {
            // Update the assignment (Task) with file information
            Optional<Task> taskOpt = taskRepository.findById(assignmentId);
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                task.setFileUrl("/api/assignment-files/" + savedFile.getId() + "/download");
                task.setFileName(savedFile.getOriginalFilename());
                task.setFileSize(savedFile.getFileSize());
                taskRepository.save(task);
                System.out.println("✅ Updated assignment with file information");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Could not update assignment with file info: " + e.getMessage());
        }
    }

    private void removeFileFromAssignment(String assignmentId, String fileId) {
        try {
            Optional<Task> taskOpt = taskRepository.findById(assignmentId);
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                // Check if this was the primary file for the assignment
                if (task.getFileUrl() != null && task.getFileUrl().contains(fileId)) {
                    task.setFileUrl(null);
                    task.setFileName(null);
                    task.setFileSize(null);
                    taskRepository.save(task);
                    System.out.println("✅ Removed file reference from assignment");
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Could not remove file reference from assignment: " + e.getMessage());
        }
    }

    private boolean canUserUploadToAssignment(String assignmentId, String userId) {
        try {
            Optional<Task> taskOpt = taskRepository.findById(assignmentId);
            if (taskOpt.isEmpty()) return false;

            Task task = taskOpt.get();

            // Check if user is the instructor of the task
            if (userId.equals(task.getInstructorId())) {
                return true;
            }

            // Check if user is lecturer of the course
            Optional<Course> courseOpt = courseRepository.findById(task.getCourseId());
            if (courseOpt.isPresent()) {
                return userId.equals(courseOpt.get().getLecturerId());
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canUserAccessAssignment(String assignmentId, String userId, String userRole) {
        // Implementation would check assignment access permissions
        // For now, simplified version
        return true; // You should implement proper permission checking
    }

    private boolean canUserAccessCourse(String courseId, String userId, String userRole) {
        // Implementation would check course access permissions
        // For now, simplified version
        return true; // You should implement proper permission checking
    }

    private boolean isStudentEnrolledInCourse(String userId, String courseId) {
        try {
            Optional<Course> courseOpt = courseRepository.findById(courseId);
            if (courseOpt.isPresent()) {
                Course course = courseOpt.get();
                return course.getEnrollments().stream()
                        .anyMatch(enrollment -> enrollment.getStudentIds().contains(userId));
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLecturerOfCourse(String userId, String courseId) {
        try {
            Optional<Course> courseOpt = courseRepository.findById(courseId);
            return courseOpt.isPresent() && userId.equals(courseOpt.get().getLecturerId());
        } catch (Exception e) {
            return false;
        }
    }
}