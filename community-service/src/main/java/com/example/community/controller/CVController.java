package com.example.community.controller;

import com.example.community.service.CVService;
import com.example.common.service.UserService; // Add this import
import com.example.community.dto.CVDto;
import com.example.community.dto.request.SaveCVRequest;
import com.example.community.dto.request.AIExtractRequest;
import com.example.community.dto.request.CvAiRequest;
import com.example.community.dto.response.CvAiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/cv")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class CVController {

    @Autowired
    private CVService cvService;

    @Autowired
    private UserService userService; // Add this

    @GetMapping
    public ResponseEntity<CVDto> getCV(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        CVDto cv = cvService.getUserCV(userId);
        return ResponseEntity.ok(cv);
    }

    @PostMapping
    public ResponseEntity<CVDto> saveCV(@RequestBody SaveCVRequest request, Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        CVDto cv = cvService.saveCV(request, userId);
        return ResponseEntity.ok(cv);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteCV(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        cvService.deleteCV(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadCV(
            @RequestParam("cv") MultipartFile file,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        Map<String, String> result = cvService.uploadCV(file, userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadCV(Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        Resource resource = cvService.downloadCV(userId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"cv.pdf\"")
                .body(resource);
    }

    @PostMapping("/ai-extract")
    public ResponseEntity<Map<String, String>> aiExtractCV(
            @RequestBody AIExtractRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        Map<String, String> extractedData = cvService.aiExtractCV(request.getText());
        return ResponseEntity.ok(extractedData);
    }

    @PostMapping("/ai/generate")
    public ResponseEntity<CvAiResponse> generateCVWithAI(
            @RequestBody CvAiRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        CvAiResponse response = cvService.generateCVWithAI(request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ai/improve-section")
    public ResponseEntity<CvAiResponse> improveCVSection(
            @RequestBody CvAiRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        CvAiResponse response = cvService.improveCVSection(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<CVDto> getUserCV(@PathVariable String userId, Authentication authentication) {
        String username = authentication.getName();
        String currentUserId = userService.getUserByUsername(username).getId();
        CVDto cv = cvService.getUserCVForJobApplication(userId, currentUserId);
        return ResponseEntity.ok(cv);
    }

    @GetMapping("/download/{applicantId}")
    public ResponseEntity<Resource> downloadApplicantCV(
            @PathVariable String applicantId,
            Authentication authentication) {
        String username = authentication.getName();
        String employerId = userService.getUserByUsername(username).getId();
        Resource resource = cvService.downloadApplicantCV(applicantId, employerId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"applicant_cv.pdf\"")
                .body(resource);
    }

    @PostMapping("/generate-pdf")
    public ResponseEntity<Resource> generateCVPDF(
            @RequestBody SaveCVRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String userId = userService.getUserByUsername(username).getId();
        Resource pdfResource = cvService.generateCVPDF(request, userId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"generated_cv.pdf\"")
                .body(pdfResource);
    }
}