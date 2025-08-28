package com.example.edusphere.service;

import com.example.edusphere.dto.request.FileUploadRequest;
import com.example.edusphere.dto.response.FileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    FileResponse uploadFileWithMetadata(MultipartFile file, FileUploadRequest fileMetadata, String uploaderId, String uploaderName);

    List<FileResponse> getAccessibleFiles(String userId, String userRole, String userDepartment);

    FileResponse getFileMetadata(String fileId);

    void deleteFile(String fileId, String deleterId, String userRole);
}