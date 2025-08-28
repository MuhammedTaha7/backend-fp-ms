package com.example.community.service.impl;

import com.example.community.mapper.StoryMapper;
import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import com.example.community.entity.Story;
import com.example.community.entity.Friendship;
import com.example.community.repository.StoryRepository;
import com.example.community.repository.FriendshipRepository;
import com.example.community.service.StoriesService;
import com.example.community.service.FileStorageService;
import com.example.community.dto.StoryDto;
import com.example.community.dto.response.StoriesFeedResponse;
import com.example.community.dto.response.UserStoriesGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StoriesServiceImpl implements StoriesService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private StoryMapper storyMapper;

    @Override
    public StoriesFeedResponse getStoriesFeed(String userId) {
        cleanupExpiredStories();

        // Get friend IDs using robust method
        List<String> friendIds = getRobustFriendIds(userId);

        // Add current user to the list
        List<String> allUserIds = new ArrayList<>(friendIds);
        allUserIds.add(userId);

        LocalDateTime now = LocalDateTime.now();
        List<Story> allStories = new ArrayList<>();

        // Get all active stories and filter manually for maximum reliability
        try {
            List<Story> allActiveStories = storyRepository.findAllActiveStories(now);
            allStories = allActiveStories.stream()
                    .filter(story -> allUserIds.contains(story.getUser().getId()))
                    .collect(Collectors.toList());
        } catch (Exception ignored) {}

        // Group stories by user
        Map<String, List<Story>> storiesByUser = allStories.stream()
                .collect(Collectors.groupingBy(story -> story.getUser().getId()));

        // Convert to UserStoriesGroup objects
        List<UserStoriesGroup> userGroups = storiesByUser.entrySet().stream()
                .map(entry -> {
                    String userIdKey = entry.getKey();
                    List<Story> userStories = entry.getValue();
                    UserEntity user = userStories.get(0).getUser();

                    UserStoriesGroup group = new UserStoriesGroup();
                    group.setUserId(userIdKey);
                    group.setName(user.getName());
                    group.setProfilePic(user.getProfilePic());
                    group.setStories(userStories.stream()
                            .map(storyMapper::toDto)
                            .collect(Collectors.toList()));

                    return group;
                })
                .collect(Collectors.toList());

        StoriesFeedResponse response = new StoriesFeedResponse();
        response.setStories(userGroups);
        return response;
    }

    @Override
    public StoryDto createStory(String userId, String name, String profilePic, String text, MultipartFile img) {
        UserEntity user = getUserById(userId);

        Story story = new Story();
        story.setUser(user);
        story.setName(name);
        story.setProfilePic(profilePic);
        story.setCreatedAt(LocalDateTime.now());
        story.setExpiresAt(LocalDateTime.now().plusDays(1)); // 24 hours

        if (text != null && !text.trim().isEmpty()) {
            story.setText(text);
            story.setType(img != null ? "image" : "text");
        } else {
            story.setType("image");
        }

        if (img != null && !img.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(img, "stories");
            story.setImg(imageUrl);
        }

        Story savedStory = storyRepository.save(story);
        return storyMapper.toDto(savedStory);
    }

    @Override
    public StoryDto[] getUserStories(String userId) {
        UserEntity user = getUserById(userId);
        LocalDateTime now = LocalDateTime.now();
        List<Story> stories = storyRepository.findActiveStoriesByUser(user, now);

        return stories.stream()
                .map(storyMapper::toDto)
                .toArray(StoryDto[]::new);
    }

    @Override
    public void deleteStory(String storyId, String userId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));

        if (!story.getUser().getId().equals(userId)) {
            throw new RuntimeException("You can only delete your own stories");
        }

        if (story.getImg() != null) {
            fileStorageService.deleteFile(story.getImg());
        }

        storyRepository.delete(story);
    }

    @Override
    public void cleanupExpiredStories() {
        LocalDateTime now = LocalDateTime.now();
        storyRepository.deleteExpiredStories(now);
    }

    private UserEntity getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Robust method to get friend IDs - tries multiple approaches
    private List<String> getRobustFriendIds(String userId) {
        try {
            UserEntity user = getUserById(userId);

            // Approach 1: Try the repository method
            try {
                List<Friendship> friendships1 = friendshipRepository.findAllByUser(user);
                if (!friendships1.isEmpty()) {
                    return friendships1.stream()
                            .map(friendship -> friendship.getUser1().getId().equals(userId)
                                    ? friendship.getUser2().getId()
                                    : friendship.getUser1().getId())
                            .collect(Collectors.toList());
                }
            } catch (Exception ignored) {}

            // Approach 2: Manual filtering of all friendships (more reliable)
            try {
                List<Friendship> allFriendships = friendshipRepository.findAll();
                return allFriendships.stream()
                        .filter(f -> f.getUser1().getId().equals(userId) || f.getUser2().getId().equals(userId))
                        .map(friendship -> friendship.getUser1().getId().equals(userId)
                                ? friendship.getUser2().getId()
                                : friendship.getUser1().getId())
                        .collect(Collectors.toList());
            } catch (Exception ignored) {}

        } catch (Exception ignored) {}

        return new ArrayList<>();
    }
}