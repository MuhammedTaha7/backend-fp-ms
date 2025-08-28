package com.example.community.repository;

import com.example.community.entity.AttachedFile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttachedFileRepository extends MongoRepository<AttachedFile, String> {

    // Find files by uploader
    List<AttachedFile> findByUploadedByOrderByUploadedAtDesc(String uploadedBy);

    // Find files by type
    List<AttachedFile> findByFileTypeOrderByUploadedAtDesc(String fileType);

    // Find files uploaded after date
    List<AttachedFile> findByUploadedAtAfter(LocalDateTime date);

    // Find files by mime type
    List<AttachedFile> findByMimeType(String mimeType);

    // Delete old files (cleanup)
    void deleteByUploadedAtBefore(LocalDateTime before);
}