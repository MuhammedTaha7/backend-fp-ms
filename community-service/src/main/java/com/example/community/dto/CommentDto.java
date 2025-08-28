package com.example.community.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class CommentDto {
    private String id;
    private String postId;
    private UserDto user;
    private String text;
    private String username; // Cached for performance
    private String profilePicture; // Cached for performance
    private LocalDateTime createdAt;
}