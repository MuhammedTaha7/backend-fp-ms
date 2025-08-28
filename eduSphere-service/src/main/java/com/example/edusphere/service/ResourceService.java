package com.example.edusphere.service;

import com.example.edusphere.dto.request.ResourceRequestDto;
import com.example.edusphere.dto.response.ResourceResponseDto;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResourceService {
    List<ResourceResponseDto> getResourcesByLecturerId(String lecturerId);
    ResourceResponseDto uploadResource(String lecturerId, MultipartFile file, ResourceRequestDto metadata);
    ResourceResponseDto updateResource(String resourceId, ResourceRequestDto metadata);
    void deleteResource(String resourceId);
}