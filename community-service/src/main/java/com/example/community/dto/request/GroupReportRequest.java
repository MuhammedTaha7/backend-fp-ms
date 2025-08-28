package com.example.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupReportRequest {

    @NotBlank(message = "Reason is required")
    private String reason; // "spam", "harassment", "inappropriate_content", "fake_group", "other"

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description; // Additional details about the report

    private String evidenceUrl; // Optional: URL to evidence (screenshot, etc.)
    private Boolean anonymous; // Whether to report anonymously
}