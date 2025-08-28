package com.example.community.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateChallengeStatusRequest {
    private String status; // "NOT_STARTED", "IN_PROGRESS", "COMPLETED"
    private String submissionLink;
}