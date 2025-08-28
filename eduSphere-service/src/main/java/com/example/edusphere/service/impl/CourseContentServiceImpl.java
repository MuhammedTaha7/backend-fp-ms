package com.example.edusphere.service.impl;

import com.example.edusphere.dto.request.CreateCategoryRequest;
import com.example.edusphere.entity.Course;
import com.example.edusphere.entity.CourseFile;
import com.example.edusphere.entity.FileCategory;
import com.example.edusphere.repository.CourseFileRepository;
import com.example.edusphere.repository.CourseRepository;
import com.example.edusphere.repository.FileCategoryRepository;
import com.example.edusphere.service.CourseContentService;
import com.example.edusphere.service.CourseFileStorageService;
import com.example.common.exceptions.FileStorageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CourseContentServiceImpl implements CourseContentService {

    private final FileCategoryRepository fileCategoryRepository;
    private final CourseFileRepository courseFileRepository;
    private final CourseRepository courseRepository;
    private final CourseFileStorageService courseFileStorageService;

    // File validation constants
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/gif",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "video/mp4", "audio/mpeg", "application/zip"
    );

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            ".exe", ".bat", ".cmd", ".scr", ".pif", ".jar", ".js", ".vbs", ".ps1"
    );

    public CourseContentServiceImpl(FileCategoryRepository fileCategoryRepository,
                                    CourseFileRepository courseFileRepository,
                                    CourseRepository courseRepository,
                                    CourseFileStorageService courseFileStorageService) {
        this.fileCategoryRepository = fileCategoryRepository;
        this.courseFileRepository = courseFileRepository;
        this.courseRepository = courseRepository;
        this.courseFileStorageService = courseFileStorageService;
    }

    // --- Category Implementations ---

    @Override
    @Transactional
    public FileCategory createCategory(String courseId, int year, CreateCategoryRequest request) {
        // Validate course exists
        if (!courseRepository.existsById(courseId)) {
            throw new RuntimeException("Course not found with id: " + courseId);
        }

        // Validate input
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }

        String trimmedName = request.getName().trim();

        // Check for duplicate category names in the same course and year
        if (fileCategoryRepository.existsByCourseIdAndAcademicYearAndName(courseId, year, trimmedName)) {
            throw new IllegalArgumentException("Category with this name already exists for this course and year");
        }

        FileCategory newCategory = new FileCategory();
        newCategory.setName(trimmedName);
        newCategory.setDescription(request.getDescription());
        newCategory.setColor(request.getColor());
        newCategory.setCourseId(courseId);
        newCategory.setAcademicYear(year);

        return fileCategoryRepository.save(newCategory);
    }

    @Override
    public List<FileCategory> getCategoriesByCourse(String courseId, int year) {
        return fileCategoryRepository.findByCourseIdAndAcademicYear(courseId, year);
    }

    @Override
    @Transactional
    public FileCategory updateCategory(String categoryId, FileCategory categoryDetails) {
        FileCategory existingCategory = fileCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

        // Validate and update name if provided
        if (categoryDetails.getName() != null && !categoryDetails.getName().trim().isEmpty()) {
            String newName = categoryDetails.getName().trim();

            // Check for duplicate names (excluding current category)
            if (!existingCategory.getName().equals(newName)) {
                // Use a simple check instead of the complex repository method
                List<FileCategory> existingCategories = fileCategoryRepository
                        .findByCourseIdAndAcademicYear(existingCategory.getCourseId(), existingCategory.getAcademicYear());

                boolean nameExists = existingCategories.stream()
                        .anyMatch(cat -> !cat.getId().equals(categoryId) && cat.getName().equals(newName));

                if (nameExists) {
                    throw new IllegalArgumentException("Category with this name already exists for this course and year");
                }
            }

            existingCategory.setName(newName);
        }

        if (categoryDetails.getDescription() != null) {
            existingCategory.setDescription(categoryDetails.getDescription());
        }

        if (categoryDetails.getColor() != null) {
            existingCategory.setColor(categoryDetails.getColor());
        }

        return fileCategoryRepository.save(existingCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(String categoryId) {
        // Verify category exists
        if (!fileCategoryRepository.existsById(categoryId)) {
            throw new RuntimeException("Category not found with id: " + categoryId);
        }

        try {
            // 1. Find all files associated with the category
            List<CourseFile> filesToDelete = courseFileRepository.findByCategoryId(categoryId);

            // 2. Delete each physical file from storage
            for (CourseFile file : filesToDelete) {
                try {
                    courseFileStorageService.deleteFile(file.getStoredFileName());
                } catch (Exception e) {
                    // Log the error but continue with deletion
                    System.err.println("Failed to delete physical file: " + file.getStoredFileName() + ". Error: " + e.getMessage());
                }
            }

            // 3. Delete all file metadata records from the database for this category
            courseFileRepository.deleteByCategoryId(categoryId);

            // 4. Finally, delete the category itself
            fileCategoryRepository.deleteById(categoryId);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete category: " + e.getMessage(), e);
        }
    }

    // --- File Implementations ---

    @Override
    @Transactional
    public CourseFile storeFile(String categoryId, MultipartFile file) {
        // Validate category exists
        if (!fileCategoryRepository.existsById(categoryId)) {
            throw new RuntimeException("Category not found with id: " + categoryId);
        }

        // Validate file
        validateFile(file);

        try {
            // 1. Store the physical file and get its unique stored name
            String storedFileName = courseFileStorageService.storeFile(file);

            // 2. Create the file metadata object
            CourseFile courseFile = new CourseFile();
            courseFile.setFileName(file.getOriginalFilename());
            courseFile.setStoredFileName(storedFileName);
            courseFile.setFileType(file.getContentType());
            courseFile.setSize(file.getSize());
            courseFile.setCategoryId(categoryId);

            // 3. Save the metadata to the database
            return courseFileRepository.save(courseFile);

        } catch (Exception e) {
            throw new FileStorageException("Failed to store file: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteFile(String fileId) {
        // 1. Find the file metadata
        CourseFile file = courseFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));

        try {
            // 2. Delete the physical file from storage
            courseFileStorageService.deleteFile(file.getStoredFileName());
        } catch (Exception e) {
            // Log the error but continue with metadata deletion
            System.err.println("Failed to delete physical file: " + file.getStoredFileName() + ". Error: " + e.getMessage());
        }

        // 3. Delete the file metadata record from the database
        courseFileRepository.deleteById(fileId);
    }

    @Override
    public Optional<CourseFile> getFileMetadata(String fileId) {
        return courseFileRepository.findById(fileId);
    }

    @Override
    public List<CourseFile> getFilesByCategory(String categoryId) {
        return courseFileRepository.findByCategoryId(categoryId);
    }

    @Override
    public Page<CourseFile> getFilesByCategoryPaginated(String categoryId, Pageable pageable) {
        return courseFileRepository.findByCategoryId(categoryId, pageable);
    }

    @Override
    public boolean canUserAccessCategory(String categoryId, String userId, String userRole) {
        FileCategory category = fileCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Check if user belongs to the course
        Course course = courseRepository.findById(category.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if ("1100".equals(userRole)) return true; // Admin
        if ("1200".equals(userRole)) return course.getLecturerId().equals(userId); // Lecturer
        if ("1300".equals(userRole)) {
            // Check if student is enrolled for the academic year
            return course.getEnrollments().stream()
                    .anyMatch(enrollment -> enrollment.getAcademicYear() == category.getAcademicYear()
                            && enrollment.getStudentIds().contains(userId));
        }
        return false;
    }

    @Override
    public long getFilesCountByCategory(String categoryId) {
        return courseFileRepository.countByCategoryId(categoryId);
    }

    // --- Private Helper Methods ---

    private void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot store empty file");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File size exceeds maximum limit of 50MB");
        }

        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new FileStorageException("File type not allowed: " + contentType);
        }

        // Check filename for security issues
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new FileStorageException("Filename cannot be empty");
        }

        // Check for path traversal attempts
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new FileStorageException("Invalid filename: " + filename);
        }

        // Check for dangerous file extensions
        String lowerFilename = filename.toLowerCase();
        if (DANGEROUS_EXTENSIONS.stream().anyMatch(lowerFilename::endsWith)) {
            throw new FileStorageException("File extension not allowed for security reasons");
        }
    }
}