package com.example.community.mapper;

import com.example.community.entity.JobApplication;
import com.example.community.dto.JobApplicationDto;
import org.springframework.stereotype.Component;

@Component
public class JobApplicationMapper {

    public JobApplicationDto toDto(JobApplication entity) {
        if (entity == null) {
            return null;
        }

        JobApplicationDto dto = new JobApplicationDto();
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());
        dto.setApplicationLink(entity.getApplicationLink());
        dto.setMessage(entity.getMessage());
        dto.setAppliedAt(entity.getAppliedAt());
        dto.setReviewedAt(entity.getReviewedAt());
        dto.setReviewNotes(entity.getReviewNotes());

        // Map applicant
        if (entity.getApplicant() != null) {
            JobApplicationDto.ApplicantDto applicantDto = new JobApplicationDto.ApplicantDto();
            applicantDto.setId(entity.getApplicant().getId());
            applicantDto.setName(entity.getApplicant().getName());
            applicantDto.setEmail(entity.getApplicant().getEmail());
            applicantDto.setProfilePic(entity.getApplicant().getProfilePic());
            try {
                applicantDto.setPhoneNumber(entity.getApplicant().getPhoneNumber());
            } catch (Exception e) {
                applicantDto.setPhoneNumber(null);
            }
            dto.setApplicant(applicantDto);
        }

        // Map job
        if (entity.getJobPost() != null) {
            JobApplicationDto.JobSummaryDto jobDto = new JobApplicationDto.JobSummaryDto();
            jobDto.setId(entity.getJobPost().getId());
            jobDto.setTitle(entity.getJobPost().getTitle());
            jobDto.setCompany(entity.getJobPost().getCompany());
            dto.setJob(jobDto);
        }

        return dto;
    }

    public JobApplication toEntity(JobApplicationDto dto) {
        if (dto == null) {
            return null;
        }

        JobApplication entity = new JobApplication();
        entity.setId(dto.getId());
        entity.setStatus(dto.getStatus());
        entity.setApplicationLink(dto.getApplicationLink());
        entity.setMessage(dto.getMessage());
        entity.setAppliedAt(dto.getAppliedAt());
        entity.setReviewedAt(dto.getReviewedAt());
        entity.setReviewNotes(dto.getReviewNotes());

        return entity;
    }
}