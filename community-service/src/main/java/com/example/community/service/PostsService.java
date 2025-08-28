package com.example.community.service;

import com.example.community.dto.PostDto;
import com.example.community.dto.CommentDto;
import com.example.community.dto.request.CreatePostRequest;
import com.example.community.dto.request.CreateCommentRequest;
import java.util.List;

public interface PostsService {
    List<PostDto> getFeed(String userId, List<String> friendIds);
    List<PostDto> getUserPosts(String userId);
    PostDto createPost(CreatePostRequest request, String userId);
    PostDto toggleLike(String postId, String userId);
    void savePost(String postId, String userId);
    void unsavePost(String postId, String userId);
    List<PostDto> getSavedPosts(String userId);
    List<CommentDto> getComments(String postId);
    CommentDto createComment(String postId, CreateCommentRequest request, String userId);
}