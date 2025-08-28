package com.example.community.service.impl;

import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import com.example.community.entity.FriendRequest;
import com.example.community.entity.Friendship;
import com.example.community.entity.Activity;
import com.example.community.repository.FriendRequestRepository;
import com.example.community.repository.FriendshipRepository;
import com.example.community.repository.ActivityRepository;
import com.example.community.service.FriendsService;
import com.example.community.dto.UserDto;
import com.example.community.dto.ActivityDto;
import com.example.community.dto.FriendshipStatusDto;
import com.example.community.mapper.UserMapper;
import com.example.community.mapper.ActivityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class FriendsServiceImpl implements FriendsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Override
    public List<UserDto> getFriends(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> friendIds = getRobustFriendIds(userId);

        return friendIds.stream()
                .map(friendId -> {
                    UserEntity friend = userRepository.findById(friendId)
                            .orElse(null);
                    return friend != null ? userMapper.toDto(friend) : null;
                })
                .filter(friend -> friend != null)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDto> getFriendSuggestions(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<UserEntity> allUsers = userRepository.findAll();
        List<String> friendIds = getRobustFriendIds(userId);
        List<String> allRequestRelatedIds = getAllRequestRelatedUserIds(userId);

        List<String> excludeIds = new ArrayList<>();
        excludeIds.addAll(friendIds);
        excludeIds.addAll(allRequestRelatedIds);
        excludeIds.add(userId);
        excludeIds = excludeIds.stream().distinct().collect(Collectors.toList());

        List<String> finalExcludeIds = excludeIds;
        List<UserEntity> suggestions = allUsers.stream()
                .filter(u -> !finalExcludeIds.contains(u.getId()))
                .collect(Collectors.toList());

        return suggestions.stream()
                .limit(10)
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDto> getFriendRequests(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<FriendRequest> requests = friendRequestRepository.findByReceiverAndStatus(user, "PENDING");

        return requests.stream()
                .map(request -> userMapper.toDto(request.getSender()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void sendFriendRequest(String senderId, String receiverId) {
        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        UserEntity receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (isDuplicateRequest(sender, receiver)) {
            throw new RuntimeException("Friend request already exists or users are already friends");
        }

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequest.setStatus("PENDING");
        friendRequest.setCreatedAt(LocalDateTime.now());

        friendRequestRepository.save(friendRequest);
        createActivity(sender, "sent a friend request to " + receiver.getName(), "friend_request", receiverId);
    }

    @Override
    @Transactional
    public void acceptFriendRequest(String senderId, String receiverId) {
        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        UserEntity receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        FriendRequest friendRequest = findPendingRequest(sender, receiver);
        if (friendRequest == null) {
            throw new RuntimeException("No pending friend request found");
        }

        if (friendshipExists(sender, receiver)) {
            friendRequest.setStatus("ACCEPTED");
            friendRequest.setRespondedAt(LocalDateTime.now());
            friendRequestRepository.save(friendRequest);
            return;
        }

        friendRequest.setStatus("ACCEPTED");
        friendRequest.setRespondedAt(LocalDateTime.now());
        friendRequestRepository.save(friendRequest);

        Friendship friendship = new Friendship();
        friendship.setUser1(sender);
        friendship.setUser2(receiver);
        friendship.setCreatedAt(LocalDateTime.now());
        friendshipRepository.save(friendship);

        createActivity(receiver, "accepted friend request from " + sender.getName(), "friend_accept", senderId);
    }

    @Override
    @Transactional
    public void rejectFriendRequest(String senderId, String receiverId) {
        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        UserEntity receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        FriendRequest friendRequest = findPendingRequest(sender, receiver);
        if (friendRequest == null) {
            throw new RuntimeException("No pending friend request found");
        }

        friendRequest.setStatus("REJECTED");
        friendRequest.setRespondedAt(LocalDateTime.now());
        friendRequestRepository.save(friendRequest);
    }

    @Override
    @Transactional
    public void removeFriend(String userId1, String userId2) {
        UserEntity user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserEntity user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Remove friendships
        try {
            List<Friendship> allFriendships = friendshipRepository.findAll();
            List<Friendship> toDelete = new ArrayList<>();

            for (Friendship friendship : allFriendships) {
                try {
                    String friend1Id = friendship.getUser1().getId();
                    String friend2Id = friendship.getUser2().getId();

                    boolean shouldDelete = (friend1Id.equals(userId1) && friend2Id.equals(userId2)) ||
                            (friend1Id.equals(userId2) && friend2Id.equals(userId1));

                    if (shouldDelete) {
                        toDelete.add(friendship);
                    }
                } catch (Exception e) {
                    // Skip malformed friendships
                }
            }

            if (!toDelete.isEmpty()) {
                friendshipRepository.deleteAll(toDelete);
            }

        } catch (Exception e) {
            // Fallback to individual deletion
            performAggressiveCleanup(userId1, userId2);
        }

        // Remove friend requests
        try {
            List<FriendRequest> allRequests = friendRequestRepository.findAll();
            List<FriendRequest> requestsToDelete = new ArrayList<>();

            for (FriendRequest request : allRequests) {
                try {
                    String senderId = request.getSender().getId();
                    String receiverId = request.getReceiver().getId();

                    boolean shouldDelete = (senderId.equals(userId1) && receiverId.equals(userId2)) ||
                            (senderId.equals(userId2) && receiverId.equals(userId1));

                    if (shouldDelete) {
                        requestsToDelete.add(request);
                    }
                } catch (Exception e) {
                    // Skip malformed requests
                }
            }

            if (!requestsToDelete.isEmpty()) {
                friendRequestRepository.deleteAll(requestsToDelete);
            }

        } catch (Exception e) {
            // Log error but continue
        }
    }

    @Override
    public FriendshipStatusDto getFriendshipStatus(String userId1, String userId2) {
        UserEntity user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserEntity user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (friendshipExists(user1, user2)) {
            return new FriendshipStatusDto("friends");
        }

        if (hasPendingRequest(user1, user2)) {
            return new FriendshipStatusDto("pending");
        }

        return new FriendshipStatusDto("none");
    }

    @Override
    public List<ActivityDto> getFriendsActivities(String userId) {
        List<String> friendIds = getRobustFriendIds(userId);
        List<Activity> activities = activityRepository.findByUserIdInOrderByCreatedAtDesc(friendIds);

        return activities.stream()
                .limit(20)
                .map(activityMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void dismissSuggestion(String userId, String suggestionId) {
        // Implementation for dismissing suggestions
    }

    // PRIVATE HELPER METHODS

    private boolean isDuplicateRequest(UserEntity sender, UserEntity receiver) {
        if (friendshipExists(sender, receiver)) {
            return true;
        }

        try {
            List<FriendRequest> allRequests = friendRequestRepository.findAll();
            return allRequests.stream()
                    .anyMatch(r -> {
                        String senderId = r.getSender().getId();
                        String receiverId = r.getReceiver().getId();
                        String targetSenderId = sender.getId();
                        String targetReceiverId = receiver.getId();

                        return (senderId.equals(targetSenderId) && receiverId.equals(targetReceiverId)) ||
                                (senderId.equals(targetReceiverId) && receiverId.equals(targetSenderId));
                    });
        } catch (Exception e) {
            return false;
        }
    }

    private boolean friendshipExists(UserEntity user1, UserEntity user2) {
        try {
            Optional<Friendship> friendship = friendshipRepository.findBetweenUsers(user1, user2);
            if (friendship.isPresent()) {
                return true;
            }
        } catch (Exception e) {
            // Fallback to manual check
        }

        try {
            List<Friendship> allFriendships = friendshipRepository.findAll();
            return allFriendships.stream()
                    .anyMatch(f -> {
                        String friend1Id = f.getUser1().getId();
                        String friend2Id = f.getUser2().getId();
                        String userId1 = user1.getId();
                        String userId2 = user2.getId();

                        return (friend1Id.equals(userId1) && friend2Id.equals(userId2)) ||
                                (friend1Id.equals(userId2) && friend2Id.equals(userId1));
                    });
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasPendingRequest(UserEntity user1, UserEntity user2) {
        try {
            List<FriendRequest> allRequests = friendRequestRepository.findAll();
            return allRequests.stream()
                    .anyMatch(r -> {
                        if (!"PENDING".equals(r.getStatus())) {
                            return false;
                        }

                        String senderId = r.getSender().getId();
                        String receiverId = r.getReceiver().getId();
                        String userId1 = user1.getId();
                        String userId2 = user2.getId();

                        return (senderId.equals(userId1) && receiverId.equals(userId2)) ||
                                (senderId.equals(userId2) && receiverId.equals(userId1));
                    });
        } catch (Exception e) {
            return false;
        }
    }

    private FriendRequest findPendingRequest(UserEntity sender, UserEntity receiver) {
        try {
            List<FriendRequest> pendingRequests = friendRequestRepository.findByReceiverAndStatus(receiver, "PENDING");
            return pendingRequests.stream()
                    .filter(req -> req.getSender().getId().equals(sender.getId()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> getRobustFriendIds(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            List<Friendship> friendships = friendshipRepository.findAllByUser(user);
            if (!friendships.isEmpty()) {
                return friendships.stream()
                        .map(friendship -> friendship.getUser1().getId().equals(userId)
                                ? friendship.getUser2().getId()
                                : friendship.getUser1().getId())
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {}

        try {
            List<Friendship> allFriendships = friendshipRepository.findAll();
            return allFriendships.stream()
                    .filter(f -> f.getUser1().getId().equals(userId) || f.getUser2().getId().equals(userId))
                    .map(friendship -> friendship.getUser1().getId().equals(userId)
                            ? friendship.getUser2().getId()
                            : friendship.getUser1().getId())
                    .collect(Collectors.toList());
        } catch (Exception ignored) {}

        return new ArrayList<>();
    }

    private List<String> getAllRequestRelatedUserIds(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> relatedIds = new ArrayList<>();

        try {
            List<FriendRequest> sentRequests = friendRequestRepository.findBySender(user);
            List<String> sentIds = sentRequests.stream()
                    .map(request -> request.getReceiver().getId())
                    .collect(Collectors.toList());
            relatedIds.addAll(sentIds);

            List<FriendRequest> allReceivedRequests = friendRequestRepository.findByReceiver(user);
            List<String> receivedIds = allReceivedRequests.stream()
                    .map(request -> request.getSender().getId())
                    .collect(Collectors.toList());
            relatedIds.addAll(receivedIds);

        } catch (Exception ignored) {}

        return relatedIds.stream().distinct().collect(Collectors.toList());
    }

    private void performAggressiveCleanup(String userId1, String userId2) {
        try {
            List<Friendship> allFriendships = friendshipRepository.findAll();
            List<String> idsToDelete = new ArrayList<>();

            for (Friendship f : allFriendships) {
                try {
                    String id1 = f.getUser1().getId();
                    String id2 = f.getUser2().getId();

                    if ((id1.equals(userId1) && id2.equals(userId2)) ||
                            (id1.equals(userId2) && id2.equals(userId1))) {
                        idsToDelete.add(f.getId());
                    }
                } catch (Exception e) {
                    // Skip malformed entries
                }
            }

            for (String id : idsToDelete) {
                try {
                    friendshipRepository.deleteById(id);
                } catch (Exception e) {
                    // Continue with other deletions
                }
            }

            List<FriendRequest> allRequests = friendRequestRepository.findAll();
            List<String> requestIdsToDelete = new ArrayList<>();

            for (FriendRequest r : allRequests) {
                try {
                    String senderId = r.getSender().getId();
                    String receiverId = r.getReceiver().getId();

                    if ((senderId.equals(userId1) && receiverId.equals(userId2)) ||
                            (senderId.equals(userId2) && receiverId.equals(userId1))) {
                        requestIdsToDelete.add(r.getId());
                    }
                } catch (Exception e) {
                    // Skip malformed entries
                }
            }

            for (String id : requestIdsToDelete) {
                try {
                    friendRequestRepository.deleteById(id);
                } catch (Exception e) {
                    // Continue with other deletions
                }
            }

        } catch (Exception e) {
            // Log error but don't throw exception
        }
    }

    private void createActivity(UserEntity user, String action, String type, String targetId) {
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setAction(action);
        activity.setType(type);
        activity.setTargetId(targetId);
        activity.setCreatedAt(LocalDateTime.now());
        activityRepository.save(activity);
    }
}