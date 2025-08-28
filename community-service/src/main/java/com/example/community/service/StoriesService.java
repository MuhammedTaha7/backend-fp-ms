package com.example.community.service;

import com.example.community.dto.StoryDto;
import com.example.community.dto.response.StoriesFeedResponse;
import org.springframework.web.multipart.MultipartFile;

public interface StoriesService {
    StoriesFeedResponse getStoriesFeed(String userId);
    StoryDto createStory(String userId, String name, String profilePic, String text, MultipartFile img);
    StoryDto[] getUserStories(String userId);
    void deleteStory(String storyId, String userId);
    void cleanupExpiredStories();
}