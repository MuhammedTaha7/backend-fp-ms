package com.example.community.mapper;

import com.example.community.entity.Challenge;
import com.example.community.dto.ChallengeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChallengeMapper {

    @Autowired
    private UserMapper userMapper;

    public ChallengeDto toDto(Challenge entity) {
        if (entity == null) return null;

        ChallengeDto dto = new ChallengeDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory());
        dto.setType(entity.getType());
        dto.setDifficulty(entity.getDifficulty());
        dto.setPoints(entity.getPoints());
        dto.setCreator(userMapper.toDto(entity.getCreator()));
        dto.setCreatedAt(entity.getCreatedAt());

        return dto;
    }

    public Challenge toEntity(ChallengeDto dto) {
        if (dto == null) return null;

        Challenge entity = new Challenge();
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setCategory(dto.getCategory());
        entity.setType(dto.getType());
        entity.setDifficulty(dto.getDifficulty());
        entity.setPoints(dto.getPoints());
        entity.setCreator(userMapper.toEntity(dto.getCreator()));
        entity.setCreatedAt(dto.getCreatedAt());

        return entity;
    }
}