package com.example.edusphere.controller;

import com.example.edusphere.dto.request.TemplateRequest;
import com.example.edusphere.dto.request.UseTemplateRequest;
import com.example.edusphere.dto.response.TemplateResponse;
import com.example.common.entity.UserEntity;
import com.example.edusphere.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<TemplateResponse>> getAllTemplates(@AuthenticationPrincipal UserDetails userDetails) {
        List<TemplateResponse> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TemplateResponse> getTemplateById(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        TemplateResponse template = templateService.getTemplateById(id);
        return ResponseEntity.ok(template);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TemplateResponse> createTemplate(@RequestBody TemplateRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        TemplateResponse createdTemplate = templateService.createTemplate(request, currentUser.getId());
        return ResponseEntity.ok(createdTemplate);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TemplateResponse> updateTemplate(@PathVariable String id, @RequestBody TemplateRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        TemplateResponse updatedTemplate = templateService.updateTemplate(id, request, currentUser.getId());
        return ResponseEntity.ok(updatedTemplate);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        templateService.deleteTemplate(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/use")
    public ResponseEntity<Void> useTemplate(@PathVariable String id, @RequestBody UseTemplateRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity currentUser = (UserEntity) userDetails;
        templateService.useTemplate(id, request, currentUser.getId(), currentUser.getName());
        return ResponseEntity.ok().build();
    }
}