package com.example.community.mapper;

import com.example.community.entity.Story;
import com.example.community.dto.StoryDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class StoryMapper {

    @Autowired
    private UserMapper userMapper;

    public StoryDto toDto(Story entity) {
        if (entity == null) return null;

        StoryDto dto = new StoryDto();
        dto.setId(entity.getId());
        dto.setUser(userMapper.toDto(entity.getUser()));
        dto.setText(entity.getText());
        dto.setImg(entity.getImg());
        dto.setType(entity.getType());
        dto.setName(entity.getName());
        dto.setProfilePic(entity.getProfilePic());
        dto.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setExpiresAt(entity.getExpiresAt());

        // Format time ago
        if (entity.getCreatedAt() != null) {
            dto.setTime(formatTimeAgo(entity.getCreatedAt()));
        }

        return dto;
    }

    public Story toEntity(StoryDto dto) {
        if (dto == null) return null;

        Story entity = new Story();
        entity.setId(dto.getId());
        entity.setUser(userMapper.toEntity(dto.getUser()));
        entity.setText(dto.getText());
        entity.setImg(dto.getImg());
        entity.setType(dto.getType());
        entity.setName(dto.getName());
        entity.setProfilePic(dto.getProfilePic());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setExpiresAt(dto.getExpiresAt());

        return entity;
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (days > 0) {
            return days + "d ago";
        } else if (hours > 0) {
            return hours + "h ago";
        } else if (minutes > 0) {
            return minutes + "m ago";
        } else {
            return "just now";
        }
    }
}