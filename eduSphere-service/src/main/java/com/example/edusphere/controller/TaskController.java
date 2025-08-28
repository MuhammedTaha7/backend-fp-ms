package com.example.edusphere.controller;

import com.example.edusphere.dto.request.TaskCreateRequest;
import com.example.edusphere.dto.request.TaskUpdateRequest;
import com.example.edusphere.dto.response.TaskDetailResponse;
import com.example.edusphere.dto.response.TaskResponse;
import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import com.example.edusphere.entity.Task;
import com.example.edusphere.service.TaskService;
import com.example.edusphere.service.TaskSubmissionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/tasks")
// REMOVED: @CrossOrigin annotation to prevent CORS duplication - handled by SecurityConfig
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;
    private final TaskSubmissionService taskSubmissionService;

    @Value("${app.upload.dir:${user.home}/uploads}")
    private String uploadDir;

    @Value("${app.upload.max-file-size:10485760}") // 10MB default
    private long maxFileSize;

    public TaskController(TaskService taskService, UserRepository userRepository, TaskSubmissionService taskSubmissionService) {
        this.taskService = taskService;
        this.userRepository = userRepository;
        this.taskSubmissionService = taskSubmissionService;
    }


    /**
     * POST /api/tasks/upload-file : Upload file for task with proper encoding
     */
    @PostMapping("/upload-file")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> uploadTaskFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "context", defaultValue = "assignment") String context,
            @RequestParam(value = "assignmentId", required = false) String assignmentId,
            @RequestParam(value = "courseId", required = false) String courseId,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📁 === UPLOADING TASK FILE ===");
            System.out.println("File: " + file.getOriginalFilename());
            System.out.println("Size: " + file.getSize() + " bytes");
            System.out.println("Content Type: " + file.getContentType());
            System.out.println("Context: " + context);
            System.out.println("Assignment ID: " + assignmentId);
            System.out.println("Course ID: " + courseId);

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty"));
            }

            if (file.getSize() > maxFileSize) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File size exceeds maximum limit of " + (maxFileSize / 1024 / 1024) + "MB"));
            }

            // Enhanced file type validation
            String contentType = file.getContentType();
            String originalFilename = file.getOriginalFilename();

            if (!isValidFileType(contentType, originalFilename)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File type not supported. Please use PDF, DOC, DOCX, TXT, ZIP, JPG, PNG, or GIF files."));
            }

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate ASCII-safe unique filename to prevent corruption
            String cleanOriginalFilename = StringUtils.cleanPath(originalFilename);
            String fileExtension = getFileExtension(cleanOriginalFilename);
            String uniqueFilename = generateSafeUniqueFilename(cleanOriginalFilename, fileExtension);

            // Save file with proper error handling
            Path filePath = uploadPath.resolve(uniqueFilename);
            try {
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("📁 File saved to: " + filePath.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("❌ Error saving file: " + e.getMessage());
                throw new IOException("Failed to save file: " + e.getMessage());
            }

            // Verify file was saved correctly
            if (!Files.exists(filePath) || Files.size(filePath) != file.getSize()) {
                throw new IOException("File was not saved correctly - size mismatch");
            }

            // Generate proper file URL with encoding
            String fileUrl = "/api/tasks/files/" + URLEncoder.encode(uniqueFilename, StandardCharsets.UTF_8);

            // Determine and store the correct content type
            String detectedContentType = detectContentType(filePath, originalFilename);

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("id", UUID.randomUUID().toString());
            response.put("url", fileUrl);
            response.put("fileUrl", fileUrl);
            response.put("fileName", cleanOriginalFilename);
            response.put("name", cleanOriginalFilename);
            response.put("fileSize", file.getSize());
            response.put("size", file.getSize());
            response.put("contentType", detectedContentType);
            response.put("originalContentType", contentType);
            response.put("uploadedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            response.put("context", context);
            response.put("uniqueFilename", uniqueFilename);

            System.out.println("✅ File uploaded successfully: " + uniqueFilename);
            System.out.println("📄 Detected content type: " + detectedContentType);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            System.err.println("❌ Error uploading file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error uploading file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * FIXED: Enhanced file serving with proper headers, encoding, and security
     * REMOVED manual CORS headers to prevent duplication - handled by SecurityConfig
     */
    @GetMapping("/files/{filename}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String filename,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📁 === SERVING FILE ===");
            System.out.println("Raw filename parameter: " + filename);

            // Decode URL-encoded filename properly
            String decodedFilename;
            try {
                decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
                System.out.println("📄 Decoded filename: " + decodedFilename);
            } catch (Exception e) {
                System.err.println("⚠️ Could not decode filename, using as-is: " + e.getMessage());
                decodedFilename = filename;
            }

            // Enhanced security validation
            if (!isValidFilename(decodedFilename)) {
                System.err.println("❌ Invalid filename: " + decodedFilename);
                return ResponseEntity.badRequest().build();
            }

            // Resolve file path securely with normalization
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(decodedFilename).normalize();

            // Security check: ensure file is within upload directory
            if (!filePath.startsWith(uploadPath)) {
                System.err.println("❌ Security violation: file path outside upload directory");
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new FileSystemResource(filePath);

            if (!resource.exists() || !resource.isReadable()) {
                System.err.println("❌ File not found or not readable: " + decodedFilename);
                return ResponseEntity.notFound().build();
            }

            // Content type detection with better accuracy
            String contentType = detectContentType(filePath, decodedFilename);
            System.out.println("📄 Serving with content type: " + contentType);

            // Get original filename from the unique filename
            String originalFilename = extractOriginalFilename(decodedFilename);
            System.out.println("📄 Original filename: " + originalFilename);

            // Build response headers (REMOVED manual CORS headers to prevent duplication)
            HttpHeaders headers = new HttpHeaders();

            // Content disposition with proper encoding for Hebrew/Unicode filenames
            String encodedOriginalFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8);

            // Set content disposition based on file type for proper browser handling
            if (contentType.startsWith("image/") || contentType.equals("application/pdf")) {
                // For images and PDFs, use inline to display in browser
                headers.add(HttpHeaders.CONTENT_DISPOSITION,
                        String.format("inline; filename=\"%s\"; filename*=UTF-8''%s",
                                originalFilename, encodedOriginalFilename));
            } else {
                // For other files, force download
                headers.add(HttpHeaders.CONTENT_DISPOSITION,
                        String.format("attachment; filename=\"%s\"; filename*=UTF-8''%s",
                                originalFilename, encodedOriginalFilename));
            }

            // Cache control headers to prevent corruption
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            // Add file size for proper content length
            try {
                long fileSize = Files.size(filePath);
                headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize));
                System.out.println("📄 File size: " + fileSize + " bytes");
            } catch (IOException e) {
                System.err.println("⚠️ Could not determine file size: " + e.getMessage());
            }

            // Set proper media type with charset for text files
            MediaType mediaType;
            try {
                if (contentType.startsWith("text/")) {
                    mediaType = MediaType.parseMediaType(contentType + "; charset=UTF-8");
                } else {
                    mediaType = MediaType.parseMediaType(contentType);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Invalid content type, using octet-stream: " + e.getMessage());
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            System.out.println("✅ Serving file: " + decodedFilename + " as " + originalFilename);
            System.out.println("📄 Content-Type: " + mediaType);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(mediaType)
                    .body(resource);

        } catch (Exception e) {
            System.err.println("❌ Error serving file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/tasks/files/{filename} : Delete uploaded file with proper decoding
     */
    @DeleteMapping("/files/{filename}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteFile(
            @PathVariable String filename,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("🗑️ === DELETING FILE ===");
            System.out.println("Raw filename: " + filename);

            // Decode URL-encoded filename
            String decodedFilename;
            try {
                decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
                System.out.println("📄 Decoded filename: " + decodedFilename);
            } catch (Exception e) {
                System.err.println("⚠️ Could not decode filename, using as-is: " + e.getMessage());
                decodedFilename = filename;
            }

            // Validate filename (security check)
            if (!isValidFilename(decodedFilename)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid filename"));
            }

            // Secure path resolution
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(decodedFilename).normalize();

            // Security check: ensure file is within upload directory
            if (!filePath.startsWith(uploadPath)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid file path"));
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("✅ File deleted successfully: " + decodedFilename);
                return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
            } else {
                System.out.println("⚠️ File not found: " + decodedFilename);
                return ResponseEntity.ok(Map.of("message", "File not found (may already be deleted)"));
            }

        } catch (IOException e) {
            System.err.println("❌ Error deleting file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error deleting file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    // ENHANCED HELPER METHODS FOR FILE OPERATIONS

    /**
     * Enhanced file type validation using both content type and extension
     */
    private boolean isValidFileType(String contentType, String filename) {
        if (filename == null || filename.isEmpty()) return false;

        // Get file extension
        String extension = getFileExtension(filename).toLowerCase();

        // Valid extensions
        Set<String> allowedExtensions = Set.of(
                ".pdf", ".doc", ".docx", ".txt", ".zip",
                ".jpg", ".jpeg", ".png", ".gif"
        );

        // Check extension first
        if (!allowedExtensions.contains(extension)) {
            System.err.println("❌ Invalid file extension: " + extension);
            return false;
        }

        // If content type is provided, validate it as well (but be lenient)
        if (contentType != null) {
            List<String> allowedTypes = List.of(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "text/plain",
                    "application/zip",
                    "application/x-zip-compressed",
                    "application/octet-stream", // Sometimes browsers send this
                    "image/jpeg",
                    "image/jpg",
                    "image/png",
                    "image/gif"
            );

            // Be more lenient with content type validation
            if (!allowedTypes.contains(contentType.toLowerCase())) {
                System.err.println("⚠️ Suspicious content type: " + contentType + " for extension: " + extension);
                // Still allow if extension is valid (browser MIME detection can be unreliable)
            }
        }

        return true;
    }

    /**
     * Content type detection with better accuracy for PDFs
     */
    private String detectContentType(Path filePath, String filename) {
        try {
            // First try to probe content type from file content
            String detectedType = Files.probeContentType(filePath);

            if (detectedType != null && !detectedType.equals("application/octet-stream")) {
                System.out.println("📄 Content type detected from file: " + detectedType);
                return detectedType;
            }

            // Fallback to extension-based detection
            String extension = getFileExtension(filename).toLowerCase();
            String typeFromExtension = getContentTypeByExtension(extension);
            System.out.println("📄 Content type from extension: " + typeFromExtension);
            return typeFromExtension;

        } catch (IOException e) {
            System.err.println("⚠️ Error detecting content type, using extension-based detection: " + e.getMessage());
            String extension = getFileExtension(filename).toLowerCase();
            return getContentTypeByExtension(extension);
        }
    }

    /**
     * Get content type based on file extension
     */
    private String getContentTypeByExtension(String extension) {
        Map<String, String> extensionToContentType = Map.of(
                ".pdf", "application/pdf",
                ".doc", "application/msword",
                ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                ".txt", "text/plain",
                ".zip", "application/zip",
                ".jpg", "image/jpeg",
                ".jpeg", "image/jpeg",
                ".png", "image/png",
                ".gif", "image/gif"
        );

        return extensionToContentType.getOrDefault(extension, "application/octet-stream");
    }

    /**
     * Enhanced filename validation
     */
    private boolean isValidFilename(String filename) {
        if (filename == null || filename.isEmpty()) return false;

        // Check for path traversal attempts
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return false;
        }

        // Check for null bytes
        if (filename.contains("\0")) {
            return false;
        }

        // Check for control characters (but allow Unicode characters for international names)
        for (char c : filename.toCharArray()) {
            if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r') {
                return false;
            }
        }

        return true;
    }

    /**
     * Extract original filename from unique filename
     */
    private String extractOriginalFilename(String uniqueFilename) {
        try {
            // Format: timestamp_uuid_originalfilename
            // Find the second underscore and extract everything after it
            int firstUnderscore = uniqueFilename.indexOf('_');
            if (firstUnderscore > 0) {
                int secondUnderscore = uniqueFilename.indexOf('_', firstUnderscore + 1);
                if (secondUnderscore > 0) {
                    return uniqueFilename.substring(secondUnderscore + 1);
                }
            }
            // Fallback to the unique filename if pattern doesn't match
            return uniqueFilename;
        } catch (Exception e) {
            System.err.println("⚠️ Could not extract original filename from: " + uniqueFilename);
            return uniqueFilename;
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) return "";
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }

    /**
     * Generate ASCII-safe unique filename to prevent file corruption
     */
    private String generateSafeUniqueFilename(String originalFilename, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        // Clean the base name but preserve important characters
        String baseName = originalFilename;
        if (extension != null && !extension.isEmpty()) {
            baseName = baseName.substring(0, baseName.lastIndexOf('.'));
        }

        // Replace problematic characters and ensure ASCII-safe filename
        // Convert Hebrew and other Unicode characters to safe ASCII equivalents
        baseName = baseName
                .replaceAll("[<>:\"/\\\\|?*]", "_")  // Replace filesystem-unsafe characters
                .replaceAll("[\\u0590-\\u05FF]", "_")  // Replace Hebrew characters
                .replaceAll("[\\u0600-\\u06FF]", "_")  // Replace Arabic characters
                .replaceAll("[\\u4e00-\\u9fff]", "_")  // Replace Chinese characters
                .replaceAll("[\\u3040-\\u309f]", "_")  // Replace Hiragana
                .replaceAll("[\\u30a0-\\u30ff]", "_")  // Replace Katakana
                .replaceAll("[^\\x00-\\x7F]", "_")     // Replace any remaining non-ASCII
                .replaceAll("_+", "_")                 // Replace multiple underscores with single
                .replaceAll("^_|_$", "");              // Remove leading/trailing underscores

        // Ensure we have a valid base name
        if (baseName.isEmpty()) {
            baseName = "file";
        }

        // Limit length
        if (baseName.length() > 50) {
            baseName = baseName.substring(0, 50);
        }

        return timestamp + "_" + uuid + "_" + baseName + extension;
    }

    // New endpoint for inter-service communication
    @GetMapping("/by-courses")
    public ResponseEntity<List<Task>> getTasksByCourses(@RequestParam List<String> courseIds) {
        try {
            List<Task> tasks = taskService.findByCourseIdIn(courseIds);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/tasks : Create a new task
     */
    @PostMapping
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> createTask(
            @Valid @RequestBody TaskCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📝 === CREATING TASK ===");
            System.out.println("User: " + userDetails.getUsername());
            System.out.println("Request: " + request);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            TaskResponse createdTask = taskService.createTask(request, currentUser.getId());
            System.out.println("✅ Task created successfully: " + createdTask.getId());

            return new ResponseEntity<>(createdTask, HttpStatus.CREATED);

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error creating task: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error creating task: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/tasks/{taskId} : Update an existing task
     */
    @PutMapping("/{taskId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateTask(
            @PathVariable String taskId,
            @Valid @RequestBody TaskUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("🔄 === UPDATING TASK ===");
            System.out.println("Task ID: " + taskId);
            System.out.println("User: " + userDetails.getUsername());

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            TaskResponse updatedTask = taskService.updateTask(taskId, request, currentUser.getId());
            System.out.println("✅ Task updated successfully");

            return ResponseEntity.ok(updatedTask);

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error updating task: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error updating task: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/tasks/{taskId} : Delete a task
     */
    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteTask(
            @PathVariable String taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("🗑️ === DELETING TASK ===");
            System.out.println("Task ID: " + taskId);
            System.out.println("User: " + userDetails.getUsername());

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            taskService.deleteTask(taskId, currentUser.getId());
            System.out.println("✅ Task deleted successfully");

            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error deleting task: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error deleting task: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasks/{taskId} : Get a single task by ID
     */
    @GetMapping("/{taskId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> getTaskById(
            @PathVariable String taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("🔍 === FETCHING TASK ===");
            System.out.println("Task ID: " + taskId);
            System.out.println("User: " + userDetails.getUsername());

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check access permission
            if (!taskService.canUserAccessTask(taskId, currentUser.getId(), currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You don't have permission to view this task"));
            }

            Optional<TaskResponse> task = taskService.getTaskById(taskId);
            if (task.isPresent()) {
                System.out.println("✅ Task found: " + task.get().getTitle());
                return ResponseEntity.ok(task.get());
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error fetching task: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error fetching task: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasks/{taskId}/details : Get detailed task information
     */
    @GetMapping("/{taskId}/details")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> getTaskDetails(
            @PathVariable String taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📋 === FETCHING TASK DETAILS ===");
            System.out.println("Task ID: " + taskId);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            TaskDetailResponse taskDetails = taskService.getTaskDetails(taskId, currentUser.getId());
            System.out.println("✅ Task details fetched successfully");

            return ResponseEntity.ok(taskDetails);

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error fetching task details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error fetching task details: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasks/{taskId}/download : Download task file - FIXED with proper authorization
     */
    @GetMapping("/{taskId}/download")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> downloadTaskFile(
            @PathVariable String taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📥 === DOWNLOADING TASK FILE ===");
            System.out.println("Task ID: " + taskId);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check access permission
            if (!taskService.canUserAccessTask(taskId, currentUser.getId(), currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You don't have permission to download this file"));
            }

            Optional<TaskResponse> taskOpt = taskService.getTaskById(taskId);
            if (taskOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            TaskResponse task = taskOpt.get();
            if (task.getFileUrl() == null || task.getFileUrl().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No file attached to this task"));
            }

            // Extract filename from file URL
            String filename = task.getFileUrl().substring(task.getFileUrl().lastIndexOf("/") + 1);

            // Serve the file using the existing file serving endpoint
            return serveFile(filename, userDetails);

        } catch (Exception e) {
            System.err.println("❌ Error downloading task file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasks/{taskId}/can-submit/{studentId} : Check if student can submit
     */
    @GetMapping("/{taskId}/can-submit/{studentId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> canStudentSubmit(
            @PathVariable String taskId,
            @PathVariable String studentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("🔍 === CHECKING SUBMISSION ELIGIBILITY ===");
            System.out.println("Task ID: " + taskId + ", Student ID: " + studentId);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Students can only check their own eligibility
            if ("1300".equals(currentUser.getRole()) && !currentUser.getId().equals(studentId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You can only check your own submission eligibility"));
            }

            boolean canSubmit = taskSubmissionService.canStudentSubmit(taskId, studentId);
            boolean hasSubmitted = taskSubmissionService.hasStudentSubmitted(taskId, studentId);
            int attemptCount = taskSubmissionService.getSubmissionAttemptCount(taskId, studentId);

            Map<String, Object> result = Map.of(
                    "canSubmit", canSubmit,
                    "hasSubmitted", hasSubmitted,
                    "attemptCount", attemptCount,
                    "taskId", taskId,
                    "studentId", studentId
            );

            System.out.println("✅ Submission eligibility checked");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("❌ Error checking submission eligibility: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasks/course/{courseId} : Get all tasks for a course
     */
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> getTasksByCourse(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📚 === FETCHING TASKS BY COURSE ===");
            System.out.println("Course ID: " + courseId);
            System.out.println("User: " + userDetails.getUsername());
            System.out.println("Filters - Status: " + status + ", Category: " + category + ", Priority: " + priority);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Create sort object
            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();

            // Create pageable object
            Pageable pageable = PageRequest.of(page, size, sort);

            List<TaskResponse> tasks;

            // Apply filters based on parameters
            if (status != null) {
                tasks = taskService.getTasksByStatus(courseId, status);
            } else if (category != null) {
                tasks = taskService.getTasksByCategory(courseId, category);
            } else if (priority != null) {
                tasks = taskService.getTasksByPriority(courseId, priority);
            } else {
                // For students, only return available tasks
                if ("1300".equals(currentUser.getRole())) {
                    tasks = taskService.getAvailableTasksForStudents(courseId);
                } else {
                    tasks = taskService.getTasksByCourse(courseId);
                }
            }

            System.out.println("✅ Found " + tasks.size() + " tasks");
            return ResponseEntity.ok(tasks);

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error fetching tasks: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error fetching tasks: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasks/student/{studentId}/course/{courseId} : Get tasks for a student in a specific course
     */
    @GetMapping("/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> getTasksForStudent(
            @PathVariable String studentId,
            @PathVariable String courseId,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("👨‍🎓 === FETCHING TASKS FOR STUDENT ===");
            System.out.println("Student ID: " + studentId + ", Course ID: " + courseId);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Students can only see their own tasks
            if ("1300".equals(currentUser.getRole()) && !currentUser.getId().equals(studentId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You can only view your own tasks"));
            }

            List<TaskResponse> tasks = taskService.getTasksForStudent(studentId, courseId, status);
            System.out.println("✅ Found " + tasks.size() + " tasks for student");

            return ResponseEntity.ok(tasks);

        } catch (Exception e) {
            System.err.println("❌ Error fetching tasks for student: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasks/student/{studentId}/overdue : Get overdue tasks for a student
     */
    @GetMapping("/student/{studentId}/overdue")
    @PreAuthorize("hasRole('STUDENT') or hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> getOverdueTasksForStudent(
            @PathVariable String studentId,
            @RequestParam(required = false) String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("⏰ === FETCHING OVERDUE TASKS FOR STUDENT ===");
            System.out.println("Student ID: " + studentId + ", Course ID: " + courseId);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Students can only see their own tasks
            if ("1300".equals(currentUser.getRole()) && !currentUser.getId().equals(studentId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You can only view your own tasks"));
            }

            List<TaskResponse> tasks = taskService.getOverdueTasksForStudent(studentId, courseId);
            System.out.println("✅ Found " + tasks.size() + " overdue tasks for student");

            return ResponseEntity.ok(tasks);

        } catch (Exception e) {
            System.err.println("❌ Error fetching overdue tasks for student: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasks/student/{studentId}/upcoming : Get upcoming tasks for a student
     */
    @GetMapping("/student/{studentId}/upcoming")
    @PreAuthorize("hasRole('STUDENT') or hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> getUpcomingTasksForStudent(
            @PathVariable String studentId,
            @RequestParam(required = false) String courseId,
            @RequestParam(defaultValue = "7") int daysAhead,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📅 === FETCHING UPCOMING TASKS FOR STUDENT ===");
            System.out.println("Student ID: " + studentId + ", Course ID: " + courseId + ", Days Ahead: " + daysAhead);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Students can only see their own tasks
            if ("1300".equals(currentUser.getRole()) && !currentUser.getId().equals(studentId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You can only view your own tasks"));
            }

            List<TaskResponse> tasks = taskService.getUpcomingTasksForStudent(studentId, courseId, daysAhead);
            System.out.println("✅ Found " + tasks.size() + " upcoming tasks for student");

            return ResponseEntity.ok(tasks);

        } catch (Exception e) {
            System.err.println("❌ Error fetching upcoming tasks for student: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasks/course/{courseId}/overdue : Get overdue tasks for a course
     */
    @GetMapping("/course/{courseId}/overdue")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> getOverdueTasks(
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("⏰ === FETCHING OVERDUE TASKS ===");
            System.out.println("Course ID: " + courseId);

            List<TaskResponse> tasks = taskService.getOverdueTasks(courseId);
            System.out.println("✅ Found " + tasks.size() + " overdue tasks");

            return ResponseEntity.ok(tasks);

        } catch (Exception e) {
            System.err.println("❌ Error fetching overdue tasks: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasks/course/{courseId}/upcoming : Get upcoming tasks for a course
     */
    @GetMapping("/course/{courseId}/upcoming")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> getUpcomingTasks(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "7") int daysAhead,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📅 === FETCHING UPCOMING TASKS ===");
            System.out.println("Course ID: " + courseId + ", Days Ahead: " + daysAhead);

            List<TaskResponse> tasks = taskService.getUpcomingTasks(courseId, daysAhead);
            System.out.println("✅ Found " + tasks.size() + " upcoming tasks");

            return ResponseEntity.ok(tasks);

        } catch (Exception e) {
            System.err.println("❌ Error fetching upcoming tasks: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasks/course/{courseId}/search : Search tasks in a course
     */
    @GetMapping("/course/{courseId}/search")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> searchTasks(
            @PathVariable String courseId,
            @RequestParam String searchTerm,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("🔍 === SEARCHING TASKS ===");
            System.out.println("Course ID: " + courseId + ", Search Term: " + searchTerm);

            List<TaskResponse> tasks = taskService.searchTasks(courseId, searchTerm);
            System.out.println("✅ Found " + tasks.size() + " tasks matching search");

            return ResponseEntity.ok(tasks);

        } catch (Exception e) {
            System.err.println("❌ Error searching tasks: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasks/instructor/{instructorId} : Get all tasks by instructor
     */
    @GetMapping("/instructor/{instructorId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> getTasksByInstructor(
            @PathVariable String instructorId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("👨‍🏫 === FETCHING TASKS BY INSTRUCTOR ===");
            System.out.println("Instructor ID: " + instructorId);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Lecturers can only see their own tasks unless they're admin
            if ("1200".equals(currentUser.getRole()) && !currentUser.getId().equals(instructorId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You can only view your own tasks"));
            }

            List<TaskResponse> tasks = taskService.getTasksByInstructor(instructorId);
            System.out.println("✅ Found " + tasks.size() + " tasks for instructor");

            return ResponseEntity.ok(tasks);

        } catch (Exception e) {
            System.err.println("❌ Error fetching tasks by instructor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasks/course/{courseId}/needing-grading : Get tasks that need grading
     */
    @GetMapping("/course/{courseId}/needing-grading")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> getTasksNeedingGrading(
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📝 === FETCHING TASKS NEEDING GRADING ===");
            System.out.println("Course ID: " + courseId);

            List<TaskResponse> tasks = taskService.getTasksNeedingGrading(courseId);
            System.out.println("✅ Found " + tasks.size() + " tasks needing grading");

            return ResponseEntity.ok(tasks);

        } catch (Exception e) {
            System.err.println("❌ Error fetching tasks needing grading: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tasks/{taskId}/statistics : Get task statistics
     */
    @GetMapping("/{taskId}/statistics")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> getTaskStatistics(
            @PathVariable String taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📊 === FETCHING TASK STATISTICS ===");
            System.out.println("Task ID: " + taskId);

            TaskDetailResponse.TaskStatistics statistics = taskService.getTaskStatistics(taskId);
            if (statistics != null) {
                System.out.println("✅ Task statistics retrieved successfully");
                return ResponseEntity.ok(statistics);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            System.err.println("❌ Error fetching task statistics: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * POST /api/tasks/{taskId}/recalculate-statistics : Recalculate task statistics
     */
    @PostMapping("/{taskId}/recalculate-statistics")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> recalculateTaskStatistics(
            @PathVariable String taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("🔄 === RECALCULATING TASK STATISTICS ===");
            System.out.println("Task ID: " + taskId);

            taskService.recalculateTaskStatistics(taskId);
            System.out.println("✅ Task statistics recalculated successfully");

            return ResponseEntity.ok(Map.of("message", "Task statistics recalculated successfully"));

        } catch (Exception e) {
            System.err.println("❌ Error recalculating task statistics: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * POST /api/tasks/course/{courseId}/recalculate-all-statistics : Recalculate all task statistics for a course
     */
    @PostMapping("/course/{courseId}/recalculate-all-statistics")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> recalculateAllTaskStatisticsForCourse(
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("🔄 === RECALCULATING ALL TASK STATISTICS FOR COURSE ===");
            System.out.println("Course ID: " + courseId);

            taskService.recalculateAllTaskStatisticsForCourse(courseId);
            System.out.println("✅ All task statistics recalculated successfully for course");

            return ResponseEntity.ok(Map.of("message", "All task statistics recalculated successfully for course"));

        } catch (Exception e) {
            System.err.println("❌ Error recalculating all task statistics: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}