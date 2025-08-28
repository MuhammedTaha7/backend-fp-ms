package com.example.edusphere.controller;

import com.example.edusphere.dto.response.AssignmentFileResponse;
import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import com.example.edusphere.service.AssignmentFileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignment-files")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AssignmentFileController {

    private final AssignmentFileService assignmentFileService;
    private final UserRepository userRepository;

    public AssignmentFileController(AssignmentFileService assignmentFileService, UserRepository userRepository) {
        this.assignmentFileService = assignmentFileService;
        this.userRepository = userRepository;
    }

    /**
     * POST /api/assignment-files/upload : Upload a file for an assignment
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> uploadAssignmentFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("assignmentId") String assignmentId,
            @RequestParam("courseId") String courseId,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📁 === UPLOADING ASSIGNMENT FILE ===");
            System.out.println("File: " + file.getOriginalFilename());
            System.out.println("Size: " + file.getSize());
            System.out.println("Assignment ID: " + assignmentId);
            System.out.println("Course ID: " + courseId);
            System.out.println("Description: " + description);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Upload file
            AssignmentFileResponse fileResponse = assignmentFileService.uploadAssignmentFile(
                    file,
                    assignmentId,
                    courseId,
                    currentUser.getId(),
                    description
            );

            System.out.println("✅ Assignment file uploaded successfully: " + fileResponse.getId());
            return new ResponseEntity<>(fileResponse, HttpStatus.CREATED);

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error uploading assignment file: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error uploading assignment file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/assignment-files/{fileId}/download : Download an assignment file
     */
    @GetMapping("/{fileId}/download")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> downloadAssignmentFile(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📥 === DOWNLOADING ASSIGNMENT FILE ===");
            System.out.println("File ID: " + fileId);
            System.out.println("User: " + userDetails.getUsername());

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check access permission
            if (!assignmentFileService.canUserAccessFile(fileId, currentUser.getId(), currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You don't have permission to download this file"));
            }

            // Get file resource
            Resource fileResource = assignmentFileService.getAssignmentFileAsResource(fileId);
            AssignmentFileResponse fileInfo = assignmentFileService.getAssignmentFileInfo(fileId);

            if (fileResource == null || !fileResource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = assignmentFileService.getContentType(fileInfo.getOriginalFilename());

            System.out.println("✅ Assignment file download initiated: " + fileInfo.getOriginalFilename());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileInfo.getOriginalFilename() + "\"")
                    .body(fileResource);

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error downloading assignment file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error downloading assignment file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/assignment-files/{fileId}/view : View an assignment file (for images, PDFs)
     */
    @GetMapping("/{fileId}/view")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> viewAssignmentFile(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("👁️ === VIEWING ASSIGNMENT FILE ===");
            System.out.println("File ID: " + fileId);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check access permission
            if (!assignmentFileService.canUserAccessFile(fileId, currentUser.getId(), currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You don't have permission to view this file"));
            }

            // Get file resource
            Resource fileResource = assignmentFileService.getAssignmentFileAsResource(fileId);
            AssignmentFileResponse fileInfo = assignmentFileService.getAssignmentFileInfo(fileId);

            if (fileResource == null || !fileResource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = assignmentFileService.getContentType(fileInfo.getOriginalFilename());

            System.out.println("✅ Assignment file view initiated: " + fileInfo.getOriginalFilename());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileInfo.getOriginalFilename() + "\"")
                    .body(fileResource);

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error viewing assignment file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error viewing assignment file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/assignment-files/{fileId}/info : Get assignment file information
     */
    @GetMapping("/{fileId}/info")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> getAssignmentFileInfo(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("ℹ️ === GETTING ASSIGNMENT FILE INFO ===");
            System.out.println("File ID: " + fileId);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check access permission
            if (!assignmentFileService.canUserAccessFile(fileId, currentUser.getId(), currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You don't have permission to view this file"));
            }

            AssignmentFileResponse fileInfo = assignmentFileService.getAssignmentFileInfo(fileId);
            System.out.println("✅ Assignment file info retrieved: " + fileInfo.getOriginalFilename());
            return ResponseEntity.ok(fileInfo);

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error getting assignment file info: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error getting assignment file info: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/assignment-files/{fileId} : Delete an assignment file
     */
    @DeleteMapping("/{fileId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteAssignmentFile(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("🗑️ === DELETING ASSIGNMENT FILE ===");
            System.out.println("File ID: " + fileId);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check deletion permission
            if (!assignmentFileService.canUserDeleteFile(fileId, currentUser.getId(), currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You don't have permission to delete this file"));
            }

            assignmentFileService.deleteAssignmentFile(fileId, currentUser.getId(), currentUser.getRole());
            System.out.println("✅ Assignment file deleted successfully");

            return ResponseEntity.ok(Map.of("message", "Assignment file deleted successfully"));

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error deleting assignment file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error deleting assignment file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/assignment-files/assignment/{assignmentId} : Get files for a specific assignment
     */
    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> getFilesByAssignment(
            @PathVariable String assignmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📋 === GETTING FILES BY ASSIGNMENT ===");
            System.out.println("Assignment ID: " + assignmentId);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            List<AssignmentFileResponse> files = assignmentFileService.getFilesByAssignment(
                    assignmentId,
                    currentUser.getId(),
                    currentUser.getRole()
            );

            System.out.println("✅ Found " + files.size() + " files for assignment");
            return ResponseEntity.ok(files);

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error getting assignment files: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error getting assignment files: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/assignment-files/course/{courseId} : Get files for a specific course
     */
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> getFilesByCourse(
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📚 === GETTING FILES BY COURSE ===");
            System.out.println("Course ID: " + courseId);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            List<AssignmentFileResponse> files = assignmentFileService.getFilesByCourse(
                    courseId,
                    currentUser.getId(),
                    currentUser.getRole()
            );

            System.out.println("✅ Found " + files.size() + " files for course");
            return ResponseEntity.ok(files);

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error getting course files: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error getting course files: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/assignment-files/{fileId}/metadata : Update assignment file metadata
     */
    @PutMapping("/{fileId}/metadata")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateAssignmentFileMetadata(
            @PathVariable String fileId,
            @RequestBody Map<String, Object> updateRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📝 === UPDATING ASSIGNMENT FILE METADATA ===");
            System.out.println("File ID: " + fileId);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            String description = (String) updateRequest.get("description");
            Boolean visibleToStudents = (Boolean) updateRequest.get("visibleToStudents");

            AssignmentFileResponse updatedFile = assignmentFileService.updateAssignmentFileMetadata(
                    fileId,
                    description,
                    visibleToStudents,
                    currentUser.getId(),
                    currentUser.getRole()
            );

            System.out.println("✅ Assignment file metadata updated successfully");
            return ResponseEntity.ok(updatedFile);

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error updating assignment file metadata: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error updating assignment file metadata: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/assignment-files/stats/assignment/{assignmentId} : Get file statistics for an assignment
     */
    @GetMapping("/stats/assignment/{assignmentId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> getAssignmentFileStats(
            @PathVariable String assignmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📊 === GETTING ASSIGNMENT FILE STATISTICS ===");
            System.out.println("Assignment ID: " + assignmentId);

            AssignmentFileService.AssignmentFileStats stats = assignmentFileService.getAssignmentFileStats(assignmentId);
            System.out.println("✅ Assignment file statistics retrieved");
            return ResponseEntity.ok(stats);

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error getting assignment file stats: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error getting assignment file stats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/assignment-files/stats/course/{courseId} : Get file statistics for a course
     */
    @GetMapping("/stats/course/{courseId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> getCourseFileStats(
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("📊 === GETTING COURSE FILE STATISTICS ===");
            System.out.println("Course ID: " + courseId);

            AssignmentFileService.CourseFileStats stats = assignmentFileService.getCourseFileStats(courseId);
            System.out.println("✅ Course file statistics retrieved");
            return ResponseEntity.ok(stats);

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error getting course file stats: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error getting course file stats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * POST /api/assignment-files/cleanup/orphaned : Clean up orphaned files
     */
    @PostMapping("/cleanup/orphaned")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cleanupOrphanedFiles(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("🧹 === CLEANING UP ORPHANED FILES ===");

            int cleanedCount = assignmentFileService.cleanupOrphanedFiles();
            System.out.println("✅ Orphaned files cleanup completed");

            return ResponseEntity.ok(Map.of(
                    "message", "Orphaned files cleanup completed",
                    "cleanedCount", cleanedCount
            ));

        } catch (Exception e) {
            System.err.println("❌ Error cleaning up orphaned files: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/assignment-files/user/{userId} : Get files uploaded by a specific user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<?> getFilesByUploader(
            @PathVariable String userId,
            @RequestParam(required = false) String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            System.out.println("👤 === GETTING FILES BY UPLOADER ===");
            System.out.println("User ID: " + userId);
            System.out.println("Course ID: " + courseId);

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check permission - users can only view their own files unless they're admin
            if (!"1100".equals(currentUser.getRole()) && !currentUser.getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You can only view your own files"));
            }

            List<AssignmentFileResponse> files = assignmentFileService.getFilesByUploader(userId, courseId, currentUser.getRole());
            System.out.println("✅ Found " + files.size() + " files for uploader");
            return ResponseEntity.ok(files);

        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error getting files by uploader: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Unexpected error getting files by uploader: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}