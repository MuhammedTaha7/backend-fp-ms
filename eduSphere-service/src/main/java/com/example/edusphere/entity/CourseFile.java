package com.example.edusphere.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "course_files")
@CompoundIndex(def = "{'categoryId': 1, 'uploadDate': -1}")
@CompoundIndex(def = "{'categoryId': 1, 'fileName': 1}")
public class CourseFile {

    @Id
    private String id;

    @Field("file_name")
    @Indexed
    private String fileName; // The original name of the file, e.g., "lecture1.pdf"

    @Field("stored_file_name")
    private String storedFileName; // The unique name on the server, e.g., "uuid-lecture1.pdf"

    @Field("file_type")
    @Indexed
    private String fileType; // The MIME type, e.g., "application/pdf"

    @Field("size")
    private long size; // File size in bytes

    @CreatedDate
    @Field("upload_date")
    @Indexed
    private LocalDateTime uploadDate;

    @LastModifiedDate
    @Field("last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Field("category_id")
    @Indexed
    private String categoryId; // Crucial link to the FileCategory this file belongs to

    @Field("uploaded_by")
    private String uploadedBy; // User ID who uploaded the file

    @Field("file_description")
    private String fileDescription; // Optional description of the file

    @Field("download_count")
    private long downloadCount = 0; // Track how many times file has been downloaded

    @Field("is_active")
    private boolean isActive = true; // Soft delete flag

    // Helper method to get file extension
    public String getFileExtension() {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        }
        return "";
    }

    // Helper method to get file size in human readable format
    public String getFormattedSize() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    // Helper method to check if file is an image
    public boolean isImage() {
        return fileType != null && fileType.startsWith("image/");
    }

    // Helper method to check if file is a document
    public boolean isDocument() {
        return fileType != null && (
                fileType.equals("application/pdf") ||
                        fileType.equals("application/msword") ||
                        fileType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                        fileType.equals("application/vnd.ms-excel") ||
                        fileType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                        fileType.equals("application/vnd.ms-powerpoint") ||
                        fileType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation") ||
                        fileType.equals("text/plain")
        );
    }

    // Method to increment download count
    public void incrementDownloadCount() {
        this.downloadCount++;
    }
}