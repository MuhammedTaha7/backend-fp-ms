package com.example.community.service.impl;

import com.example.community.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public String storeFile(MultipartFile file, String folder) {
        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir, folder);
            Files.createDirectories(uploadPath);

            // Generate unique filename
            String fileName = generateFileName(file.getOriginalFilename());
            Path filePath = uploadPath.resolve(fileName);

            // Copy file to upload directory
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return URL
            return baseUrl + "/uploads/" + folder + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    @Override
    public String generateFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }
}