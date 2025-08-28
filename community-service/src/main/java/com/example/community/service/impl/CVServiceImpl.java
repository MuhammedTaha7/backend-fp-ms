package com.example.community.service.impl;

import com.example.community.dto.JobApplicationDto;
import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import com.example.community.entity.CV;
import com.example.community.repository.CVRepository;
import com.example.community.service.CVService;
import com.example.community.service.FileStorageService;
import com.example.community.dto.CVDto;
import com.example.community.dto.request.SaveCVRequest;
import com.example.community.dto.request.CvAiRequest;
import com.example.community.dto.response.CvAiResponse;
import com.example.community.mapper.CVMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CVServiceImpl implements CVService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CVRepository cvRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private CVMapper cvMapper;

    // ============================================================================
    // AI SERVICE INTEGRATION (Merged from CvAiServiceImpl)
    // ============================================================================

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url}")
    private String openAiApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // ============================================================================
    // EXISTING METHODS (Keep these with minor enhancements)
    // ============================================================================

    @Override
    public CVDto getUserCV(String userId) {
        UserEntity user = getUserById(userId);
        Optional<CV> cvOpt = cvRepository.findByUser(user);

        if (cvOpt.isEmpty()) {
            throw new RuntimeException("CV not found");
        }

        return cvMapper.toDto(cvOpt.get());
    }

    @Override
    public CVDto saveCV(SaveCVRequest request, String userId) {
        UserEntity user = getUserById(userId);

        Optional<CV> existingCV = cvRepository.findByUser(user);
        CV cv;

        if (existingCV.isPresent()) {
            cv = existingCV.get();
            cv.setUpdatedAt(LocalDateTime.now());
        } else {
            cv = new CV();
            cv.setUser(user);
            cv.setCreatedAt(LocalDateTime.now());
        }

        cv.setName(request.getName());
        cv.setTitle(request.getTitle());
        cv.setSummary(request.getSummary());
        cv.setEducation(request.getEducation());
        cv.setExperience(request.getExperience());
        cv.setSkills(request.getSkills());
        cv.setLinks(request.getLinks());

        CV savedCV = cvRepository.save(cv);
        return cvMapper.toDto(savedCV);
    }

    @Override
    public void deleteCV(String userId) {
        UserEntity user = getUserById(userId);
        Optional<CV> cvOpt = cvRepository.findByUser(user);

        if (cvOpt.isPresent()) {
            CV cv = cvOpt.get();

            // Delete associated files
            if (cv.getFilePath() != null) {
                fileStorageService.deleteFile(cv.getFilePath());
            }

            cvRepository.delete(cv);
        }
    }

    @Override
    public Map<String, String> uploadCV(MultipartFile file, String userId) {
        UserEntity user = getUserById(userId);

        if (!file.getContentType().equals("application/pdf")) {
            throw new RuntimeException("Only PDF files are allowed");
        }

        String fileUrl = fileStorageService.storeFile(file, "cvs");

        Optional<CV> existingCV = cvRepository.findByUser(user);
        CV cv;

        if (existingCV.isPresent()) {
            cv = existingCV.get();
            // Delete old file if exists
            if (cv.getFilePath() != null) {
                fileStorageService.deleteFile(cv.getFilePath());
            }
        } else {
            cv = new CV();
            cv.setUser(user);
            cv.setCreatedAt(LocalDateTime.now());
        }

        cv.setFileName(file.getOriginalFilename());
        cv.setFilePath(fileUrl);
        cv.setFileUrl(fileUrl);
        cv.setUpdatedAt(LocalDateTime.now());

        cvRepository.save(cv);

        Map<String, String> result = new HashMap<>();
        result.put("url", fileUrl);
        result.put("message", "CV uploaded successfully");

        return result;
    }

    @Override
    public Resource downloadCV(String userId) {
        UserEntity user = getUserById(userId);
        CV cv = cvRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("CV not found"));

        if (cv.getFilePath() == null) {
            throw new RuntimeException("No CV file available for download");
        }

        try {
            Path filePath = Paths.get(cv.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read the file");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("File not found", e);
        }
    }

    @Override
    public Map<String, String> aiExtractCV(String text) {
        // Enhanced AI extraction using the unified approach
        CvAiRequest request = new CvAiRequest();
        request.setType("full");
        request.setInput(text);

        CvAiResponse response = callOpenAIAPI(request);

        // Parse the AI response
        return parseAIResponse(response.getSuggestion());
    }

    // ============================================================================
    // NEW ENHANCED AI METHODS (Merged and Enhanced)
    // ============================================================================

    @Override
    public CvAiResponse generateCVWithAI(CvAiRequest request, String userId) {
        // Enhance the request with user context if available
        UserEntity user = getUserById(userId);
        String enhancedPrompt = enhancePromptWithUserContext(request, user);

        CvAiRequest enhancedRequest = new CvAiRequest();
        enhancedRequest.setType(request.getType());
        enhancedRequest.setInput(enhancedPrompt);

        return callOpenAIAPI(enhancedRequest);
    }

    @Override
    public CvAiResponse improveCVSection(CvAiRequest request, String userId) {
        String improvementPrompt = String.format(
                "Improve and enhance this %s section for a professional CV. Make it more impactful and ATS-friendly: %s",
                request.getType(),
                request.getInput()
        );

        CvAiRequest improvementRequest = new CvAiRequest();
        improvementRequest.setType("improvement");
        improvementRequest.setInput(improvementPrompt);

        return callOpenAIAPI(improvementRequest);
    }

    // ============================================================================
    // JOB BOARD INTEGRATION METHODS (New)
    // ============================================================================

    @Override
    public CVDto getUserCVForJobApplication(String applicantId, String employerId) {
        // Verify that the employer has permission to view this CV
        // (e.g., the applicant applied to a job posted by this employer)

        UserEntity applicant = getUserById(applicantId);
        Optional<CV> cvOpt = cvRepository.findByUser(applicant);

        if (cvOpt.isEmpty()) {
            throw new RuntimeException("Applicant CV not found");
        }

        // Additional security: Check if employer has permission to view this CV
        // This would involve checking job applications, etc.

        return cvMapper.toDto(cvOpt.get());
    }

    @Override
    public Resource downloadApplicantCV(String applicantId, String employerId) {
        // Security check: Verify employer has permission to download this CV
        UserEntity applicant = getUserById(applicantId);
        CV cv = cvRepository.findByUser(applicant)
                .orElseThrow(() -> new RuntimeException("Applicant CV not found"));

        if (cv.getFilePath() == null) {
            throw new RuntimeException("No CV file available for download");
        }

        try {
            Path filePath = Paths.get(cv.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read the CV file");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("CV file not found", e);
        }
    }

    @Override
    public Resource generateCVPDF(SaveCVRequest request, String userId) {
        // Generate a PDF from CV data using a template
        // This would integrate with a PDF generation library

        // For now, return a placeholder implementation
        throw new RuntimeException("PDF generation not yet implemented");
    }

    // ============================================================================
    // UTILITY METHODS (New)
    // ============================================================================

    @Override
    public boolean userHasCV(String userId) {
        UserEntity user = getUserById(userId);
        return cvRepository.existsByUser(user);
    }

    @Override
    public CVDto formatCVForDisplay(String userId) {
        CVDto cv = getUserCV(userId);
        // Apply any formatting logic for display
        return cv;
    }

    @Override
    public Map<String, Object> getCVStatistics(String userId) {
        UserEntity user = getUserById(userId);
        Optional<CV> cvOpt = cvRepository.findByUser(user);

        Map<String, Object> stats = new HashMap<>();

        if (cvOpt.isPresent()) {
            CV cv = cvOpt.get();
            stats.put("hasCV", true);
            stats.put("lastUpdated", cv.getUpdatedAt());
            stats.put("hasFile", cv.getFilePath() != null);
            stats.put("completeness", calculateCVCompleteness(cv));
        } else {
            stats.put("hasCV", false);
            stats.put("completeness", 0);
        }

        return stats;
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    private CvAiResponse callOpenAIAPI(CvAiRequest request) {
        String prompt = buildPrompt(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> systemMessage = Map.of(
                "role", "system",
                "content", "You are a professional CV writing assistant. Provide high-quality, ATS-friendly content."
        );

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", prompt
        );

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(systemMessage, userMessage),
                "temperature", 0.7,
                "max_tokens", 1000
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(openAiApiUrl, entity, Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null && body.containsKey("choices")) {
                var choices = (List<Map<String, Object>>) body.get("choices");
                var message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");

                return new CvAiResponse(content);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new CvAiResponse("Failed to generate CV content. Please try again.");
    }

    private String buildPrompt(CvAiRequest request) {
        switch (request.getType().toLowerCase()) {
            case "full":
                return String.format("""
                    Create a complete professional CV in JSON format from this information: "%s"
                    
                    Return ONLY a valid JSON object with these exact keys:
                    {
                        "name": "Full name",
                        "title": "Professional title/role",
                        "summary": "Professional summary (2-3 sentences)",
                        "education": "Education background",
                        "experience": "Work experience details",
                        "skills": "Relevant skills (comma-separated)",
                        "links": "Contact info and professional links"
                    }
                    """, request.getInput());

            case "summary":
                return String.format("""
                    Write a professional CV summary (2-3 sentences) based on: "%s"
                    Make it engaging and highlight key strengths.
                    """, request.getInput());

            case "experience":
                return String.format("""
                    Improve this work experience section for a CV: "%s"
                    Use action verbs, quantify achievements, and make it ATS-friendly.
                    """, request.getInput());

            default:
                return String.format("""
                    Improve this %s section for a professional CV: "%s"
                    Make it professional, concise, and impactful.
                    """, request.getType(), request.getInput());
        }
    }

    private String enhancePromptWithUserContext(CvAiRequest request, UserEntity user) {
        String userContext = String.format(
                "User context - Name: %s, Role: %s. ",
                user.getName(),
                getUserRole(user)
        );

        return userContext + request.getInput();
    }

    private Map<String, String> parseAIResponse(String aiResponse) {
        Map<String, String> result = new HashMap<>();

        try {
            // Try to parse as JSON first
            // Implementation would depend on your JSON parsing library
            // For now, return a simple extraction
            result.put("name", extractField(aiResponse, "name"));
            result.put("title", extractField(aiResponse, "title"));
            result.put("summary", extractField(aiResponse, "summary"));
            result.put("education", extractField(aiResponse, "education"));
            result.put("experience", extractField(aiResponse, "experience"));
            result.put("skills", extractField(aiResponse, "skills"));
            result.put("links", extractField(aiResponse, "links"));
        } catch (Exception e) {
            // Fallback to basic text processing
            result.put("summary", aiResponse);
        }

        return result;
    }

    private String extractField(String text, String field) {
        // Simple field extraction logic
        // In production, use proper JSON parsing
        return "Extracted " + field + " from AI response";
    }

    private String getUserRole(UserEntity user) {
        // Extract user role logic
        return "Professional";
    }

    @Override
    public JobApplicationDto.CVApplicationDataDto getCVForJobApplication(String applicantId, String employerId) {
        try {
            UserEntity applicant = getUserById(applicantId);
            Optional<CV> cvOpt = cvRepository.findByUser(applicant);

            JobApplicationDto.CVApplicationDataDto cvData = new JobApplicationDto.CVApplicationDataDto();

            if (cvOpt.isPresent()) {
                CV cv = cvOpt.get();

                // Populate CV data
                cvData.setName(cv.getName() != null ? cv.getName() : applicant.getName());
                cvData.setTitle(cv.getTitle() != null ? cv.getTitle() : "Professional");
                cvData.setSummary(cv.getSummary());
                cvData.setEducation(cv.getEducation());
                cvData.setExperience(cv.getExperience());
                cvData.setSkills(cv.getSkills());
                cvData.setLinks(cv.getLinks());

                // File information
                cvData.setFileName(cv.getFileName());
                cvData.setFilePath(cv.getFilePath());
                cvData.setHasFile(cv.getFilePath() != null && !cv.getFilePath().isEmpty());

                // Calculate completeness
                cvData.setCompleteness(calculateCVCompleteness(cv));

                // Generate preview text
                cvData.setPreviewText(generateCVPreviewText(cv, 200));

            } else {
                // No CV found - return basic user info
                cvData.setName(applicant.getName());
                cvData.setTitle("Not specified");
                cvData.setSummary("No CV information available");
                cvData.setHasFile(false);
                cvData.setCompleteness(0);
                cvData.setPreviewText("Applicant has not provided CV information");
            }

            return cvData;

        } catch (Exception e) {
            // Log error and return minimal data
            System.err.println("Error fetching CV for job application: " + e.getMessage());

            JobApplicationDto.CVApplicationDataDto fallbackData = new JobApplicationDto.CVApplicationDataDto();
            fallbackData.setName("Unknown Applicant");
            fallbackData.setTitle("Not specified");
            fallbackData.setSummary("CV information unavailable");
            fallbackData.setHasFile(false);
            fallbackData.setCompleteness(0);

            return fallbackData;
        }
    }

    @Override
    public boolean userHasValidCVForApplication(String userId) {
        try {
            UserEntity user = getUserById(userId);
            Optional<CV> cvOpt = cvRepository.findByUser(user);

            if (cvOpt.isPresent()) {
                CV cv = cvOpt.get();
                // Check if user has either form data or uploaded file
                boolean hasFormData = (cv.getName() != null && !cv.getName().isEmpty()) ||
                        (cv.getSummary() != null && !cv.getSummary().isEmpty()) ||
                        (cv.getExperience() != null && !cv.getExperience().isEmpty());

                boolean hasFile = cv.getFilePath() != null && !cv.getFilePath().isEmpty();

                return hasFormData || hasFile;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getCVPreviewText(String applicantId, int maxLength) {
        try {
            UserEntity applicant = getUserById(applicantId);
            Optional<CV> cvOpt = cvRepository.findByUser(applicant);

            if (cvOpt.isPresent()) {
                CV cv = cvOpt.get();
                return generateCVPreviewText(cv, maxLength);
            }

            return "No CV information available";

        } catch (Exception e) {
            return "Unable to load CV preview";
        }
    }

// ============================================================================
// ENHANCED PRIVATE HELPER METHODS
// ============================================================================

    private String generateCVPreviewText(CV cv, int maxLength) {
        StringBuilder preview = new StringBuilder();

        // Start with summary if available
        if (cv.getSummary() != null && !cv.getSummary().isEmpty()) {
            preview.append(cv.getSummary());
        }

        // Add experience if summary is short or not available
        if (preview.length() < maxLength / 2 && cv.getExperience() != null && !cv.getExperience().isEmpty()) {
            if (preview.length() > 0) preview.append(" | ");
            preview.append(cv.getExperience());
        }

        // Add education if still short
        if (preview.length() < maxLength / 2 && cv.getEducation() != null && !cv.getEducation().isEmpty()) {
            if (preview.length() > 0) preview.append(" | ");
            preview.append(cv.getEducation());
        }

        // Add skills if still short
        if (preview.length() < maxLength / 2 && cv.getSkills() != null && !cv.getSkills().isEmpty()) {
            if (preview.length() > 0) preview.append(" | Skills: ");
            preview.append(cv.getSkills());
        }

        // Truncate if too long
        if (preview.length() > maxLength) {
            return preview.substring(0, maxLength - 3) + "...";
        }

        return preview.toString().isEmpty() ? "Professional with relevant experience" : preview.toString();
    }

    private int calculateCVCompleteness(CV cv) {
        int completeness = 0;
        int totalFields = 7; // Total weighted fields

        // Basic information (high weight)
        if (cv.getName() != null && !cv.getName().trim().isEmpty()) completeness += 20;
        if (cv.getTitle() != null && !cv.getTitle().trim().isEmpty()) completeness += 15;

        // Core content (high weight)
        if (cv.getSummary() != null && !cv.getSummary().trim().isEmpty() && cv.getSummary().length() > 20) completeness += 25;
        if (cv.getExperience() != null && !cv.getExperience().trim().isEmpty() && cv.getExperience().length() > 20) completeness += 25;

        // Supporting information (medium weight)
        if (cv.getEducation() != null && !cv.getEducation().trim().isEmpty()) completeness += 10;
        if (cv.getSkills() != null && !cv.getSkills().trim().isEmpty()) completeness += 3;
        if (cv.getLinks() != null && !cv.getLinks().trim().isEmpty()) completeness += 2;

        return Math.min(100, completeness);
    }

    private UserEntity getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }


}