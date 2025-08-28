package com.example.community.mapper;

import com.example.community.entity.Group;
import com.example.community.dto.GroupDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GroupMapper {

    @Autowired
    private UserMapper userMapper;

    public GroupDto toDto(Group entity) {
        if (entity == null) return null;

        GroupDto dto = new GroupDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setImg(entity.getImg());
        dto.setType(entity.getType());
        dto.setFounder(userMapper.toDto(entity.getFounder()));
        dto.setMemberCount(entity.getMemberCount());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    public Group toEntity(GroupDto dto) {
        if (dto == null) return null;

        Group entity = new Group();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setImg(dto.getImg());
        entity.setType(dto.getType());
        entity.setFounder(userMapper.toEntity(dto.getFounder()));
        entity.setMemberCount(dto.getMemberCount());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());

        return entity;
    }
}