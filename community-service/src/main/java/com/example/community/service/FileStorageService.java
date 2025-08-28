package com.example.community.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file, String folder);
    void deleteFile(String filePath);
    String generateFileName(String originalFileName);
}