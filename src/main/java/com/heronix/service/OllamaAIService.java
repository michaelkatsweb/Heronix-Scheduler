package com.heronix.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for interacting with Ollama AI (Local LLM Server)
 *
 * Features:
 * - Completely offline operation (localhost only)
 * - Background schedule analysis
 * - Interactive chat with conversation history
 * - Proactive issue detection
 * - No internet required after model download
 *
 * Prerequisites:
 * - Ollama server running on localhost:11434
 * - Model downloaded (e.g., mistral:7b-instruct)
 *
 * @since Phase 4 - AI Integration
 * @version 1.0.0
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.ollama.enabled", havingValue = "true", matchIfMissing = false)
public class OllamaAIService {

    // Ollama server configuration
    @Value("${ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ai.ollama.model:mistral:7b-instruct}")
    private String defaultModel;

    @Value("${ai.ollama.timeout:30000}")
    private int timeoutMs;

    private final RestTemplate restTemplate;

    // Conversation history per session
    private final Map<String, List<Map<String, String>>> conversationHistory = new ConcurrentHashMap<>();

    // System prompt for scheduling context
    private static final String SYSTEM_PROMPT = """
        You are an AI assistant integrated into Heronix Scheduling System, a comprehensive school scheduling system.
        Your role is to help school administrators manage schedules, detect conflicts, and optimize resource allocation.

        Context:
        - You're analyzing schedules for a high school
        - Teachers typically handle 5-6 courses per year
        - Teachers should be certified for subjects they teach
        - Rooms should match course requirements (Science → Lab, PE → Gym)
        - Room capacity should accommodate enrolled students
        - Course sequences (English 1 → English 2) should have consistent teachers

        Response Guidelines:
        - Be concise and actionable (2-4 sentences for alerts, more for detailed analysis)
        - Prioritize critical issues (certification mismatches, overloaded teachers, capacity problems)
        - Provide specific solutions, not just problems
        - Use friendly, professional tone
        - Format lists with bullet points for readability

        When analyzing schedules, focus on:
        1. Teacher certification and workload balance
        2. Room type and capacity matching
        3. Schedule conflicts and overlaps
        4. Course sequence consistency
        5. Resource utilization efficiency
        """;

    public OllamaAIService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Check if Ollama server is available and responsive
     */
    public boolean isOllamaAvailable() {
        try {
            String healthUrl = ollamaBaseUrl + "/api/tags";
            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);
            boolean available = response.getStatusCode() == HttpStatus.OK;

            if (available) {
                log.info("Ollama server is available at {}", ollamaBaseUrl);
            } else {
                log.warn("Ollama server returned unexpected status: {}", response.getStatusCode());
            }

            return available;
        } catch (Exception e) {
            log.error("Ollama server not available at {}: {}", ollamaBaseUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Generate a one-off response (no conversation history)
     * Use for: Background analysis, quick checks, one-time questions
     *
     * @param prompt The question or analysis request
     * @return AI response or empty if error
     */
    public Optional<String> generate(String prompt) {
        return generate(prompt, defaultModel);
    }

    /**
     * Generate a one-off response with specific model
     *
     * @param prompt The question or analysis request
     * @param model The Ollama model to use (e.g., "mistral:7b-instruct")
     * @return AI response or empty if error
     */
    public Optional<String> generate(String prompt, String model) {
        try {
            log.info("Generating AI response with model: {}", model);
            log.debug("Prompt: {}", prompt);

            String generateUrl = ollamaBaseUrl + "/api/generate";

            // Build request with system context
            Map<String, Object> request = new HashMap<>();
            request.put("model", model);
            request.put("prompt", SYSTEM_PROMPT + "\n\nUser Request:\n" + prompt);
            request.put("stream", false);
            request.put("options", Map.of(
                "temperature", 0.7,
                "top_p", 0.9,
                "num_predict", 500  // Max tokens for response
            ));

            // Send request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(generateUrl, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String aiResponse = (String) response.getBody().get("response");
                log.info("AI response generated successfully ({} chars)", aiResponse.length());
                log.debug("Response: {}", aiResponse);
                return Optional.of(aiResponse.trim());
            } else {
                log.error("Unexpected response from Ollama: {}", response.getStatusCode());
                return Optional.empty();
            }

        } catch (ResourceAccessException e) {
            log.error("Cannot connect to Ollama server at {}: {}", ollamaBaseUrl, e.getMessage());
            log.error("Please ensure Ollama is running: ollama serve");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error generating AI response", e);
            return Optional.empty();
        }
    }

    /**
     * Start or continue a chat conversation with conversation history
     * Use for: Interactive AI assistant, multi-turn questions
     *
     * @param sessionId Unique session identifier (e.g., user ID)
     * @param userMessage User's message
     * @return AI response or empty if error
     */
    public Optional<String> chat(String sessionId, String userMessage) {
        return chat(sessionId, userMessage, defaultModel);
    }

    /**
     * Chat with specific model
     *
     * @param sessionId Unique session identifier
     * @param userMessage User's message
     * @param model The Ollama model to use
     * @return AI response or empty if error
     */
    public Optional<String> chat(String sessionId, String userMessage, String model) {
        try {
            log.info("Chat request for session: {}, model: {}", sessionId, model);
            log.debug("User message: {}", userMessage);

            // Get or create conversation history
            List<Map<String, String>> history = conversationHistory.computeIfAbsent(
                sessionId, k -> new ArrayList<>());

            String chatUrl = ollamaBaseUrl + "/api/chat";

            // Build messages list
            List<Map<String, String>> messages = new ArrayList<>();

            // Add system prompt (once)
            if (history.isEmpty()) {
                messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
            }

            // Add conversation history
            messages.addAll(history);

            // Add current user message
            messages.add(Map.of("role", "user", "content", userMessage));

            // Build request
            Map<String, Object> request = new HashMap<>();
            request.put("model", model);
            request.put("messages", messages);
            request.put("stream", false);
            request.put("options", Map.of(
                "temperature", 0.7,
                "top_p", 0.9,
                "num_predict", 500
            ));

            // Send request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(chatUrl, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> messageObj = (Map<String, Object>) response.getBody().get("message");
                String aiResponse = (String) messageObj.get("content");

                // Update conversation history
                history.add(Map.of("role", "user", "content", userMessage));
                history.add(Map.of("role", "assistant", "content", aiResponse));

                // Limit history to last 10 exchanges (20 messages)
                if (history.size() > 20) {
                    history.subList(0, history.size() - 20).clear();
                }

                log.info("AI chat response generated successfully ({} chars)", aiResponse.length());
                log.debug("Response: {}", aiResponse);
                return Optional.of(aiResponse.trim());
            } else {
                log.error("Unexpected response from Ollama: {}", response.getStatusCode());
                return Optional.empty();
            }

        } catch (ResourceAccessException e) {
            log.error("Cannot connect to Ollama server at {}: {}", ollamaBaseUrl, e.getMessage());
            log.error("Please ensure Ollama is running: ollama serve");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error in AI chat", e);
            return Optional.empty();
        }
    }

