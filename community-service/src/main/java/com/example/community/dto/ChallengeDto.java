package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ChallengeDto {
    private String id;
    private String title;
    private String description;
    private String category;
    private String type;
    private String difficulty;
    private Integer points;
    private UserDto creator;
    private LocalDateTime createdAt;
}