package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class StoryDto {
    private String id;
    private UserDto user;
    private String text;
    private String img;
    private String type;
    private String name; // Cached user name
    private String profilePic; // Cached user profile pic
    private String userId; // For convenience
    private String time; // Formatted time string (e.g., "2h ago")
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}