    /**
     * Clear conversation history for a session
     */
    public void clearConversation(String sessionId) {
        conversationHistory.remove(sessionId);
        log.info("Cleared conversation history for session: {}", sessionId);
    }

    /**
     * Clear all conversation histories
     */
    public void clearAllConversations() {
        conversationHistory.clear();
        log.info("Cleared all conversation histories");
    }

    /**
     * Get available models from Ollama server
     */
    public List<String> getAvailableModels() {
        try {
            String tagsUrl = ollamaBaseUrl + "/api/tags";
            ResponseEntity<Map> response = restTemplate.getForEntity(tagsUrl, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> models = (List<Map<String, Object>>) response.getBody().get("models");
                return models.stream()
                    .map(m -> (String) m.get("name"))
                    .toList();
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error fetching available models", e);
            return List.of();
        }
    }

    /**
     * Analyze schedule data and provide recommendations
     * This is the main entry point for background schedule analysis
     *
     * @param scheduleData JSON or formatted string with schedule information
     * @return AI analysis and recommendations
     */
    public Optional<String> analyzeSchedule(String scheduleData) {
        String prompt = String.format("""
            Analyze the following schedule data and identify any issues or optimization opportunities:

            %s

            Focus on:
            1. Critical issues (certification mismatches, overloaded teachers, capacity problems)
            2. Optimization opportunities (workload balance, resource utilization)
            3. Specific actionable recommendations

            Provide a concise summary (3-5 bullet points) of the most important findings.
            """, scheduleData);

        return generate(prompt);
    }

    /**
     * Get quick suggestions for a specific course assignment
     *
     * @param courseInfo Course details (name, subject, enrollment)
     * @param availableTeachers List of available teachers with their info
     * @return AI recommendation for best teacher match
     */
    public Optional<String> suggestTeacherForCourse(String courseInfo, String availableTeachers) {
        String prompt = String.format("""
            Course to assign: %s

            Available teachers: %s

            Which teacher would be the best match? Consider certification, workload, and subject expertise.
            Provide ONE recommendation with a brief explanation (2 sentences).
            """, courseInfo, availableTeachers);

        return generate(prompt);
    }

    /**
     * Detect potential schedule conflicts
     *
     * @param scheduleData Schedule information including time slots, rooms, teachers
     * @return List of detected conflicts with severity
     */
    public Optional<String> detectConflicts(String scheduleData) {
        String prompt = String.format("""
            Review this schedule for conflicts:

            %s

            Identify:
            - Teacher double-bookings (same teacher, overlapping times)
            - Room conflicts (same room, overlapping times)
            - Student conflicts (if applicable)

            List conflicts in order of severity (High/Medium/Low).
            """, scheduleData);

        return generate(prompt);
    }
}
