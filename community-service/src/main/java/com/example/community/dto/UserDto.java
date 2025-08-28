package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class UserDto {
    private String id;
    private String username;
    private String email;
    private String name;
    private String role;
    private String profilePic;
    private String coverPic;
    private String title;
    private String university;
    private String bio;
    private String location;
    private String website;
    private Map<String, String> socialLinks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}