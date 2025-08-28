package com.example.community.mapper;

import com.example.community.entity.UserChallenge;
import com.example.community.dto.UserChallengeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserChallengeMapper {

    @Autowired
    private ChallengeMapper challengeMapper;

    public UserChallengeDto toDto(UserChallenge entity) {
        if (entity == null) return null;

        UserChallengeDto dto = new UserChallengeDto();
        dto.setId(entity.getId());
        dto.setChallenge(challengeMapper.toDto(entity.getChallenge()));
        dto.setStatus(entity.getStatus());
        dto.setSubmissionLink(entity.getSubmissionLink());
        dto.setNotes(entity.getNotes());
        dto.setStartedAt(entity.getStartedAt());
        dto.setCompletedAt(entity.getCompletedAt());

        return dto;
    }

    public UserChallenge toEntity(UserChallengeDto dto) {
        if (dto == null) return null;

        UserChallenge entity = new UserChallenge();
        entity.setId(dto.getId());
        entity.setChallenge(challengeMapper.toEntity(dto.getChallenge()));
        entity.setStatus(dto.getStatus());
        entity.setSubmissionLink(dto.getSubmissionLink());
        entity.setNotes(dto.getNotes());
        entity.setStartedAt(dto.getStartedAt());
        entity.setCompletedAt(dto.getCompletedAt());

        return entity;
    }
}