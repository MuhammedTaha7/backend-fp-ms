package com.example.community.controller;

import com.example.community.service.ChallengesService;
import com.example.community.dto.ChallengeDto;
import com.example.community.dto.UserChallengeDto;
import com.example.community.dto.request.CreateChallengeRequest;
import com.example.community.dto.request.UpdateChallengeStatusRequest;
import com.example.community.dto.request.SubmitChallengeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@RequestMapping("/api/challenges")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ChallengesController {

    @Autowired
    private ChallengesService challengesService;

    @GetMapping
    public ResponseEntity<List<ChallengeDto>> getAllChallenges(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String type) {
        List<ChallengeDto> challenges = challengesService.getAllChallenges(category, difficulty, type);
        return ResponseEntity.ok(challenges);
    }

    @GetMapping("/my-challenges")
    public ResponseEntity<List<UserChallengeDto>> getMyChallenges(Authentication authentication) {
        String userId = authentication.getName();
        List<UserChallengeDto> challenges = challengesService.getUserChallenges(userId);
        return ResponseEntity.ok(challenges);
    }

    @PostMapping
    public ResponseEntity<ChallengeDto> createChallenge(
            @RequestBody CreateChallengeRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        ChallengeDto challenge = challengesService.createChallenge(request, userId);
        return ResponseEntity.ok(challenge);
    }

    @PutMapping("/{challengeId}/status")
    public ResponseEntity<UserChallengeDto> updateChallengeStatus(
            @PathVariable String challengeId,
            @RequestBody UpdateChallengeStatusRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        UserChallengeDto userChallenge = challengesService.updateChallengeStatus(challengeId, request, userId);
        return ResponseEntity.ok(userChallenge);
    }

    @PostMapping("/{challengeId}/submit")
    public ResponseEntity<Void> submitChallenge(
            @PathVariable String challengeId,
            @RequestBody SubmitChallengeRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        challengesService.submitChallenge(challengeId, request, userId);
        return ResponseEntity.ok().build();
    }
}