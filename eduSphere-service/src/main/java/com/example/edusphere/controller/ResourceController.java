package com.example.edusphere.controller;

import com.example.edusphere.dto.request.ResourceRequestDto;
import com.example.edusphere.dto.response.ResourceResponseDto;
import com.example.edusphere.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping("/by-lecturer/{lecturerId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<List<ResourceResponseDto>> getResourcesByLecturer(@PathVariable String lecturerId) {
        List<ResourceResponseDto> resources = resourceService.getResourcesByLecturerId(lecturerId);
        return ResponseEntity.ok(resources);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<ResourceResponseDto> uploadResource(@RequestParam("lecturerId") String lecturerId,
                                                              @RequestParam("file") MultipartFile file,
                                                              @ModelAttribute ResourceRequestDto metadata) {
        if (file.isEmpty() || metadata.getTitle() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File and title are required.");
        }
        ResourceResponseDto newResource = resourceService.uploadResource(lecturerId, file, metadata);
        return new ResponseEntity<>(newResource, HttpStatus.CREATED);
    }

    @PutMapping("/{resourceId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<ResourceResponseDto> updateResource(@PathVariable String resourceId, @RequestBody ResourceRequestDto metadata) {
        ResourceResponseDto updatedResource = resourceService.updateResource(resourceId, metadata);
        return ResponseEntity.ok(updatedResource);
    }


    @DeleteMapping("/{resourceId}")
    @PreAuthorize("hasRole('LECTURER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteResource(@PathVariable String resourceId) {
        resourceService.deleteResource(resourceId);
        return ResponseEntity.noContent().build();
    }
}