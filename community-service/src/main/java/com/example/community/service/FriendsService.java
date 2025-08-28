package com.example.community.service;

import com.example.community.dto.UserDto;
import com.example.community.dto.ActivityDto;
import com.example.community.dto.FriendshipStatusDto;
import java.util.List;

public interface FriendsService {
    List<UserDto> getFriends(String userId);
    List<UserDto> getFriendSuggestions(String userId);
    List<UserDto> getFriendRequests(String userId);
    void sendFriendRequest(String senderId, String receiverId);
    void acceptFriendRequest(String senderId, String receiverId);
    void rejectFriendRequest(String senderId, String receiverId);
    void removeFriend(String userId1, String userId2);
    FriendshipStatusDto getFriendshipStatus(String userId1, String userId2);
    List<ActivityDto> getFriendsActivities(String userId);
    void dismissSuggestion(String userId, String suggestionId);
}