package com.example.community.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadResponse {
    private String fileName;
    private String fileUrl;
    private String fileType;
    private long fileSize;
    private String message;

    public FileUploadResponse() {}

    public FileUploadResponse(String fileName, String fileUrl, String fileType, long fileSize) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.message = "File uploaded successfully";
    }
}