package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class GroupMemberDto {
    private String id;
    private UserDto user;
    private String role;
    private LocalDateTime joinDate;

    // Cached fields for convenience
    private String userId;
    private String name;
    private String profilePic;
}