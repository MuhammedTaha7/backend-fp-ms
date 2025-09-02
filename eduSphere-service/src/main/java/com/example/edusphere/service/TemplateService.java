package com.example.edusphere.service;

import com.example.edusphere.dto.request.TemplateRequest;
import com.example.edusphere.dto.request.UseTemplateRequest;
import com.example.edusphere.dto.response.TemplateResponse;

import java.util.List;

public interface TemplateService {

    List<TemplateResponse> getAllTemplates();

    TemplateResponse getTemplateById(String templateId);

    TemplateResponse createTemplate(TemplateRequest templateRequest, String creatorId);

    TemplateResponse updateTemplate(String templateId, TemplateRequest templateRequest, String updaterId);

    void deleteTemplate(String templateId, String deleterId);

    void useTemplate(String templateId, UseTemplateRequest useTemplateRequest, String creatorId, String creatorName);
}