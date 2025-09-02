package com.example.edusphere.service.impl;

import com.example.edusphere.dto.request.FileUploadRequest;
import com.example.edusphere.dto.request.ResourceRequestDto;
import com.example.edusphere.dto.response.FileResponse;
import com.example.edusphere.dto.response.ResourceResponseDto;
import com.example.edusphere.entity.LecturerResource;
import com.example.edusphere.repository.LecturerResourceRepository;
import com.example.common.repository.UserRepository;
import com.example.edusphere.service.FileService;
import com.example.edusphere.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final LecturerResourceRepository resourceRepository;
    private final FileService fileService;
    private final UserRepository userRepository;

    @Override
    public List<ResourceResponseDto> getResourcesByLecturerId(String lecturerId) {
        List<LecturerResource> resources = resourceRepository.findByLecturerId(lecturerId);
        return resources.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ResourceResponseDto uploadResource(String lecturerId, MultipartFile file, ResourceRequestDto metadata) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required for upload");
        }

        try {
            String uploaderName = userRepository.findById(lecturerId)
                    .map(user -> user.getName())
                    .orElse("Unknown Lecturer");

            FileUploadRequest fileUploadRequest = new FileUploadRequest();
            fileUploadRequest.setName(metadata.getTitle());
            fileUploadRequest.setDescription(metadata.getDescription());
            fileUploadRequest.setCategory(metadata.getType());

            FileResponse fileResponse = fileService.uploadFileWithMetadata(file, fileUploadRequest, lecturerId, uploaderName);

            LecturerResource resource = new LecturerResource();
            resource.setLecturerId(lecturerId);
            resource.setTitle(metadata.getTitle());
            resource.setType(metadata.getType());
            resource.setDescription(metadata.getDescription());
            resource.setDate(metadata.getDate());
            resource.setInstitution(metadata.getInstitution());
            resource.setUrl(metadata.getUrl());
            resource.setTags(metadata.getTags());

            //  Use the actual stored filename (UUID)
            resource.setFileName(fileResponse.getName()); // UUID-based filename
            resource.setSize(Long.parseLong(fileResponse.getSize()));
            resource.setMimeType(fileResponse.getType());
            resource.setFilePath("edusphere/files/" + fileResponse.getName()); // relative path

            //  Set real values from FileResponse
            resource.setFilePath("uploads/edusphere/file/" + fileResponse.getName()); // or use uploadDir from @Value
            resource.setMimeType(fileResponse.getType());
            resource.setSize(Long.parseLong(fileResponse.getSize()));
            resource.setUploadDate(fileResponse.getUploadDate());

            LecturerResource savedResource = resourceRepository.save(resource);
            return mapToDto(savedResource);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file: " + e.getMessage());
        }
    }

    @Override
    public ResourceResponseDto updateResource(String resourceId, ResourceRequestDto metadata) {
        LecturerResource existingResource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));

        if (metadata.getTitle() != null) existingResource.setTitle(metadata.getTitle());
        if (metadata.getType() != null) existingResource.setType(metadata.getType());
        if (metadata.getDescription() != null) existingResource.setDescription(metadata.getDescription());
        if (metadata.getDate() != null) existingResource.setDate(metadata.getDate());
        if (metadata.getInstitution() != null) existingResource.setInstitution(metadata.getInstitution());
        if (metadata.getUrl() != null) existingResource.setUrl(metadata.getUrl());
        if (metadata.getTags() != null) existingResource.setTags(metadata.getTags());

        LecturerResource updatedResource = resourceRepository.save(existingResource);
        return mapToDto(updatedResource);
    }



    @Override
    public void deleteResource(String resourceId) {
        LecturerResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));

        fileService.deleteFile(resource.getId(), resource.getLecturerId(), "Lecturer");
        resourceRepository.delete(resource);
    }

    private ResourceResponseDto mapToDto(LecturerResource resource) {
        ResourceResponseDto dto = new ResourceResponseDto();
        dto.setId(resource.getId());
        dto.setLecturerId(resource.getLecturerId());
        dto.setTitle(resource.getTitle());
        dto.setType(resource.getType());
        dto.setDescription(resource.getDescription());
        dto.setDate(resource.getDate());
        dto.setInstitution(resource.getInstitution());
        dto.setUrl(resource.getUrl());
        dto.setTags(resource.getTags());
        dto.setFileName(resource.getFileName());
        dto.setSize(formatFileSize(resource.getSize()));
        dto.setUploadDate(resource.getUploadDate());
        return dto;
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}