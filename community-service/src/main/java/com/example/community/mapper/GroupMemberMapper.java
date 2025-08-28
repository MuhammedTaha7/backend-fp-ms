package com.example.community.mapper;

import com.example.community.entity.GroupMembership;
import com.example.community.dto.GroupMemberDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GroupMemberMapper {

    @Autowired
    private UserMapper userMapper;

    public GroupMemberDto toDto(GroupMembership entity) {
        if (entity == null) return null;

        GroupMemberDto dto = new GroupMemberDto();
        dto.setId(entity.getId());

        // Map user information
        if (entity.getUser() != null) {
            dto.setUser(userMapper.toDto(entity.getUser()));
            dto.setUserId(entity.getUser().getId());
            dto.setName(entity.getUser().getName());
            dto.setProfilePic(entity.getUser().getProfilePic());
        }

        dto.setRole(entity.getRole());
        dto.setJoinDate(entity.getJoinDate());

        return dto;
    }

    public GroupMembership toEntity(GroupMemberDto dto) {
        if (dto == null) return null;

        GroupMembership entity = new GroupMembership();
        entity.setId(dto.getId());

        // Map user information
        if (dto.getUser() != null) {
            entity.setUser(userMapper.toEntity(dto.getUser()));
        }

        entity.setRole(dto.getRole());
        entity.setJoinDate(dto.getJoinDate());

        return entity;
    }
}