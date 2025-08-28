package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class JobApplicationDto {
    private String id;
    private String status; // "PENDING", "REVIEWED", "ACCEPTED", "REJECTED"
    private String applicationLink;
    private String message;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    private String reviewNotes;

    // Applicant information
    private ApplicantDto applicant;

    // Job information
    private JobSummaryDto job;

    // CV data attached to application
    private CVApplicationDataDto cvData;

    @Getter
    @Setter
    public static class ApplicantDto {
        private String id;
        private String name;
        private String email;
        private String profilePic;
        private String phoneNumber;
    }

    @Getter
    @Setter
    public static class JobSummaryDto {
        private String id;
        private String title;
        private String company;
    }

    @Getter
    @Setter
    public static class CVApplicationDataDto {
        private String name;
        private String title;
        private String summary;
        private String education;
        private String experience;
        private String skills;
        private String links;

        // File information
        private String fileName;
        private String filePath;
        private boolean hasFile;

        // CV completeness indicator
        private int completeness;

        // Quick preview data
        private String previewText;
    }
}