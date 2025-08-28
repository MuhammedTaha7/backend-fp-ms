package com.example.edusphere.controller;

import com.example.edusphere.dto.request.CreateCategoryRequest;
import com.example.edusphere.entity.CourseFile;
import com.example.edusphere.entity.FileCategory;
import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import com.example.edusphere.service.CourseContentService;
import com.example.edusphere.service.CourseFileStorageService;
import com.example.common.exceptions.FileStorageException;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course-content")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class FileController {

    private final CourseContentService courseContentService;
    private final CourseFileStorageService fileStorageService;
    private final UserRepository userRepository;

    public FileController(CourseContentService courseContentService,
                          CourseFileStorageService fileStorageService,
                          UserRepository userRepository) {
        this.courseContentService = courseContentService;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    // ===========================================
    // CATEGORY MANAGEMENT ENDPOINTS
    // ===========================================

    /**
     * POST /api/course-content/categories : Create a new category
     */
    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(
            @RequestParam String courseId,
            @RequestParam int year,
            @RequestBody CreateCategoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Only admins and lecturers can create categories
            if (!"1100".equals(currentUser.getRole()) && !"1200".equals(currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only administrators and lecturers can create categories"));
            }

            FileCategory category = courseContentService.createCategory(courseId, year, request);
            return new ResponseEntity<>(category, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * GET /api/course-content/categories/by-course/{courseId} : Get categories by course and year
     */
    @GetMapping("/categories/by-course/{courseId}")
    public ResponseEntity<?> getCategoriesByCourse(
            @PathVariable String courseId,
            @RequestParam int year,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Basic access control - all authenticated users can view categories
            List<FileCategory> categories = courseContentService.getCategoriesByCourse(courseId, year);
            return ResponseEntity.ok(categories);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * PUT /api/course-content/categories/{categoryId} : Update a category
     */
    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<?> updateCategory(
            @PathVariable String categoryId,
            @RequestBody FileCategory categoryDetails,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check permissions
            if (!courseContentService.canUserAccessCategory(categoryId, currentUser.getId(), currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to update this category"));
            }

            // Only admins and lecturers can update categories
            if (!"1100".equals(currentUser.getRole()) && !"1200".equals(currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only administrators and lecturers can update categories"));
            }

            FileCategory updatedCategory = courseContentService.updateCategory(categoryId, categoryDetails);
            return ResponseEntity.ok(updatedCategory);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * DELETE /api/course-content/categories/{categoryId} : Delete a category
     */
    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<?> deleteCategory(
            @PathVariable String categoryId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check permissions
            if (!courseContentService.canUserAccessCategory(categoryId, currentUser.getId(), currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to delete this category"));
            }

            // Only admins and lecturers can delete categories
            if (!"1100".equals(currentUser.getRole()) && !"1200".equals(currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only administrators and lecturers can delete categories"));
            }

            courseContentService.deleteCategory(categoryId);
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    // ===========================================
    // FILE MANAGEMENT ENDPOINTS
    // ===========================================

    /**
     * POST /api/course-content/files/upload/{categoryId} : Upload a file to a specific category
     */
    @PostMapping("/files/upload/{categoryId}")
    public ResponseEntity<?> uploadFile(
            @PathVariable String categoryId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check access permissions
            if (!courseContentService.canUserAccessCategory(categoryId, currentUser.getId(), currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to upload files to this category"));
            }

            // Store the file
            CourseFile uploadedFile = courseContentService.storeFile(categoryId, file);

            // Set additional metadata
            if (description != null && !description.trim().isEmpty()) {
                uploadedFile.setFileDescription(description.trim());
            }
            uploadedFile.setUploadedBy(currentUser.getId());

            return new ResponseEntity<>(uploadedFile, HttpStatus.CREATED);

        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * GET /api/course-content/files/{fileId}/download : Download a specific file
     */
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<?> downloadFile(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Find file metadata
            CourseFile fileMetadata = courseContentService.getFileMetadata(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check access permissions
            if (!courseContentService.canUserAccessCategory(fileMetadata.getCategoryId(), currentUser.getId(), currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to download this file"));
            }

            // Load file as Resource
            Resource resource = fileStorageService.loadFileAsResource(fileMetadata.getStoredFileName());

            // Increment download count
            fileMetadata.incrementDownloadCount();

            // Build the response with correct headers
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileMetadata.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileMetadata.getFileName() + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileMetadata.getSize()))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(resource);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred while downloading the file"));
        }
    }

    /**
     * DELETE /api/course-content/files/{fileId} : Delete a specific file
     */
    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<?> deleteFile(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Find file metadata first to check permissions
            CourseFile fileMetadata = courseContentService.getFileMetadata(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check access permissions (only admin and lecturers can delete)
            if (!"1100".equals(currentUser.getRole()) &&
                    !courseContentService.canUserAccessCategory(fileMetadata.getCategoryId(), currentUser.getId(), currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You don't have permission to delete this file"));
            }

            // Students cannot delete files
            if ("1300".equals(currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Students cannot delete files"));
            }

            courseContentService.deleteFile(fileId);
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred while deleting the file"));
        }
    }

    /**
     * GET /api/course-content/files/by-category/{categoryId} : Get all files for a specific category with pagination
     */
    @GetMapping("/files/by-category/{categoryId}")
    public ResponseEntity<?> getFilesByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check access permissions
//            if (!courseContentService.canUserAccessCategory(categoryId, currentUser.getId(), currentUser.getRole())) {
//                return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                        .body(Map.of("error", "You don't have permission to view files in this category"));
//            }

            // Create sort object
            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();

            // Create pageable object
            Pageable pageable = PageRequest.of(page, size, sort);

            // Get paginated files
            Page<CourseFile> files = courseContentService.getFilesByCategoryPaginated(categoryId, pageable);
            return ResponseEntity.ok(files);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred while fetching files"));
        }
    }

    /**
     * GET /api/course-content/files/by-category/{categoryId}/simple : Get all files for a specific category (simple list)
     */
    @GetMapping("/files/by-category/{categoryId}/simple")
    public ResponseEntity<?> getFilesByCategorySimple(
            @PathVariable String categoryId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

//            // Check access permissions
//            if (!courseContentService.canUserAccessCategory(categoryId, currentUser.getId(), currentUser.getRole())) {
//                return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                        .body(Map.of("error", "You don't have permission to view files in this category"));
//            }

            List<CourseFile> files = courseContentService.getFilesByCategory(categoryId);
            return ResponseEntity.ok(files);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred while fetching files"));
        }
    }

    /**
     * GET /api/course-content/files/category/{categoryId}/count : Get file count for a specific category
     */
    @GetMapping("/files/category/{categoryId}/count")
    public ResponseEntity<?> getFilesCountByCategory(
            @PathVariable String categoryId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check access permissions
//            if (!courseContentService.canUserAccessCategory(categoryId, currentUser.getId(), currentUser.getRole())) {
//                return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                        .body(Map.of("error", "You don't have permission to view this category"));
//            }

            long count = courseContentService.getFilesCountByCategory(categoryId);
            return ResponseEntity.ok(Map.of("count", count));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * GET /api/course-content/files/{fileId}/info : Get file metadata without downloading
     */
    @GetMapping("/files/{fileId}/info")
    public ResponseEntity<?> getFileInfo(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Find file metadata
            CourseFile fileMetadata = courseContentService.getFileMetadata(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));

            // Get current user
            UserEntity currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

            // Check access permissions
//            if (!courseContentService.canUserAccessCategory(fileMetadata.getCategoryId(), currentUser.getId(), currentUser.getRole())) {
//                return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                        .body(Map.of("error", "You don't have permission to view this file"));
//            }

            return ResponseEntity.ok(fileMetadata);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }
}