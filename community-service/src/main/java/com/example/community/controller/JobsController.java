package com.example.community.controller;

import com.example.community.service.JobsService;
import com.example.common.service.UserService;
import com.example.community.dto.JobDto;
import com.example.community.dto.JobApplicationDto;
import com.example.community.dto.request.CreateJobRequest;
import com.example.community.dto.request.UpdateJobRequest;
import com.example.community.dto.request.ApplyToJobRequest;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class JobsController {

    @Autowired
    private JobsService jobsService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<JobDto>> getAllJobs(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String search) {
        List<JobDto> jobs = jobsService.getAllJobs(type, location, search);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/my-posts")
    public ResponseEntity<List<JobDto>> getMyPostedJobs(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        List<JobDto> jobs = jobsService.getJobsByPoster(userId);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/applied")
    public ResponseEntity<List<JobDto>> getAppliedJobs(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        List<JobDto> jobs = jobsService.getAppliedJobs(userId);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/saved")
    public ResponseEntity<List<JobDto>> getSavedJobs(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        List<JobDto> jobs = jobsService.getSavedJobs(userId);
        return ResponseEntity.ok(jobs);
    }

    @PostMapping
    public ResponseEntity<JobDto> createJob(@RequestBody CreateJobRequest request, Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        JobDto job = jobsService.createJob(request, userId);
        return ResponseEntity.ok(job);
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<JobDto> updateJob(
            @PathVariable String jobId,
            @RequestBody UpdateJobRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        JobDto job = jobsService.updateJob(jobId, request, userId);
        return ResponseEntity.ok(job);
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> deleteJob(@PathVariable String jobId, Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        jobsService.deleteJob(jobId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{jobId}/apply")
    public ResponseEntity<Void> applyToJob(
            @PathVariable String jobId,
            @RequestBody ApplyToJobRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        jobsService.applyToJob(jobId, request, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{jobId}/save")
    public ResponseEntity<Void> saveJob(@PathVariable String jobId, Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        jobsService.saveJob(jobId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{jobId}/save")
    public ResponseEntity<Void> unsaveJob(@PathVariable String jobId, Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        jobsService.unsaveJob(jobId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{jobId}/applications")
    public ResponseEntity<List<JobApplicationDto>> getJobApplications(
            @PathVariable String jobId,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();

        List<JobApplicationDto> applications = jobsService.getJobApplicationsWithCVData(jobId, userId);
        return ResponseEntity.ok(applications);
    }

    @PutMapping("/applications/{applicationId}/accept")
    public ResponseEntity<Void> acceptApplication(
            @PathVariable String applicationId,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        jobsService.acceptApplication(applicationId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/applications/{applicationId}/reject")
    public ResponseEntity<Void> rejectApplication(
            @PathVariable String applicationId,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        jobsService.rejectApplication(applicationId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cv/download/{applicantId}")
    public ResponseEntity<Resource> downloadApplicantCV(
            @PathVariable String applicantId,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();

        try {
            Resource resource = jobsService.downloadApplicantCV(applicantId, userId);

            String filename = String.format("cv_%s.pdf", applicantId);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .header("Content-Type", "application/pdf")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}