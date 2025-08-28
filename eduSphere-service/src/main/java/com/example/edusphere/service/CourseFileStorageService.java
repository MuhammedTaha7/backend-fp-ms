package com.example.edusphere.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface CourseFileStorageService {

    /**
     * Stores a file on the server.
     * @param file The file uploaded by the user.
     * @return The unique filename under which the file was stored.
     */
    String storeFile(MultipartFile file);

    /**
     * Loads a file from the server as a resource for downloading.
     * @param filename The unique name of the file to load.
     * @return The file as a downloadable Resource.
     */
    Resource loadFileAsResource(String filename);

    /**
     * Deletes a file from the server.
     * @param filename The unique name of the file to delete.
     */
    void deleteFile(String filename);

    /**
     * Checks if a file exists on the server.
     * @param filename The unique name of the file to check.
     * @return true if the file exists, false otherwise.
     */
    boolean fileExists(String filename);

    /**
     * Gets the size of a file in bytes.
     * @param filename The unique name of the file.
     * @return The file size in bytes, or 0 if file doesn't exist.
     */
    long getFileSize(String filename);
}