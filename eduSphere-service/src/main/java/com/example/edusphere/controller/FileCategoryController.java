package com.example.edusphere.controller;

import com.example.edusphere.dto.request.FileUploadRequest;
import com.example.edusphere.dto.response.FileResponse;
import com.example.common.entity.UserEntity;
import com.example.edusphere.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileCategoryController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<FileResponse> uploadFile(@RequestParam("file") MultipartFile file,
                                                   @ModelAttribute FileUploadRequest fileMetadata,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        FileResponse newFile = fileService.uploadFileWithMetadata(file, fileMetadata, currentUser.getId(), currentUser.getName());
        return ResponseEntity.ok(newFile);
    }

    @GetMapping
    public ResponseEntity<List<FileResponse>> getFiles(@AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        List<FileResponse> files = fileService.getAccessibleFiles(currentUser.getId(), currentUser.getRole(), null); // Pass null for userDepartment
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileResponse> getFileMetadata(@PathVariable String id) {
        FileResponse fileMetadata = fileService.getFileMetadata(id);
        return ResponseEntity.ok(fileMetadata);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteFile(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        fileService.deleteFile(id, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.noContent().build();
    }
}