package com.example.community.service.impl;

import com.example.common.entity.UserEntity; // Main project
import com.example.common.repository.UserRepository; // Main project
import com.example.community.entity.Challenge;
import com.example.community.entity.UserChallenge;
import com.example.community.repository.ChallengeRepository;
import com.example.community.repository.UserChallengeRepository;
import com.example.community.service.ChallengesService;
import com.example.community.dto.ChallengeDto;
import com.example.community.dto.UserChallengeDto;
import com.example.community.dto.request.CreateChallengeRequest;
import com.example.community.dto.request.UpdateChallengeStatusRequest;
import com.example.community.dto.request.SubmitChallengeRequest;
import com.example.community.mapper.ChallengeMapper;
import com.example.community.mapper.UserChallengeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
public class ChallengesServiceImpl implements ChallengesService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private UserChallengeRepository userChallengeRepository;

    @Autowired
    private ChallengeMapper challengeMapper;

    @Autowired
    private UserChallengeMapper userChallengeMapper;

    @Override
    public List<ChallengeDto> getAllChallenges(String category, String difficulty, String type) {
        List<Challenge> challenges;

        if (category != null || difficulty != null || type != null) {
            challenges = challengeRepository.findWithFilters(category, difficulty, type);
        } else {
            challenges = challengeRepository.findAll();
        }

        return challenges.stream()
                .map(challengeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserChallengeDto> getUserChallenges(String userId) {
        UserEntity user = getUserById(userId);
        List<UserChallenge> userChallenges = userChallengeRepository.findByUserOrderByStartedAtDesc(user);

        return userChallenges.stream()
                .map(userChallengeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ChallengeDto createChallenge(CreateChallengeRequest request, String userId) {
        UserEntity user = getUserById(userId);

        // Check if user has permission to create challenges (e.g., Lecturer role)
        if (!"Lecturer".equals(user.getRole())) {
            throw new RuntimeException("Only lecturers can create challenges");
        }

        Challenge challenge = new Challenge();
        challenge.setTitle(request.getTitle());
        challenge.setDescription(request.getDescription());
        challenge.setCategory(request.getCategory());
        challenge.setType(request.getType());
        challenge.setDifficulty(request.getDifficulty());
        challenge.setPoints(request.getPoints());
        challenge.setCreator(user);
        challenge.setCreatedAt(LocalDateTime.now());

        Challenge savedChallenge = challengeRepository.save(challenge);
        return challengeMapper.toDto(savedChallenge);
    }

    @Override
    public UserChallengeDto updateChallengeStatus(String challengeId, UpdateChallengeStatusRequest request, String userId) {
        Challenge challenge = getChallengeById(challengeId);
        UserEntity user = getUserById(userId);

        Optional<UserChallenge> existingUserChallenge = userChallengeRepository.findByUserAndChallenge(user, challenge);
        UserChallenge userChallenge;

        if (existingUserChallenge.isPresent()) {
            userChallenge = existingUserChallenge.get();
        } else {
            userChallenge = new UserChallenge();
            userChallenge.setUser(user);
            userChallenge.setChallenge(challenge);
            userChallenge.setStartedAt(LocalDateTime.now());
        }

        userChallenge.setStatus(request.getStatus());

        if ("COMPLETED".equals(request.getStatus())) {
            userChallenge.setCompletedAt(LocalDateTime.now());
        }

        if (request.getSubmissionLink() != null) {
            userChallenge.setSubmissionLink(request.getSubmissionLink());
        }

        UserChallenge savedUserChallenge = userChallengeRepository.save(userChallenge);
        return userChallengeMapper.toDto(savedUserChallenge);
    }

    @Override
    public void submitChallenge(String challengeId, SubmitChallengeRequest request, String userId) {
        Challenge challenge = getChallengeById(challengeId);
        UserEntity user = getUserById(userId);

        UserChallenge userChallenge = userChallengeRepository.findByUserAndChallenge(user, challenge)
                .orElseThrow(() -> new RuntimeException("You haven't started this challenge yet"));

        userChallenge.setStatus("COMPLETED");
        userChallenge.setSubmissionLink(request.getSubmissionLink());
        userChallenge.setNotes(request.getNotes());
        userChallenge.setCompletedAt(LocalDateTime.now());

        userChallengeRepository.save(userChallenge);
    }

    private UserEntity getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Challenge getChallengeById(String challengeId) {
        return challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));
    }
}