package com.example.community.mapper;

import com.example.community.entity.JobPost;
import com.example.community.dto.JobDto;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public JobDto toDto(JobPost entity) {
        if (entity == null) {
            return null;
        }

        JobDto dto = new JobDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setCompany(entity.getCompany());
        dto.setLocation(entity.getLocation());
        dto.setDescription(entity.getDescription());
        dto.setType(entity.getType());
        dto.setTags(entity.getTags());
        dto.setSalary(entity.getSalary());
        dto.setRemote(entity.getRemote());
        dto.setExperience(entity.getExperience());
        dto.setBenefits(entity.getBenefits());
        dto.setRequirements(entity.getRequirements());
        dto.setStatus(entity.getStatus());

        // Handle deadline conversion
        if (entity.getDeadline() != null) {
            dto.setDeadline(entity.getDeadline().toString());
        }

        // Handle date conversions
        if (entity.getCreatedAt() != null) {
            dto.setPostedDate(entity.getCreatedAt().toString());
        }
        if (entity.getUpdatedAt() != null) {
            dto.setUpdatedAt(entity.getUpdatedAt().toString());
        }

        // Handle poster information
        if (entity.getPoster() != null) {
            dto.setPostedById(entity.getPoster().getId());
            dto.setPostedByName(entity.getPoster().getName());
        }

        return dto;
    }

    public JobPost toEntity(JobDto dto) {
        if (dto == null) {
            return null;
        }

        JobPost entity = new JobPost();
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setCompany(dto.getCompany());
        entity.setLocation(dto.getLocation());
        entity.setDescription(dto.getDescription());
        entity.setType(dto.getType());
        entity.setTags(dto.getTags());
        entity.setSalary(dto.getSalary());
        entity.setRemote(dto.getRemote());
        entity.setExperience(dto.getExperience());
        entity.setBenefits(dto.getBenefits());
        entity.setRequirements(dto.getRequirements());
        entity.setStatus(dto.getStatus());

        return entity;
    }
}