package com.example.edusphere.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "announcements")
public class Announcement {

    @Id
    private String id;

    private String title;
    private String content;

    private String creatorId;
    private String creatorName;

    private String priority;
    private String status;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime expiryDate;
    private LocalDateTime scheduledDate;

    // The data type is now Integer to match the YearlyEnrollment entity
    private String targetAudienceType;
    private String targetDepartment;
    private String targetCourseId;
    private Integer targetAcademicYear;

    private String targetUserId;
}