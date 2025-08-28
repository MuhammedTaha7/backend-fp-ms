package com.example.community.service.impl;

import com.example.community.mapper.PostMapper;
import com.example.common.entity.UserEntity;
import com.example.common.repository.UserRepository;
import com.example.community.entity.Post;
import com.example.community.entity.Comment;
import com.example.community.entity.SavedPost;
import com.example.community.repository.PostRepository;
import com.example.community.repository.CommentRepository;
import com.example.community.repository.SavedPostRepository;
import com.example.community.service.PostsService;
import com.example.community.dto.PostDto;
import com.example.community.dto.CommentDto;
import com.example.community.dto.request.CreatePostRequest;
import com.example.community.dto.request.CreateCommentRequest;
import com.example.community.mapper.CommentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class PostsServiceImpl implements PostsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private SavedPostRepository savedPostRepository;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Override
    public List<PostDto> getFeed(String userId, List<String> friendIds) {
        List<String> userIds = new ArrayList<>();
        userIds.add(userId);

        if (friendIds != null && !friendIds.isEmpty()) {
            userIds.addAll(friendIds);
        }

        // Use the simple and reliable approach: get UserEntity objects and query with them
        try {
            List<UserEntity> users = userRepository.findAllById(userIds);
            List<Post> posts = postRepository.findByUserInOrderByCreatedAtDesc(users);

            if (!posts.isEmpty()) {
                return posts.stream()
                        .map(postMapper::toDto)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            // Fallback: get all posts and filter manually
            try {
                List<Post> allPosts = postRepository.findAll();
                List<Post> filteredPosts = allPosts.stream()
                        .filter(post -> userIds.contains(post.getUser().getId()))
                        .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                        .collect(Collectors.toList());

                return filteredPosts.stream()
                        .map(postMapper::toDto)
                        .collect(Collectors.toList());
            } catch (Exception fallbackError) {
                // Final fallback: just current user's posts
                try {
                    UserEntity currentUser = getUserById(userId);
                    List<Post> userPosts = postRepository.findByUserOrderByCreatedAtDesc(currentUser);
                    return userPosts.stream()
                            .map(postMapper::toDto)
                            .collect(Collectors.toList());
                } catch (Exception finalError) {
                    return new ArrayList<>();
                }
            }
        }

        return new ArrayList<>();
    }

    @Override
    public List<PostDto> getUserPosts(String userId) {
        UserEntity user = getUserById(userId);
        List<Post> posts = postRepository.findByUserOrderByCreatedAtDesc(user);
        return posts.stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PostDto createPost(CreatePostRequest request, String userId) {
        UserEntity user = getUserById(userId);

        Post post = new Post();
        post.setUser(user);
        post.setDesc(request.getDesc());
        post.setImg(request.getImg());
        post.setFile(request.getFile());
        post.setName(user.getName());
        post.setProfilePic(user.getProfilePic());
        post.setRole(user.getRole());
        post.setGroupId(request.getGroupId());
        post.setGroupName(request.getGroupName());
        post.setLikes(new ArrayList<>());
        post.setCommentCount(0);
        post.setCreatedAt(LocalDateTime.now());

        Post savedPost = postRepository.save(post);
        return postMapper.toDto(savedPost);
    }

    @Override
    public PostDto toggleLike(String postId, String userId) {
        Post post = getPostById(postId);

        List<String> likes = post.getLikes();
        if (likes.contains(userId)) {
            likes.remove(userId);
        } else {
            likes.add(userId);
        }

        post.setLikes(likes);
        post.setUpdatedAt(LocalDateTime.now());

        Post savedPost = postRepository.save(post);
        return postMapper.toDto(savedPost);
    }

    @Override
    public void savePost(String postId, String userId) {
        Post post = getPostById(postId);
        UserEntity user = getUserById(userId);

        if (savedPostRepository.findByUserAndPost(user, post).isPresent()) {
            throw new RuntimeException("Post is already saved");
        }

        SavedPost savedPost = new SavedPost();
        savedPost.setUser(user);
        savedPost.setPost(post);
        savedPost.setSavedAt(LocalDateTime.now());

        savedPostRepository.save(savedPost);
    }

    @Override
    public void unsavePost(String postId, String userId) {
        Post post = getPostById(postId);
        UserEntity user = getUserById(userId);

        savedPostRepository.deleteByUserAndPost(user, post);
    }

    @Override
    public List<PostDto> getSavedPosts(String userId) {
        UserEntity user = getUserById(userId);
        List<SavedPost> savedPosts = savedPostRepository.findByUserOrderBySavedAtDesc(user);

        return savedPosts.stream()
                .map(savedPost -> postMapper.toDto(savedPost.getPost()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getComments(String postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        return comments.stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentDto createComment(String postId, CreateCommentRequest request, String userId) {
        UserEntity user = getUserById(userId);
        Post post = getPostById(postId);

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUser(user);
        comment.setText(request.getText());
        comment.setUsername(request.getUsername());
        comment.setProfilePicture(user.getProfilePic());
        comment.setCreatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        return commentMapper.toDto(savedComment);
    }

    private UserEntity getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Post getPostById(String postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }
}