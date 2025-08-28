package com.example.community.mapper;

import com.example.common.entity.UserEntity;
import com.example.community.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(UserEntity entity) {
        if (entity == null) return null;

        UserDto dto = new UserDto();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setName(entity.getName());
        dto.setRole(entity.getRole());
        dto.setProfilePic(entity.getProfilePic());
        dto.setCoverPic(entity.getCoverPic());
        dto.setTitle(entity.getTitle());
        dto.setUniversity(entity.getUniversity());
        dto.setBio(entity.getBio());
        dto.setWebsite(entity.getWebsite());
        dto.setSocialLinks(entity.getSocialLinks());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    public UserEntity toEntity(UserDto dto) {
        if (dto == null) return null;

        UserEntity entity = new UserEntity();
        entity.setId(dto.getId());
        entity.setUsername(dto.getUsername());
        entity.setEmail(dto.getEmail());
        entity.setName(dto.getName());
        entity.setRole(dto.getRole());
        entity.setProfilePic(dto.getProfilePic());
        entity.setCoverPic(dto.getCoverPic());
        entity.setTitle(dto.getTitle());
        entity.setUniversity(dto.getUniversity());
        entity.setBio(dto.getBio());
        entity.setWebsite(dto.getWebsite());
        entity.setSocialLinks(dto.getSocialLinks());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());

        return entity;
    }
}