package com.example.community.mapper;

import com.example.community.entity.CV;
import com.example.community.dto.CVDto;
import org.springframework.stereotype.Component;

@Component
public class CVMapper {

    public CVDto toDto(CV entity) {
        if (entity == null) return null;

        CVDto dto = new CVDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setTitle(entity.getTitle());
        dto.setSummary(entity.getSummary());
        dto.setEducation(entity.getEducation());
        dto.setExperience(entity.getExperience());
        dto.setSkills(entity.getSkills());
        dto.setLinks(entity.getLinks());
        dto.setFileName(entity.getFileName());
        dto.setFilePath(entity.getFilePath());
        dto.setFileUrl(entity.getFileUrl());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    public CV toEntity(CVDto dto) {
        if (dto == null) return null;

        CV entity = new CV();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setTitle(dto.getTitle());
        entity.setSummary(dto.getSummary());
        entity.setEducation(dto.getEducation());
        entity.setExperience(dto.getExperience());
        entity.setSkills(dto.getSkills());
        entity.setLinks(dto.getLinks());
        entity.setFileName(dto.getFileName());
        entity.setFilePath(dto.getFilePath());
        entity.setFileUrl(dto.getFileUrl());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());

        return entity;
    }
}