package com.example.edusphere.service.impl;

import com.example.edusphere.service.OpenAiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiServiceImpl implements OpenAiService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${openai.max.tokens:3000}")
    private Integer maxTokens;

    @Value("${openai.retry.max.attempts:3}")
    private Integer maxRetryAttempts;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Security patterns
    private static final Set<String> ALLOWED_COLLECTIONS = Set.of(
            "users", "courses", "assignments", "studentgrades", "submissions",
            "tasks", "tasksubmissions", "meetings", "attendancesessions",
            "announcements", "messages", "events", "departments", "files", "studentrequests"
    );

    private static final Set<String> FORBIDDEN_OPERATIONS = Set.of(
            "$where", "$expr", "$function", "eval", "mapReduce", "javascript"
    );

    private static final Pattern SUSPICIOUS_PATTERN = Pattern.compile(
            "\\$where|\\$expr|\\$function|eval|mapReduce|javascript|function\\s*\\(|script|drop|delete",
            Pattern.CASE_INSENSITIVE
    );

    // Query complexity patterns
    private static final Pattern COMPLEX_QUERY_PATTERN = Pattern.compile(
            "(?i)(analytics?|dashboard|breakdown|summary|statistics|metrics|performance|trends|insights|compare|analysis|aggregation)"
    );

    private static final Pattern AGGREGATION_PATTERN = Pattern.compile(
            "(?i)(count|sum|average|avg|total|group\\s+by|breakdown|distribution|percentage)"
    );

    // Cache for frequently used queries
    private final Map<String, String> queryCache = new HashMap<>();

    @Override
    @Cacheable(value = "mongoQueries", key = "#naturalLanguagePrompt.hashCode()")
    public String generateMongoQuery(@NotBlank String naturalLanguagePrompt) {
        log.info("Generating MongoDB query for: {}", naturalLanguagePrompt.length() > 100 ?
                naturalLanguagePrompt.substring(0, 100) + "..." : naturalLanguagePrompt);

        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetryAttempts) {
            try {
                attempt++;

                // Enhanced input processing
                String processedPrompt = preprocessQuery(naturalLanguagePrompt);

                // Determine query complexity and approach
                QueryContext context = analyzeQueryContext(processedPrompt);

                // Generate query based on complexity
                String mongoQuery = generateIntelligentQuery(processedPrompt, context);

                // Validate and enhance the result
                String validatedQuery = validateAndEnhanceQuery(mongoQuery, context);

                log.info("Successfully generated MongoDB query on attempt {}", attempt);
                return validatedQuery;

            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < maxRetryAttempts) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("All {} attempts failed. Last error: {}", maxRetryAttempts, lastException.getMessage());
        return generateFallbackQuery(naturalLanguagePrompt);
    }

    // ============================================================================
    // ENHANCED QUERY PROCESSING
    // ============================================================================

    private String preprocessQuery(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }

        if (prompt.length() > 2000) {
            log.warn("Prompt too long, truncating to 2000 characters");
            prompt = prompt.substring(0, 2000);
        }

        // Security validation
        if (SUSPICIOUS_PATTERN.matcher(prompt).find()) {
            log.warn("Suspicious pattern detected in prompt");
            throw new SecurityException("Invalid prompt detected");
        }

        // Clean and normalize the prompt
        return prompt.trim();
    }

    private QueryContext analyzeQueryContext(String prompt) {
        QueryContext context = new QueryContext();
        context.originalPrompt = prompt;
        context.cleanPrompt = prompt.toLowerCase();

        // Analyze complexity
        context.isComplex = COMPLEX_QUERY_PATTERN.matcher(context.cleanPrompt).find();
        context.requiresAggregation = AGGREGATION_PATTERN.matcher(context.cleanPrompt).find();

        // Detect target collection
        context.targetCollection = detectPrimaryCollection(context.cleanPrompt);

        // Detect operation type
        context.operationType = detectOperationType(context.cleanPrompt);

        // Calculate complexity score
        context.complexityScore = calculateComplexityScore(context);

        return context;
    }

    private String detectPrimaryCollection(String prompt) {
        // Collection detection with priority scoring
        Map<String, Integer> collectionScores = new HashMap<>();

        // User-related terms
        if (prompt.matches(".*\\b(user|student|lecturer|teacher|admin|people|person)s?\\b.*")) {
            collectionScores.put("users", 10);
        }

        // Course-related terms
        if (prompt.matches(".*\\b(course|class|subject|module)s?\\b.*")) {
            collectionScores.put("courses", 10);
        }

        // Assignment-related terms
        if (prompt.matches(".*\\b(assignment|homework|task|work)s?\\b.*")) {
            collectionScores.put("assignments", 10);
        }

        // Grade-related terms
        if (prompt.matches(".*\\b(grade|score|mark|result)s?\\b.*")) {
            collectionScores.put("studentgrades", 10);
        }

        // Meeting-related terms
        if (prompt.matches(".*\\b(meeting|session|conference)s?\\b.*")) {
            collectionScores.put("meetings", 10);
        }

        // Announcement-related terms
        if (prompt.matches(".*\\b(announcement|news|notice|notification)s?\\b.*")) {
            collectionScores.put("announcements", 10);
        }

        return collectionScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("users"); // Default fallback
    }

    private String detectOperationType(String prompt) {
        if (prompt.matches(".*\\b(count|how many|number of)\\b.*")) {
            return "count";
        } else if (prompt.matches(".*\\b(top|best|highest|maximum)\\b.*")) {
            return "top";
        } else if (prompt.matches(".*\\b(recent|latest|new)\\b.*")) {
            return "recent";
        } else if (prompt.matches(".*\\b(search|find|look for)\\b.*")) {
            return "search";
        } else if (prompt.matches(".*\\b(compare|vs|versus)\\b.*")) {
            return "compare";
        }
        return "list";
    }

    private int calculateComplexityScore(QueryContext context) {
        int score = 1; // Base score

        if (context.isComplex) score += 3;
        if (context.requiresAggregation) score += 2;
        if (context.operationType.equals("compare")) score += 2;
        if (context.cleanPrompt.contains("analytics")) score += 2;

        return Math.min(score, 10);
    }

    private String generateIntelligentQuery(String prompt, QueryContext context) {
        if (context.complexityScore > 5) {
            return generateComplexQuery(prompt, context);
        } else {
            return generateStandardQuery(prompt, context);
        }
    }

    private String generateComplexQuery(String prompt, QueryContext context) {
        String enhancedSystemPrompt = buildComplexSystemPrompt(context);
        String enhancedUserPrompt = buildEnhancedUserPrompt(prompt, context);

        return callOpenAIAPI(enhancedSystemPrompt, enhancedUserPrompt);
    }

    private String generateStandardQuery(String prompt, QueryContext context) {
        String standardSystemPrompt = buildStandardSystemPrompt();

        return callOpenAIAPI(standardSystemPrompt, prompt);
    }

    // ============================================================================
    // SYSTEM PROMPT BUILDERS
    // ============================================================================

    private String buildComplexSystemPrompt(QueryContext context) {
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return String.format("""
            You are an EXPERT MongoDB query generator for advanced educational analytics.
            Current DateTime: %s
            
            🎯 MISSION: Generate intelligent MongoDB queries for complex educational data analysis.
            
            CONTEXT ANALYSIS:
            - Target Collection: %s
            - Operation Type: %s
            - Complexity Score: %d/10
            - Requires Aggregation: %s
            
            ⚡ ADVANCED CAPABILITIES:
            1. Complex filtering with multiple criteria
            2. Intelligent field selection based on use case
            3. Performance-optimized sorting and limiting
            4. Date range calculations and time-based filtering
            5. Cross-reference analysis preparation
            6. Statistical query preparation
            
            📋 ENHANCED RESPONSE FORMAT:
            {
              "collection": "optimal_collection_name",
              "filter": { /* intelligent multi-criteria filters */ },
              "sort": { "field_name": 1 or -1 },
              "limit": appropriate_number,
              "fields": ["relevant", "fields", "for", "analysis"],
              "skip": 0,
              "metadata": {
                "queryType": "complex|standard|aggregation",
                "estimatedComplexity": score,
                "optimizationHints": ["index_suggestions"]
              }
            }
            
            🗄️ COLLECTION SCHEMAS (Full Detail):
            
            📚 users - People in the educational system
            Core Fields: id, username, email, name, role, department, status, academicYear
            Extended: title, university, bio, specialization, experience, rating, createdAt
            Role Codes: "1300"=student, "1200"=lecturer, "1100"=admin
            
            📖 courses - Academic courses and curricula  
            Core Fields: id, name, code, description, department, semester, credits, lecturerId
            Extended: academicYear, year, language, progress, prerequisites, finalExam, createdAt
            
            📝 assignments - Academic assignments and homework
            Core Fields: id, title, description, course, type, dueDate, status, priority
            Extended: instructorId, difficulty, semester, progress, createdAt, updatedAt
            
            🎯 studentgrades - Academic performance records
            Core Fields: id, studentId, courseId, finalGrade, finalLetterGrade
            Extended: createdAt, updatedAt
            
            🎥 meetings - Virtual sessions and classes
            Core Fields: id, title, description, type, status, courseId, scheduledAt
            Extended: lecturerId, duration, maxUsers, courseName, courseCode, createdAt
            
            📢 announcements - System communications
            Core Fields: id, title, content, creatorName, priority, status, createdAt
            Extended: targetAudienceType, targetDepartment, expiryDate, scheduledDate
            
            🧠 INTELLIGENT QUERY ENHANCEMENT:
            
            TIME-BASED INTELLIGENCE:
            - "today" → {"createdAt": {"$gte": "%sT00:00:00.000Z", "$lte": "%sT23:59:59.999Z"}}
            - "this week" → calculate current week range
            - "last month" → {"createdAt": {"$gte": "%s"}}
            - "recent" → {"createdAt": {"$gte": "%s"}} + sort: {"createdAt": -1}
            
            PERFORMANCE OPTIMIZATION:
            - Always include reasonable limits (10-500 based on query type)
            - Use indexed fields for sorting: id, createdAt, status, role
            - Exclude sensitive fields: password, __v
            - Add status filters to exclude deleted records
            
            ADVANCED EXAMPLES:
            
            Input: "Show me top performing students with their course analytics"
            Output: {
              "collection": "studentgrades",
              "filter": {"finalGrade": {"$gte": 85}},
              "sort": {"finalGrade": -1},
              "limit": 20,
              "fields": ["studentId", "courseId", "finalGrade", "finalLetterGrade"],
              "metadata": {
                "queryType": "complex",
                "estimatedComplexity": 7,
                "optimizationHints": ["index on finalGrade", "consider aggregation for course stats"]
              }
            }
            
            Remember: Generate intelligent, optimized queries that provide maximum insight!
            """,
                currentDateTime, context.targetCollection, context.operationType,
                context.complexityScore, context.requiresAggregation,
                getCurrentDate(), getCurrentDate(), getLastMonth(), getLastMonth());
    }

    private String buildStandardSystemPrompt() {
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return String.format("""
            You are a MongoDB query generator for the EduSphere educational management system.
            Current DateTime: %s
            
            🎯 MISSION: Convert natural language into efficient MongoDB queries.
            
            ⚡ CORE RULES:
            1. ALWAYS return ONLY valid JSON - no explanations, markdown, or extra text
            2. Choose the most appropriate collection based on the request
            3. Select relevant fields for the user's needs
            4. Apply intelligent filters based on context
            5. Use proper data types (numbers as numbers, dates as ISO dates)
            
            📋 STANDARD RESPONSE FORMAT:
            {
              "collection": "collection_name",
              "filter": { /* filters based on request */ },
              "sort": { "field_name": 1 or -1 },
              "limit": number,
              "fields": ["relevant", "fields"]
            }
            
            🗄️ AVAILABLE COLLECTIONS:
            
            📚 users: id, username, email, name, role, department, status, academicYear, createdAt
            📖 courses: id, name, code, description, department, semester, credits, lecturerId
            📝 assignments: id, title, description, course, dueDate, status, priority
            🎯 studentgrades: id, studentId, courseId, finalGrade, finalLetterGrade
            🎥 meetings: id, title, description, scheduledAt, status, courseCode
            📢 announcements: id, title, content, creatorName, priority, createdAt
            📤 submissions: id, courseId, assignmentId, studentId, grade, submittedAt
            ✅ tasks: id, title, description, courseId, dueDate, instructorId
            💬 messages: id, subject, content, senderId, recipientId, createdAt
            📅 events: id, title, description, startDate, location, courseId
            🏢 departments: id, name, totalAcademicYears
            📁 files: id, name, type, size, uploadedByUserName, uploadDate
            📨 studentrequests: id, senderId, subject, message, status, date
            
            🧠 INTELLIGENT PROCESSING:
            - "users/students/lecturers" → users collection with role filter
            - "courses/classes" → courses collection
            - "assignments/homework" → assignments collection
            - "grades/scores" → studentgrades collection
            - "meetings/sessions" → meetings collection
            - "announcements/news" → announcements collection
            
            ⏰ DATE INTELLIGENCE:
            - "today" → filter by today's date range
            - "recent" → sort by createdAt: -1, limit: 20
            - "last year" → filter: {"createdAt": {"$gte": "%s"}}
            
            🔧 OPTIMIZATION:
            - Exclude sensitive fields: password, __v
            - Add reasonable limits (10-100)
            - Use indexed fields for sorting
            - Filter out deleted records when applicable
            
            EXAMPLES:
            
            Input: "show me all students"
            Output: {
              "collection": "users",
              "filter": {"role": "1300"},
              "fields": ["id", "username", "name", "email", "department"],
              "sort": {"name": 1},
              "limit": 50
            }
            
            Input: "recent announcements"
            Output: {
              "collection": "announcements",
              "filter": {"status": "active"},
              "fields": ["id", "title", "content", "creatorName", "createdAt"],
              "sort": {"createdAt": -1},
              "limit": 20
            }
            
            Remember: Be intelligent, efficient, and always return valid JSON!
            """, currentDateTime, getLastYear());
    }

    private String buildEnhancedUserPrompt(String originalPrompt, QueryContext context) {
        StringBuilder enhancedPrompt = new StringBuilder();
        enhancedPrompt.append("ENHANCED QUERY REQUEST:\n\n");
        enhancedPrompt.append("Original: ").append(originalPrompt).append("\n\n");

        enhancedPrompt.append("CONTEXT:\n");
        enhancedPrompt.append("- Suggested Collection: ").append(context.targetCollection).append("\n");
        enhancedPrompt.append("- Operation Type: ").append(context.operationType).append("\n");
        enhancedPrompt.append("- Complexity Level: ").append(context.complexityScore).append("/10\n");

        if (context.requiresAggregation) {
            enhancedPrompt.append("- Note: This query may benefit from aggregation operations\n");
        }

        enhancedPrompt.append("\nGenerate an optimized MongoDB query that addresses this request comprehensively.");

        return enhancedPrompt.toString();
    }

    // ============================================================================
    // API COMMUNICATION
    // ============================================================================

    private String callOpenAIAPI(String systemPrompt, String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        Map<String, Object> systemMessage = Map.of(
                "role", "system",
                "content", systemPrompt
        );

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", userPrompt
        );

        Map<String, Object> requestBody = Map.of(
                "model", openaiModel,
                "messages", List.of(systemMessage, userMessage),
                "temperature", 0.1,
                "max_tokens", maxTokens,
                "top_p", 0.9,
                "frequency_penalty", 0.2
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(openaiApiUrl, entity, Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null && body.containsKey("choices")) {
                var choices = (List<Map<String, Object>>) body.get("choices");
                var message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");

                return cleanResponseContent(content);
            }

            throw new RuntimeException("Invalid OpenAI API response structure");

        } catch (HttpClientErrorException e) {
            log.error("OpenAI API client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new RuntimeException("Invalid OpenAI API key");
            } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new RuntimeException("OpenAI API rate limit exceeded");
            }
            throw new RuntimeException("OpenAI API error: " + e.getMessage());
        } catch (HttpServerErrorException e) {
            log.error("OpenAI API server error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI API server error");
        } catch (ResourceAccessException e) {
            log.error("OpenAI API timeout or connection error: {}", e.getMessage());
            throw new RuntimeException("OpenAI API connection timeout");
        } catch (Exception e) {
            log.error("Unexpected error calling OpenAI API: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI API call failed");
        }
    }

    private String cleanResponseContent(String content) {
        // Remove any markdown formatting
        return content.replaceAll("```json\\s*", "")
                .replaceAll("```\\s*$", "")
                .replaceAll("^```.*\\n", "")
                .trim();
    }

    // ============================================================================
    // VALIDATION AND ENHANCEMENT
    // ============================================================================

    private String validateAndEnhanceQuery(String queryString, QueryContext context) {
        try {
            // Parse and validate JSON structure
            JsonNode queryNode = objectMapper.readTree(queryString);

            // Validate collection
            String collection = queryNode.path("collection").asText();
            if (!ALLOWED_COLLECTIONS.contains(collection)) {
                log.warn("Invalid collection {}, using suggested: {}", collection, context.targetCollection);
                collection = context.targetCollection;
            }

            // Security validation
            String queryStr = queryNode.toString().toLowerCase();
            for (String forbidden : FORBIDDEN_OPERATIONS) {
                if (queryStr.contains(forbidden.toLowerCase())) {
                    throw new SecurityException("Forbidden operation detected: " + forbidden);
                }
            }

            // Build enhanced query
            Map<String, Object> enhancedQuery = new HashMap<>();
            enhancedQuery.put("collection", collection);

            // Process filter
            if (queryNode.has("filter")) {
                enhancedQuery.put("filter", objectMapper.convertValue(queryNode.get("filter"), Map.class));
            } else {
                enhancedQuery.put("filter", Map.of("status", Map.of("$ne", "deleted")));
            }

            // Process other fields with enhancements
            if (queryNode.has("sort")) {
                enhancedQuery.put("sort", objectMapper.convertValue(queryNode.get("sort"), Map.class));
            }

            // Smart limit handling
            int limit = queryNode.path("limit").asInt(50);
            if (context.operationType.equals("top")) {
                limit = Math.min(limit, 20);
            } else if (context.operationType.equals("recent")) {
                limit = Math.min(limit, 100);
            }
            enhancedQuery.put("limit", Math.min(Math.max(limit, 1), 1000));

            // Process fields with security filtering
            if (queryNode.has("fields")) {
                List<String> fields = objectMapper.convertValue(queryNode.get("fields"), List.class);
                fields.removeIf(field -> Set.of("password", "__v").contains(field));
                enhancedQuery.put("fields", fields);
            }

            // Add skip if present
            if (queryNode.has("skip")) {
                int skip = Math.max(0, queryNode.get("skip").asInt());
                enhancedQuery.put("skip", skip);
            }

            // Add metadata for complex queries
            if (context.complexityScore > 5) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("queryType", context.isComplex ? "complex" : "standard");
                metadata.put("estimatedComplexity", context.complexityScore);
                metadata.put("operationType", context.operationType);
                enhancedQuery.put("metadata", metadata);
            }

            return objectMapper.writeValueAsString(enhancedQuery);

        } catch (Exception e) {
            log.error("Error validating and enhancing query: {}", e.getMessage(), e);
            return generateFallbackQuery(context.originalPrompt);
        }
    }

    private String generateFallbackQuery(String originalPrompt) {
        log.info("Generating fallback query for: {}", originalPrompt);

        // Simple fallback based on common patterns
        String collection = "users"; // Default
        if (originalPrompt.toLowerCase().contains("course")) collection = "courses";
        else if (originalPrompt.toLowerCase().contains("assignment")) collection = "assignments";
        else if (originalPrompt.toLowerCase().contains("grade")) collection = "studentgrades";
        else if (originalPrompt.toLowerCase().contains("meeting")) collection = "meetings";

        Map<String, Object> fallbackQuery = new HashMap<>();
        fallbackQuery.put("collection", collection);
        fallbackQuery.put("filter", Map.of("status", Map.of("$ne", "deleted")));
        fallbackQuery.put("sort", Map.of("createdAt", -1));
        fallbackQuery.put("limit", 20);
        fallbackQuery.put("fields", getDefaultFields(collection));

        try {
            return objectMapper.writeValueAsString(fallbackQuery);
        } catch (Exception e) {
            return String.format("""
                {
                  "collection": "%s",
                  "filter": {"status": {"$ne": "deleted"}},
                  "fields": ["id", "name"],
                  "limit": 10
                }
                """, collection);
        }
    }

    private List<String> getDefaultFields(String collection) {
        return switch (collection) {
            case "users" -> List.of("id", "username", "name", "email", "role", "department");
            case "courses" -> List.of("id", "name", "code", "description", "department", "credits");
            case "assignments" -> List.of("id", "title", "description", "dueDate", "status");
            case "studentgrades" -> List.of("id", "studentId", "courseId", "finalGrade", "finalLetterGrade");
            case "meetings" -> List.of("id", "title", "description", "scheduledAt", "status");
            case "announcements" -> List.of("id", "title", "content", "creatorName", "createdAt");
            default -> List.of("id", "name", "createdAt");
        };
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    private String getCurrentDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String getLastYear() {
        return LocalDateTime.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String getLastMonth() {
        return LocalDateTime.now().minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    // Health check method
    public boolean isApiHealthy() {
        try {
            Map<String, Object> testBody = Map.of(
                    "model", openaiModel,
                    "messages", List.of(Map.of("role", "user", "content", "test")),
                    "max_tokens", 10
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(testBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(openaiApiUrl, request, String.class);

            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("OpenAI API health check failed: {}", e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getServiceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("model", openaiModel);
        stats.put("maxTokens", maxTokens);
        stats.put("maxRetryAttempts", maxRetryAttempts);
        stats.put("allowedCollections", ALLOWED_COLLECTIONS);
        stats.put("cacheSize", queryCache.size());
        stats.put("apiHealthy", isApiHealthy());
        stats.put("version", "enhanced-2.0");
        return stats;
    }

    // ============================================================================
    // INNER CLASSES
    // ============================================================================

    private static class QueryContext {
        String originalPrompt;
        String cleanPrompt;
        String targetCollection;
        String operationType;
        boolean isComplex;
        boolean requiresAggregation;
        int complexityScore;
    }
}