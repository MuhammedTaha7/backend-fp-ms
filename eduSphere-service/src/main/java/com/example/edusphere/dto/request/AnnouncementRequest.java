package com.example.edusphere.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementRequest {

    private String title;
    private String content;
    private String priority;
    private LocalDateTime expiryDate;
    private LocalDateTime scheduledDate;

    private String targetAudienceType;
    private String targetDepartment;
    private String targetCourseId;
    private Integer targetAcademicYear;
    private String targetUserId;

}