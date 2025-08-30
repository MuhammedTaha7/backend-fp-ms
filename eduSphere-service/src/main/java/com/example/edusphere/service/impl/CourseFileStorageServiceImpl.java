package com.example.edusphere.service.impl;

import com.example.common.exceptions.FileStorageException;
import com.example.edusphere.service.CourseFileStorageService;
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
import java.util.UUID;
import java.util.Set;

@Service
public class CourseFileStorageServiceImpl implements CourseFileStorageService {

    private final Path fileStorageLocation;

    // Security: Define allowed file extensions
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".jpg", ".jpeg", ".png", ".gif", ".mp4", ".mp3",
            ".zip", ".rar", ".csv"
    );

    public CourseFileStorageServiceImpl(@Value("${app.upload.dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            // Create the upload directory if it doesn't exist
            Files.createDirectories(this.fileStorageLocation);

            // Log the upload directory for debugging

        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory for uploads: " + uploadDir, ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Validate filename
            if (originalFileName.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence: " + originalFileName);
            }

            if (originalFileName.isEmpty()) {
                throw new FileStorageException("Cannot store file with empty name");
            }

            // Check file extension
            String fileExtension = getFileExtension(originalFileName);
            if (!ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase())) {
                throw new FileStorageException("File extension not allowed: " + fileExtension);
            }

            // Generate unique filename
            String uniqueFileName = generateUniqueFileName(originalFileName);

            // Resolve target location
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);

            // Ensure the target location is within the upload directory
            if (!targetLocation.toAbsolutePath().normalize().startsWith(this.fileStorageLocation)) {
                throw new FileStorageException("Cannot store file outside upload directory");
            }

            // Copy file to target location
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Verify file was stored successfully
            if (!Files.exists(targetLocation)) {
                throw new FileStorageException("File was not stored successfully: " + uniqueFileName);
            }

            return uniqueFileName;

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String filename) {
        try {
            // Clean the filename
            String cleanFilename = StringUtils.cleanPath(filename);

            // Validate filename
            if (cleanFilename.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence: " + filename);
            }

            Path filePath = this.fileStorageLocation.resolve(cleanFilename).normalize();

            // Ensure the file is within the upload directory
            if (!filePath.toAbsolutePath().startsWith(this.fileStorageLocation)) {
                throw new FileStorageException("Cannot access file outside upload directory");
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("File not found or not readable: " + filename);
            }

        } catch (MalformedURLException ex) {
            throw new FileStorageException("File not found: " + filename, ex);
        }
    }

    @Override
    public void deleteFile(String filename) {
        try {
            // Clean the filename
            String cleanFilename = StringUtils.cleanPath(filename);

            // Validate filename
            if (cleanFilename.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence: " + filename);
            }

            Path filePath = this.fileStorageLocation.resolve(cleanFilename).normalize();

            // Ensure the file is within the upload directory
            if (!filePath.toAbsolutePath().startsWith(this.fileStorageLocation)) {
                throw new FileStorageException("Cannot delete file outside upload directory");
            }

            boolean deleted = Files.deleteIfExists(filePath);

        } catch (IOException ex) {
            throw new FileStorageException("Could not delete file: " + filename, ex);
        }
    }

    @Override
    public boolean fileExists(String filename) {
        try {
            String cleanFilename = StringUtils.cleanPath(filename);

            if (cleanFilename.contains("..")) {
                return false;
            }

            Path filePath = this.fileStorageLocation.resolve(cleanFilename).normalize();

            if (!filePath.toAbsolutePath().startsWith(this.fileStorageLocation)) {
                return false;
            }

            return Files.exists(filePath);

        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public long getFileSize(String filename) {
        try {
            String cleanFilename = StringUtils.cleanPath(filename);
            Path filePath = this.fileStorageLocation.resolve(cleanFilename).normalize();

            if (Files.exists(filePath)) {
                return Files.size(filePath);
            }

            return 0;

        } catch (IOException ex) {
            throw new FileStorageException("Could not get file size: " + filename, ex);
        }
    }

    // Private helper methods

    private String generateUniqueFileName(String originalFileName) {
        String fileExtension = getFileExtension(originalFileName);
        String baseName = getBaseName(originalFileName);

        // Generate UUID and combine with original filename
        String uuid = UUID.randomUUID().toString();

        // Limit base name length to prevent filesystem issues
        if (baseName.length() > 50) {
            baseName = baseName.substring(0, 50);
        }

        return uuid + "-" + baseName + fileExtension;
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }

    private String getBaseName(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(0, filename.lastIndexOf("."));
        }
        return filename != null ? filename : "";
    }
}