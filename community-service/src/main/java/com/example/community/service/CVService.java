package com.example.community.service;

import com.example.community.dto.CVDto;
import com.example.community.dto.JobApplicationDto;
import com.example.community.dto.request.SaveCVRequest;
import com.example.community.dto.request.CvAiRequest;
import com.example.community.dto.response.CvAiResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface CVService {

    CVDto getUserCV(String userId);
    CVDto saveCV(SaveCVRequest request, String userId);
    void deleteCV(String userId);
    Map<String, String> uploadCV(MultipartFile file, String userId);
    Resource downloadCV(String userId);
    Map<String, String> aiExtractCV(String text);
    CvAiResponse generateCVWithAI(CvAiRequest request, String userId);
    CvAiResponse improveCVSection(CvAiRequest request, String userId);
    CVDto getUserCVForJobApplication(String applicantId, String employerId);
    Resource downloadApplicantCV(String applicantId, String employerId);
    Resource generateCVPDF(SaveCVRequest request, String userId);
    JobApplicationDto.CVApplicationDataDto getCVForJobApplication(String applicantId, String employerId);
    boolean userHasValidCVForApplication(String userId);
    String getCVPreviewText(String applicantId, int maxLength);
    boolean userHasCV(String userId);
    CVDto formatCVForDisplay(String userId);
    Map<String, Object> getCVStatistics(String userId);
}