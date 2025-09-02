package com.example.community.service.impl;

import com.example.community.entity.JobPost;
import com.example.community.entity.JobApplication;
import com.example.community.repository.JobPostRepository;
import com.example.community.repository.JobApplicationRepository;
import com.example.community.service.JobsService;
import com.example.community.service.CVService;
import com.example.community.dto.JobDto;
import com.example.community.dto.JobApplicationDto;
import com.example.community.dto.request.CreateJobRequest;
import com.example.community.dto.request.UpdateJobRequest;
import com.example.community.dto.request.ApplyToJobRequest;
import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import com.example.common.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Optional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class JobsServiceImpl implements JobsService {

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CVService cvService;

    @Autowired
    private MailService mailService;

    // ============================================================================
    // JOB MANAGEMENT METHODS
    // ============================================================================

    @Override
    public List<JobDto> getAllJobs(String type, String location, String search) {
        List<JobPost> jobs = jobPostRepository.findByStatusOrderByCreatedAtDesc("Active");

        // Apply filters if provided
        if (search != null && !search.isEmpty()) {
            jobs = jobs.stream()
                    .filter(job -> job.getTitle().toLowerCase().contains(search.toLowerCase()) ||
                            job.getDescription().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (type != null && !type.equals("All")) {
            jobs = jobs.stream()
                    .filter(job -> job.getType().equals(type))
                    .collect(Collectors.toList());
        }

        if (location != null && !location.isEmpty()) {
            jobs = jobs.stream()
                    .filter(job -> job.getLocation().toLowerCase().contains(location.toLowerCase()))
                    .collect(Collectors.toList());
        }

        return jobs.stream().map(this::mapToJobDto).collect(Collectors.toList());
    }

    @Override
    public List<JobDto> getJobsByPoster(String userId) {
        UserEntity user = getUserById(userId);
        List<JobPost> jobs = jobPostRepository.findByPosterOrderByCreatedAtDesc(user);
        return jobs.stream().map(this::mapToJobDto).collect(Collectors.toList());
    }

    @Override
    public List<JobDto> getAppliedJobs(String userId) {
        UserEntity user = getUserById(userId);
        List<JobApplication> applications = jobApplicationRepository.findByApplicantOrderByAppliedAtDesc(user);
        return applications.stream()
                .map(app -> mapToJobDto(app.getJobPost()))
                .collect(Collectors.toList());
    }

    @Override
    public List<JobDto> getSavedJobs(String userId) {
        // Return empty list for now - implement based on your SavedJob entity if exists
        return new ArrayList<>();
    }

    @Override
    public JobDto createJob(CreateJobRequest request, String userId) {
        UserEntity user = getUserById(userId);

        JobPost job = new JobPost();
        job.setTitle(request.getTitle());
        job.setCompany(request.getCompany());
        job.setLocation(request.getLocation());
        job.setDescription(request.getDescription());
        job.setType(request.getType());
        job.setTags(request.getTags());
        job.setSalary(request.getSalary());
        job.setRemote(request.getRemote());
        job.setExperience(request.getExperience());
        job.setDeadline(parseDeadline(request.getDeadline())); // FIXED LINE
        job.setBenefits(request.getBenefits());
        job.setRequirements(request.getRequirements());
        job.setStatus(request.getStatus());
        job.setPoster(user);
        job.setCreatedAt(LocalDateTime.now());

        JobPost savedJob = jobPostRepository.save(job);
        return mapToJobDto(savedJob);
    }

    @Override
    public JobDto updateJob(String jobId, UpdateJobRequest request, String userId) {
        JobPost job = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Verify ownership
        if (!job.getPoster().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to update this job");
        }

        // Update fields
        job.setTitle(request.getTitle());
        job.setCompany(request.getCompany());
        job.setLocation(request.getLocation());
        job.setDescription(request.getDescription());
        job.setType(request.getType());
        job.setTags(request.getTags());
        job.setSalary(request.getSalary());
        job.setRemote(request.getRemote());
        job.setExperience(request.getExperience());
        job.setDeadline(parseDeadline(request.getDeadline())); // FIXED LINE
        job.setBenefits(request.getBenefits());
        job.setRequirements(request.getRequirements());
        job.setStatus(request.getStatus());
        job.setUpdatedAt(LocalDateTime.now());

        JobPost updatedJob = jobPostRepository.save(job);
        return mapToJobDto(updatedJob);
    }

    @Override
    public void deleteJob(String jobId, String userId) {
        JobPost job = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Verify ownership
        if (!job.getPoster().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this job");
        }

        // Delete associated job applications
        List<JobApplication> applications = jobApplicationRepository.findByJobPost(job);
        jobApplicationRepository.deleteAll(applications);

        // Delete the job
        jobPostRepository.delete(job);
    }

    // ============================================================================
    // JOB APPLICATION METHODS
    // ============================================================================

    @Override
    public void applyToJob(String jobId, ApplyToJobRequest request, String userId) {
        JobPost job = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        UserEntity applicant = getUserById(userId);

        // Check if already applied
        if (jobApplicationRepository.existsByJobPostAndApplicant(job, applicant)) {
            throw new RuntimeException("You have already applied to this job");
        }

        // Check if user has CV
        if (!cvService.userHasValidCVForApplication(userId)) {
            throw new RuntimeException("CV not found. Please create your CV before applying.");
        }

        JobApplication application = new JobApplication();
        application.setJobPost(job);
        application.setApplicant(applicant);
        application.setApplicationLink(request.getApplicationLink());
        application.setMessage(request.getMessage());
        application.setStatus("PENDING");
        application.setAppliedAt(LocalDateTime.now());

        // Attach CV data if provided in request
        if (request.getCvData() != null) {
            application.setCvData(request.getCvData());
        }

        jobApplicationRepository.save(application);
    }

    @Override
    public void saveJob(String jobId, String userId) {
        // Implement save job logic if you have SavedJob entity
        // For now, this is a placeholder
    }

    @Override
    public void unsaveJob(String jobId, String userId) {
        // Implement unsave job logic if you have SavedJob entity
        // For now, this is a placeholder
    }

    // ============================================================================
    // APPLICATION MANAGEMENT METHODS
    // ============================================================================

    @Override
    public List<JobApplicationDto> getJobApplications(String jobId, String userId) {
        return getJobApplicationsWithCVData(jobId, userId);
    }

    @Override
    public List<JobApplicationDto> getJobApplicationsWithCVData(String jobId, String employerId) {
        // Verify job ownership
        JobPost job = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getPoster().getId().equals(employerId)) {
            throw new RuntimeException("Unauthorized access to job applications");
        }

        // Get all applications for this job
        List<JobApplication> applications = jobApplicationRepository.findByJobPost(job);

        return applications.stream().map(application -> {
            JobApplicationDto dto = mapToJobApplicationDto(application);

            // Enhanced CV data mapping
            try {
                JobApplicationDto.CVApplicationDataDto cvData = cvService.getCVForJobApplication(
                        application.getApplicant().getId(),
                        employerId
                );
                dto.setCvData(cvData);
            } catch (Exception e) {
                // Fallback CV data
                JobApplicationDto.CVApplicationDataDto fallbackCvData = new JobApplicationDto.CVApplicationDataDto();
                fallbackCvData.setName(application.getApplicant().getName());
                fallbackCvData.setTitle("Not specified");
                fallbackCvData.setSummary("CV information unavailable");
                fallbackCvData.setHasFile(false);
                fallbackCvData.setCompleteness(0);
                dto.setCvData(fallbackCvData);
            }

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public void acceptApplication(String applicationId, String userId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Verify job ownership
        if (!application.getJobPost().getPoster().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to manage this application");
        }

        // Update application status
        application.setStatus("ACCEPTED");
        application.setReviewedAt(LocalDateTime.now());
        jobApplicationRepository.save(application);

        // SEND ACCEPTANCE EMAIL
        try {
            sendAcceptanceEmail(application);
        } catch (Exception e) {
            // Log error but don't fail the whole operation
            System.err.println("Failed to send acceptance email: " + e.getMessage());
        }
    }

    @Override
    public void rejectApplication(String applicationId, String userId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Verify job ownership
        if (!application.getJobPost().getPoster().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to manage this application");
        }

        // Update application status
        application.setStatus("REJECTED");
        application.setReviewedAt(LocalDateTime.now());
        jobApplicationRepository.save(application);

        // SEND REJECTION EMAIL
        try {
            sendRejectionEmail(application);
        } catch (Exception e) {
            // Log error but don't fail the whole operation
            System.err.println("Failed to send rejection email: " + e.getMessage());
        }
    }

    @Override
    public Resource downloadApplicantCV(String applicantId, String employerId) {
        // Security check: Verify employer has permission to download this CV
        boolean hasPermission = jobApplicationRepository.existsByApplicantIdAndJobPostPostedById(
                applicantId, employerId
        );

        if (!hasPermission) {
            throw new RuntimeException("Unauthorized access to applicant CV");
        }

        return cvService.downloadApplicantCV(applicantId, employerId);
    }

    // ============================================================================
    // EMAIL NOTIFICATION METHODS
    // ============================================================================

    private void sendAcceptanceEmail(JobApplication application) {
        String applicantEmail = application.getApplicant().getEmail();
        String applicantName = application.getApplicant().getName();
        String jobTitle = application.getJobPost().getTitle();
        String companyName = application.getJobPost().getCompany();
        String employerName = application.getJobPost().getPoster().getName();

        String subject = "🎉 Congratulations! Your Job Application Has Been Accepted";

        String emailBody = String.format(
                "Dear %s,\n\n" +
                        "Great news! We are pleased to inform you that your application for the position of '%s' at %s has been ACCEPTED! 🎉\n\n" +
                        "We were impressed with your qualifications and believe you would be a great fit for our team.\n\n" +
                        "📋 Job Details:\n" +
                        "• Position: %s\n" +
                        "• Company: %s\n" +
                        "• Applied Date: %s\n\n" +
                        "📞 Next Steps:\n" +
                        "Our hiring team will contact you within the next 2-3 business days to discuss the next steps in the hiring process. Please keep an eye on your email and phone for our communication.\n\n" +
                        "If you have any questions or concerns, feel free to reply to this email or contact us directly.\n\n" +
                        "We look forward to having you join our team!\n\n" +
                        "Best regards,\n" +
                        "%s\n" +
                        "%s\n\n" +
                        "---\n" +
                        "This is an automated message from our Job Board system.\n" +
                        "Please do not reply directly to this email.",

                applicantName,
                jobTitle,
                companyName,
                jobTitle,
                companyName,
                application.getAppliedAt().toLocalDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                employerName,
                companyName
        );

        mailService.sendMail(applicantEmail, subject, emailBody);
    }

    private void sendRejectionEmail(JobApplication application) {
        String applicantEmail = application.getApplicant().getEmail();
        String applicantName = application.getApplicant().getName();
        String jobTitle = application.getJobPost().getTitle();
        String companyName = application.getJobPost().getCompany();
        String employerName = application.getJobPost().getPoster().getName();

        String subject = "Thank You for Your Application - " + jobTitle + " at " + companyName;

        String emailBody = String.format(
                "Dear %s,\n\n" +
                        "Thank you for taking the time to apply for the position of '%s' at %s and for your interest in joining our team.\n\n" +
                        "📋 Job Details:\n" +
                        "• Position: %s\n" +
                        "• Company: %s\n" +
                        "• Applied Date: %s\n\n" +
                        "After careful consideration, we have decided to move forward with other candidates whose qualifications more closely match our current needs for this specific role.\n\n" +
                        "💡 This decision does not reflect on your qualifications or potential. We encourage you to:\n" +
                        "• Continue checking our job board for future opportunities\n" +
                        "• Apply for other positions that match your skills and interests\n" +
                        "• Keep building your experience and skills\n\n" +
                        "We truly appreciate the time and effort you put into your application, and we wish you the very best in your job search and future career endeavors.\n\n" +
                        "Thank you again for considering %s as your potential employer.\n\n" +
                        "Best regards,\n" +
                        "%s\n" +
                        "%s\n\n" +
                        "---\n" +
                        "This is an automated message from our Job Board system.\n" +
                        "Please do not reply directly to this email.",

                applicantName,
                jobTitle,
                companyName,
                jobTitle,
                companyName,
                application.getAppliedAt().toLocalDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                companyName,
                employerName,
                companyName
        );

        mailService.sendMail(applicantEmail, subject, emailBody);
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    private JobDto mapToJobDto(JobPost job) {
        JobDto dto = new JobDto();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setCompany(job.getCompany());
        dto.setLocation(job.getLocation());
        dto.setDescription(job.getDescription());
        dto.setType(job.getType());
        dto.setTags(job.getTags());
        dto.setSalary(job.getSalary());
        dto.setRemote(job.getRemote());
        dto.setExperience(job.getExperience());
        dto.setDeadline(formatDeadline(job.getDeadline()));
        dto.setBenefits(job.getBenefits());
        dto.setRequirements(job.getRequirements());
        dto.setStatus(job.getStatus());

        // Convert LocalDateTime to String
        dto.setPostedDate(job.getCreatedAt() != null ? job.getCreatedAt().toString() : null);
        dto.setUpdatedAt(job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : null);

        // Set poster information
        if (job.getPoster() != null) {
            dto.setPostedById(job.getPoster().getId());
            dto.setPostedByName(job.getPoster().getName());
        }

        return dto;
    }

    private JobApplicationDto mapToJobApplicationDto(JobApplication application) {
        JobApplicationDto dto = new JobApplicationDto();

        // Basic application data
        dto.setId(application.getId());
        dto.setStatus(application.getStatus());
        dto.setApplicationLink(application.getApplicationLink());
        dto.setMessage(application.getMessage());
        dto.setAppliedAt(application.getAppliedAt());
        dto.setReviewedAt(application.getReviewedAt());
        dto.setReviewNotes(application.getReviewNotes());

        // Applicant information
        JobApplicationDto.ApplicantDto applicantDto = new JobApplicationDto.ApplicantDto();
        applicantDto.setId(application.getApplicant().getId());
        applicantDto.setName(application.getApplicant().getName());
        applicantDto.setEmail(application.getApplicant().getEmail());
        applicantDto.setProfilePic(application.getApplicant().getProfilePic());
        // Handle phoneNumber safely - only set if method exists
        try {
            applicantDto.setPhoneNumber(application.getApplicant().getPhoneNumber());
        } catch (Exception e) {
            applicantDto.setPhoneNumber(null);
        }
        dto.setApplicant(applicantDto);

        // Job summary
        JobApplicationDto.JobSummaryDto jobDto = new JobApplicationDto.JobSummaryDto();
        jobDto.setId(application.getJobPost().getId());
        jobDto.setTitle(application.getJobPost().getTitle());
        jobDto.setCompany(application.getJobPost().getCompany());
        dto.setJob(jobDto);

        return dto;
    }

    private LocalDate parseDeadline(String deadlineString) {
        if (deadlineString == null || deadlineString.trim().isEmpty()) {
            return null;
        }

        try {
            // Try to parse as yyyy-MM-dd format (common from frontend date inputs)
            return LocalDate.parse(deadlineString, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            try {
                // Try MM/dd/yyyy format
                return LocalDate.parse(deadlineString, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            } catch (DateTimeParseException e2) {
                try {
                    // Try dd/MM/yyyy format
                    return LocalDate.parse(deadlineString, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } catch (DateTimeParseException e3) {
                    try {
                        // Try yyyy/MM/dd format
                        return LocalDate.parse(deadlineString, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                    } catch (DateTimeParseException e4) {
                        // If all parsing fails, log error and return null
                        System.err.println("Unable to parse deadline date: " + deadlineString);
                        return null;
                    }
                }
            }
        }
    }

    private String formatDeadline(LocalDate deadline) {
        if (deadline == null) {
            return null;
        }

        // Format as yyyy-MM-dd for frontend consistency
        return deadline.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private UserEntity getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}