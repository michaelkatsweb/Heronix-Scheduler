package com.heronix.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for OllamaAIService
 *
 * Prerequisites:
 * - Ollama server running on localhost:11434
 * - Model downloaded: ollama pull mistral:7b-instruct
 */
public class OllamaAIServiceTest {

    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String MODEL = "mistral:7b-instruct";
    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void testOllamaHealthCheck() {
        // Check if Ollama is available
        try {
            String healthUrl = OLLAMA_BASE_URL + "/api/tags";
            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            // Get available models
            List<Map<String, Object>> models = (List<Map<String, Object>>) response.getBody().get("models");
            assertNotNull(models);
            assertFalse(models.isEmpty(), "No models available in Ollama");

            System.out.println("=== Ollama Health Check PASSED ===");
            System.out.println("Available models: " + models.size());
            for (Map<String, Object> model : models) {
                System.out.println("  - " + model.get("name"));
            }
        } catch (Exception e) {
            fail("Ollama server not available: " + e.getMessage());
        }
    }

    @Test
    void testOllamaGenerate() {
        // Test simple generation
        try {
            String generateUrl = OLLAMA_BASE_URL + "/api/generate";

            Map<String, Object> request = new HashMap<>();
            request.put("model", MODEL);
            request.put("prompt", "Say 'Hello Heronix Scheduler!' and nothing else.");
            request.put("stream", false);
            request.put("options", Map.of(
                "temperature", 0.3,
                "num_predict", 50
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            System.out.println("Sending request to Ollama...");
            long startTime = System.currentTimeMillis();

            ResponseEntity<Map> response = restTemplate.postForEntity(generateUrl, entity, Map.class);

            long duration = System.currentTimeMillis() - startTime;

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            String aiResponse = (String) response.getBody().get("response");
            assertNotNull(aiResponse);
            assertFalse(aiResponse.isEmpty());

            System.out.println("=== Ollama Generate Test PASSED ===");
            System.out.println("Response: " + aiResponse.trim());
            System.out.println("Duration: " + duration + "ms");
        } catch (Exception e) {
            fail("Ollama generate failed: " + e.getMessage());
        }
    }

    @Test
    void testScheduleAnalysis() {
        // Test schedule analysis capability
        String scheduleData = """
            SCHEDULE SNAPSHOT:
            - Total Courses: 50
            - Fully Assigned: 40 (80%)
            - Partially Assigned: 5 (10%)
            - Unassigned: 5 (10%)

            TEACHER WORKLOAD:
            - 0 courses: 2 teachers
            - 1-3 courses: 5 teachers
            - 4-5 courses: 10 teachers (optimal)
            - 6+ courses: 3 teachers (overloaded)

            ISSUES:
            - Mr. Smith: 8 courses (overloaded)
            - Room 101 double-booked: Period 3, Monday
            - Biology 101 has no teacher assigned
            """;

        String prompt = """
            Analyze the following schedule data and identify the top 3 issues:

            """ + scheduleData + """

            Respond with exactly 3 bullet points, each starting with a severity
            (CRITICAL/WARNING/INFO).
            """;

        try {
            String generateUrl = OLLAMA_BASE_URL + "/api/generate";

            Map<String, Object> request = new HashMap<>();
            request.put("model", MODEL);
            request.put("prompt", prompt);
            request.put("stream", false);
            request.put("options", Map.of(
                "temperature", 0.5,
                "num_predict", 300
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            System.out.println("Analyzing schedule with AI...");
            long startTime = System.currentTimeMillis();

            ResponseEntity<Map> response = restTemplate.postForEntity(generateUrl, entity, Map.class);

            long duration = System.currentTimeMillis() - startTime;

            assertEquals(HttpStatus.OK, response.getStatusCode());

            String aiResponse = (String) response.getBody().get("response");
            assertNotNull(aiResponse);

            System.out.println("=== Schedule Analysis Test PASSED ===");
            System.out.println("AI Analysis:\n" + aiResponse.trim());
            System.out.println("\nDuration: " + duration + "ms");

        } catch (Exception e) {
            fail("Schedule analysis failed: " + e.getMessage());
        }
    }
}
