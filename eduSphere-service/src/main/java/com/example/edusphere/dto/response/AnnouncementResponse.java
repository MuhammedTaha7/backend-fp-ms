package com.example.edusphere.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {

    private String id;
    private String title;
    private String content;
    private String creatorId;
    private String creatorName;
    private String priority;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiryDate;
    private LocalDateTime scheduledDate;

    private String targetAudienceType;
    private String targetDepartment;
    private String targetCourseId;
    private Integer targetAcademicYear;
    private String targetUserId;

}