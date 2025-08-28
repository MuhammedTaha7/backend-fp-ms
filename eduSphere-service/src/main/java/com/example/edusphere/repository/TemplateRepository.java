package com.example.edusphere.repository;

import com.example.edusphere.entity.Template;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRepository extends MongoRepository<Template, String> {

    // We can add a custom method for retrieving templates by target audience if needed.
    List<Template> findByTargetAudienceOrderByCreatedAtDesc(String targetAudience);
}