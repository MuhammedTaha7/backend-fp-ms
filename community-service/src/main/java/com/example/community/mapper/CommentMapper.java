package com.example.community.mapper;

import com.example.community.entity.Comment;
import com.example.community.dto.CommentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    @Autowired
    private UserMapper userMapper;

    public CommentDto toDto(Comment entity) {
        if (entity == null) return null;

        CommentDto dto = new CommentDto();
        dto.setId(entity.getId());
        dto.setPostId(entity.getPostId());
        dto.setUser(userMapper.toDto(entity.getUser()));
        dto.setText(entity.getText());
        dto.setUsername(entity.getUsername());
        dto.setProfilePicture(entity.getProfilePicture());
        dto.setCreatedAt(entity.getCreatedAt());

        return dto;
    }

    public Comment toEntity(CommentDto dto) {
        if (dto == null) return null;

        Comment entity = new Comment();
        entity.setId(dto.getId());
        entity.setPostId(dto.getPostId());
        entity.setUser(userMapper.toEntity(dto.getUser()));
        entity.setText(dto.getText());
        entity.setUsername(dto.getUsername());
        entity.setProfilePicture(dto.getProfilePicture());
        entity.setCreatedAt(dto.getCreatedAt());

        return entity;
    }
}