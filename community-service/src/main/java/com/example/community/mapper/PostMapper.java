package com.example.community.mapper;

import com.example.community.entity.Post;
import com.example.community.dto.PostDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    @Autowired
    private UserMapper userMapper;

    public PostDto toDto(Post entity) {
        if (entity == null) return null;

        PostDto dto = new PostDto();
        dto.setId(entity.getId());
        dto.setDesc(entity.getDesc());
        dto.setImg(entity.getImg());
        dto.setFile(entity.getFile());

        // Map user information - using cached fields from Post entity
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setName(entity.getName()); // Using cached name from Post
            dto.setProfilePic(entity.getProfilePic()); // Using cached profilePic from Post
            dto.setRole(entity.getRole()); // Using cached role from Post
        }

        // Group information
        dto.setGroupId(entity.getGroupId());
        dto.setGroupName(entity.getGroupName());

        // Engagement
        dto.setLikes(entity.getLikes());
        dto.setCommentCount(entity.getCommentCount());

        // Timestamps
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    public Post toEntity(PostDto dto) {
        if (dto == null) return null;

        Post entity = new Post();
        entity.setId(dto.getId());
        entity.setDesc(dto.getDesc());
        entity.setImg(dto.getImg());
        entity.setFile(dto.getFile());

        // Cached user info
        entity.setName(dto.getName());
        entity.setProfilePic(dto.getProfilePic());
        entity.setRole(dto.getRole());

        // Group info
        entity.setGroupId(dto.getGroupId());
        entity.setGroupName(dto.getGroupName());

        // Engagement
        entity.setLikes(dto.getLikes());
        entity.setCommentCount(dto.getCommentCount());

        // Timestamps
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());

        return entity;
    }
}