package com.example.community.mapper;

import com.example.community.entity.Activity;
import com.example.community.dto.ActivityDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class ActivityMapper {

    @Autowired
    private UserMapper userMapper;

    public ActivityDto toDto(Activity entity) {
        if (entity == null) return null;

        ActivityDto dto = new ActivityDto();
        dto.setId(entity.getId());
        dto.setUser(userMapper.toDto(entity.getUser()));
        dto.setType(entity.getType());
        dto.setAction(entity.getAction());
        dto.setTargetId(entity.getTargetId());
        dto.setTargetType(entity.getTargetType());
        dto.setCreatedAt(entity.getCreatedAt());

        // Cache user info for performance
        if (entity.getUser() != null) {
            dto.setName(entity.getUser().getName());
            dto.setProfilePic(entity.getUser().getProfilePic());
            dto.setImg(entity.getUser().getProfilePic()); // Alternative field name
        }

        // Format time ago
        if (entity.getCreatedAt() != null) {
            dto.setTime(formatTimeAgo(entity.getCreatedAt()));
        }

        return dto;
    }

    public Activity toEntity(ActivityDto dto) {
        if (dto == null) return null;

        Activity entity = new Activity();
        entity.setId(dto.getId());
        entity.setUser(userMapper.toEntity(dto.getUser()));
        entity.setType(dto.getType());
        entity.setAction(dto.getAction());
        entity.setTargetId(dto.getTargetId());
        entity.setTargetType(dto.getTargetType());
        entity.setCreatedAt(dto.getCreatedAt());

        return entity;
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "just now";
        }
    }
}