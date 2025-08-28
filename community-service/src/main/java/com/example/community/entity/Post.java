package com.example.community.entity;

import com.example.common.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Document(collection = "posts")
public class Post {

    @Id
    private String id;

    @DBRef
    private UserEntity user;

    private String desc; // Post description/content
    private String img; // Image URL

    // File attachment info
    private Map<String, String> file; // {"name": "filename", "url": "file_url"}

    // Cached user info for performance
    private String name;
    private String profilePic;
    private String role;

    // Group info (if this is a group post)
    private String groupId;
    private String groupName;

    // Engagement
    private List<String> likes; // List of user IDs who liked
    private Integer commentCount = 0; // Cache for performance

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}