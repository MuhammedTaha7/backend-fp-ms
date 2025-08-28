package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class GroupActivityDto {
    private String id;
    private String groupId;
    private String groupName;
    private String activityType; // "member_joined", "post_created", "group_updated", etc.
    private UserDto actor; // Who performed the action
    private String description; // Human-readable description
    private LocalDateTime createdAt;
    private Object metadata; // Additional data like post info, etc.
}