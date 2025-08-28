package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ActivityDto {
    private String id;
    private UserDto user;
    private String type;
    private String action;
    private String targetId;
    private String targetType;
    private String name; // Cached user name
    private String profilePic; // Cached user profile pic
    private String img; // Cached user profile pic (alternative name)
    private String time; // Formatted time string
    private LocalDateTime createdAt;
}