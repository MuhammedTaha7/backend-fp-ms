package com.example.edusphere.controller;

import com.example.edusphere.entity.CourseFile;
import com.example.edusphere.service.CourseContentService;
import com.example.edusphere.service.CourseFileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class CourseFileController {

    private final CourseContentService courseContentService;
    private final CourseFileStorageService fileStorageService;

    public CourseFileController(CourseContentService courseContentService, CourseFileStorageService fileStorageService) {
        this.courseContentService = courseContentService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * POST /api/files/upload/{categoryId} : Upload a file to a specific category.
     */
    @PostMapping("/upload/{categoryId}")
    public ResponseEntity<CourseFile> uploadFile(@PathVariable String categoryId, @RequestParam("file") MultipartFile file) {
        CourseFile uploadedFile = courseContentService.storeFile(categoryId, file);
        return new ResponseEntity<>(uploadedFile, HttpStatus.CREATED);
    }

    /**
     * GET /api/files/{fileId}/download : Download a specific file.
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        // Find file metadata
        CourseFile fileMetadata = courseContentService.getFileMetadata(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));

        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(fileMetadata.getStoredFileName());

        // Build the response with correct headers
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileMetadata.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileMetadata.getFileName() + "\"")
                .body(resource);
    }

    /**
     * DELETE /api/files/{fileId} : Delete a specific file.
     */
    @DeleteMapping("/course/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileId) {
        courseContentService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/files/by-category/{categoryId} : Get all files for a specific category.
     */
    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<List<CourseFile>> getFilesByCategory(@PathVariable String categoryId) {
        List<CourseFile> files = courseContentService.getFilesByCategory(categoryId);
        return ResponseEntity.ok(files);
    }
}