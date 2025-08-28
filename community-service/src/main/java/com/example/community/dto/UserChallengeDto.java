package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class UserChallengeDto {
    private String id;
    private ChallengeDto challenge;
    private String status;
    private String submissionLink;
    private String notes;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}