package com.example.edusphere.service;

import com.example.edusphere.dto.request.CreateCategoryRequest;
import com.example.edusphere.entity.CourseFile;
import com.example.edusphere.entity.FileCategory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface CourseContentService {

    // --- Category Methods ---
    FileCategory createCategory(String courseId, int year, CreateCategoryRequest request);
    List<FileCategory> getCategoriesByCourse(String courseId, int year);
    FileCategory updateCategory(String categoryId, FileCategory categoryDetails);
    void deleteCategory(String categoryId);

    // --- File Methods ---
    CourseFile storeFile(String categoryId, MultipartFile file);
    void deleteFile(String fileId);
    Optional<CourseFile> getFileMetadata(String fileId);
    List<CourseFile> getFilesByCategory(String categoryId);
    Page<CourseFile> getFilesByCategoryPaginated(String categoryId, Pageable pageable);
    long getFilesCountByCategory(String categoryId);

    // --- Access Control Methods ---
    boolean canUserAccessCategory(String categoryId, String userId, String userRole);
}