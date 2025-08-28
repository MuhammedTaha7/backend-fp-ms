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
@Document(collection = "file_categories")
@CompoundIndex(def = "{'courseId': 1, 'academicYear': 1}")
@CompoundIndex(def = "{'courseId': 1, 'academicYear': 1, 'name': 1}")
public class FileCategory {

    @Id
    private String id;

    @Field("name")
    @Indexed
    private String name; // Category name (e.g., "Lectures", "Assignments", "Resources")

    @Field("description")
    private String description; // Optional description of the category

    @Field("color")
    private String color; // Color for UI display (e.g., "#3B82F6")

    @Field("course_id")
    @Indexed
    private String courseId; // The course this category belongs to

    @Field("academic_year")
    @Indexed
    private int academicYear; // The academic year for this category

    @CreatedDate
    @Field("created_date")
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Field("last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Field("created_by")
    private String createdBy; // User ID who created the category

    @Field("display_order")
    private int displayOrder = 0; // Order for displaying categories

    @Field("is_active")
    private boolean isActive = true; // Soft delete flag

    @Field("file_count")
    private long fileCount = 0; // Cached count of files in this category

    // Helper method to check if category is empty
    public boolean isEmpty() {
        return fileCount == 0;
    }

    // Helper method to increment file count
    public void incrementFileCount() {
        this.fileCount++;
    }

    // Helper method to decrement file count
    public void decrementFileCount() {
        if (this.fileCount > 0) {
            this.fileCount--;
        }
    }
}