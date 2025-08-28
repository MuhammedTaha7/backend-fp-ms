package com.example.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class PostDto {
    private String id;
    private String userId;
    private String desc;
    private String img;
    private Map<String, String> file;

    // Cached user info (from Post entity, not UserDto object)
    private String name;
    private String profilePic;
    private String role;

    // Group info
    private String groupId;
    private String groupName;

    // Engagement
    private List<String> likes;
    private Integer commentCount;

    // Timestamps with proper formatting
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // Helper methods for compatibility
    public String getCreatedAtISO() {
        return createdAt != null ? createdAt.toString() : null;
    }

    public String getUpdatedAtISO() {
        return updatedAt != null ? updatedAt.toString() : null;
    }
}