package com.example.community.service;

import com.example.community.dto.JobDto;
import com.example.community.dto.JobApplicationDto;
import com.example.community.dto.request.CreateJobRequest;
import com.example.community.dto.request.UpdateJobRequest;
import com.example.community.dto.request.ApplyToJobRequest;
import org.springframework.core.io.Resource;
import java.util.List;

public interface JobsService {

    // ============================================================================
    // JOB MANAGEMENT METHODS
    // ============================================================================

    /**
     * Get all jobs with optional filters
     */
    List<JobDto> getAllJobs(String type, String location, String search);

    /**
     * Get jobs posted by a specific user
     */
    List<JobDto> getJobsByPoster(String userId);

    /**
     * Get jobs applied to by a specific user
     */
    List<JobDto> getAppliedJobs(String userId);

    /**
     * Get jobs saved by a specific user
     */
    List<JobDto> getSavedJobs(String userId);

    /**
     * Create a new job posting
     */
    JobDto createJob(CreateJobRequest request, String userId);

    /**
     * Update an existing job posting
     */
    JobDto updateJob(String jobId, UpdateJobRequest request, String userId);

    /**
     * Delete a job posting
     */
    void deleteJob(String jobId, String userId);

    // ============================================================================
    // JOB APPLICATION METHODS
    // ============================================================================

    /**
     * Apply to a job
     */
    void applyToJob(String jobId, ApplyToJobRequest request, String userId);

    /**
     * Save a job for later
     */
    void saveJob(String jobId, String userId);

    /**
     * Remove a job from saved list
     */
    void unsaveJob(String jobId, String userId);

    // ============================================================================
    // APPLICATION MANAGEMENT METHODS
    // ============================================================================

    /**
     * Get job applications for a specific job (basic method)
     */
    List<JobApplicationDto> getJobApplications(String jobId, String userId);

    /**
     * Get job applications with enhanced CV data for employer review
     */
    List<JobApplicationDto> getJobApplicationsWithCVData(String jobId, String employerId);

    /**
     * Accept a job application
     */
    void acceptApplication(String applicationId, String userId);

    /**
     * Reject a job application
     */
    void rejectApplication(String applicationId, String userId);

    /**
     * Download applicant CV with proper security checks
     */
    Resource downloadApplicantCV(String applicantId, String employerId);
}