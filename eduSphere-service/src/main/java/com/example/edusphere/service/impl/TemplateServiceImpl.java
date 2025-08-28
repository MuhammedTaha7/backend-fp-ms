package com.example.edusphere.service.impl;

import com.example.common.exceptions.ResourceNotFoundException;
import com.example.edusphere.dto.request.AnnouncementRequest;
import com.example.edusphere.dto.request.TemplateRequest;
import com.example.edusphere.dto.request.UseTemplateRequest;
import com.example.edusphere.dto.response.TemplateResponse;
import com.example.edusphere.entity.Template;
import com.example.common.entity.UserEntity;
import com.example.edusphere.repository.TemplateRepository;
import com.example.common.repository.UserRepository;
import com.example.edusphere.service.AnnouncementService;
import com.example.edusphere.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateServiceImpl implements TemplateService {

    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AnnouncementService announcementService;

    @Override
    public List<TemplateResponse> getAllTemplates() {
        List<Template> templates = templateRepository.findAll();
        return templates.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TemplateResponse getTemplateById(String templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id " + templateId));
        return mapToResponse(template);
    }

    @Override
    public TemplateResponse createTemplate(TemplateRequest templateRequest, String creatorId) {
        Template template = new Template();
        template.setName(templateRequest.getName());
        template.setCategory(templateRequest.getCategory());
        template.setSubject(templateRequest.getSubject());
        template.setContent(templateRequest.getContent());
        template.setTargetAudience(templateRequest.getTargetAudience());
        template.setStatus(templateRequest.getStatus());
        template.setCreatorId(creatorId);

        template.setVariables(extractVariablesFromContent(templateRequest.getContent()));

        Template savedTemplate = templateRepository.save(template);
        return mapToResponse(savedTemplate);
    }

    @Override
    public TemplateResponse updateTemplate(String templateId, TemplateRequest templateRequest, String updaterId) {
        Template existingTemplate = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id " + templateId));

        if (!existingTemplate.getCreatorId().equals(updaterId)) {
            throw new AccessDeniedException("You do not have permission to update this template.");
        }

        existingTemplate.setName(templateRequest.getName());
        existingTemplate.setCategory(templateRequest.getCategory());
        existingTemplate.setSubject(templateRequest.getSubject());
        existingTemplate.setContent(templateRequest.getContent());
        existingTemplate.setTargetAudience(templateRequest.getTargetAudience());
        existingTemplate.setStatus(templateRequest.getStatus());

        existingTemplate.setVariables(extractVariablesFromContent(templateRequest.getContent()));

        Template updatedTemplate = templateRepository.save(existingTemplate);
        return mapToResponse(updatedTemplate);
    }

    @Override
    public void deleteTemplate(String templateId, String deleterId) {
        Template existingTemplate = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id " + templateId));

        if (!existingTemplate.getCreatorId().equals(deleterId)) {
            throw new AccessDeniedException("You do not have permission to delete this template.");
        }

        templateRepository.deleteById(templateId);
    }

    // 🆕 UPDATED: This method now uses recipientIds from the request
    @Override
    public void useTemplate(String templateId, UseTemplateRequest useTemplateRequest, String creatorId, String creatorName) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id " + templateId));

        // Find users based on the recipient IDs from the request
        List<UserEntity> targetUsers = userRepository.findAllById(useTemplateRequest.getRecipientIds());

        for (UserEntity user : targetUsers) {
            String personalizedSubject = replaceVariables(template.getSubject(), useTemplateRequest.getVariableValues(), user);
            String personalizedContent = replaceVariables(template.getContent(), useTemplateRequest.getVariableValues(), user);

            AnnouncementRequest announcementRequest = new AnnouncementRequest();
            announcementRequest.setTitle(personalizedSubject);
            announcementRequest.setContent(personalizedContent);
            announcementRequest.setPriority("medium");

            // The template sends to a list of specific users, so the type should be 'personal'
            announcementRequest.setTargetAudienceType("personal");
            announcementRequest.setTargetUserId(user.getId());

            announcementService.createAnnouncement(announcementRequest, creatorId, creatorName);
        }
    }

    private List<String> extractVariablesFromContent(String content) {
        if (content == null) {
            return List.of();
        }
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(content);
        return matcher.results()
                .map(matchResult -> matchResult.group(1).trim())
                .distinct()
                .collect(Collectors.toList());
    }

    private String replaceVariables(String content, List<UseTemplateRequest.VariableValue> variableValues, UserEntity user) {
        String result = content;
        for (UseTemplateRequest.VariableValue var : variableValues) {
            String placeholder = "{" + var.getName() + "}";
            String value = var.getValue();
            result = result.replace(placeholder, value);
        }

        result = result.replace("{name}", user.getName());
        result = result.replace("{email}", user.getEmail());

        return result;
    }

    private TemplateResponse mapToResponse(Template template) {
        return new TemplateResponse(
                template.getId(),
                template.getName(),
                template.getCategory(),
                template.getSubject(),
                template.getContent(),
                template.getVariables(),
                template.getTargetAudience(),
                template.getStatus(),
                template.getCreatorId(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}