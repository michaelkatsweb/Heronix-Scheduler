package com.heronix.service.impl;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for bundled documentation viewer
 *
 * Features:
 * - Embedded markdown documentation
 * - Searchable help topics
 * - Context-sensitive help
 * - Tutorial walkthroughs
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 14 - Documentation Viewer
 */
@Slf4j
@Service
public class DocumentationService {

    private Map<String, DocumentationTopic> topics = new LinkedHashMap<>();
    private Map<String, List<String>> searchIndex = new HashMap<>();

    public DocumentationService() {
        initializeDocumentation();
        buildSearchIndex();
    }

    /**
     * Initialize embedded documentation
     */
    private void initializeDocumentation() {
        // Getting Started
        topics.put("getting-started", DocumentationTopic.builder()
            .id("getting-started")
            .title("Getting Started with Heronix Scheduling System")
            .category("Introduction")
            .content("""
                # Getting Started with Heronix Scheduling System

                Welcome to Heronix Scheduling System, your comprehensive school scheduling solution.

                ## Quick Start Guide

                ### 1. Initial Setup
                1. Launch the application
                2. Log in with your administrator credentials
                3. Navigate to **Settings** to configure your school's basic information

                ### 2. Import Your Data
                You can import data via CSV files:
                - **Teachers**: Import staff information including certifications
                - **Students**: Import student roster with grade levels
                - **Courses**: Import course catalog
                - **Rooms**: Import available rooms and capacities

                ### 3. Generate Your Schedule
                1. Go to **Schedule** → **Generate Schedule**
                2. Select the scheduling parameters
                3. Click **Generate** and let the AI optimize your schedule

                ## Key Features
                - AI-powered schedule generation
                - Conflict detection and resolution
                - IEP/504 compliance tracking
                - Multi-campus federation support
                - Advanced analytics dashboards
                """)
            .keywords(List.of("start", "setup", "begin", "introduction", "quick", "guide"))
            .build());

        // Schedule Generation
        topics.put("schedule-generation", DocumentationTopic.builder()
            .id("schedule-generation")
            .title("Schedule Generation")
            .category("Core Features")
            .content("""
                # Schedule Generation

                Heronix Scheduling System uses advanced AI algorithms to generate optimal schedules.

                ## Generation Options

                ### Algorithm Selection
                - **OptaPlanner**: Constraint-based optimization (recommended)
                - **Genetic Algorithm**: Evolution-based optimization
                - **Simulated Annealing**: Temperature-based search
                - **Hybrid**: Combines multiple approaches

                ### Constraints
                The scheduler respects these constraints:
                - Teacher availability
                - Room capacity
                - Course requirements
                - IEP/504 mandated minutes
                - Lunch periods
                - Prep time requirements

                ## Running a Generation

                1. Navigate to **Schedule** → **Generate Schedule**
                2. Select academic year and term
                3. Choose algorithm (OptaPlanner recommended)
                4. Set constraint weights
                5. Click **Generate**

                ## Partial Scheduling
                You can lock existing assignments and only schedule unassigned courses:
                1. Check **Preserve Existing Assignments**
                2. Select which courses to schedule
                3. Generate to fill in gaps
                """)
            .keywords(List.of("generate", "create", "algorithm", "optaplanner", "genetic", "schedule"))
            .build());

        // Conflict Resolution
        topics.put("conflict-resolution", DocumentationTopic.builder()
            .id("conflict-resolution")
            .title("Conflict Detection & Resolution")
            .category("Core Features")
            .content("""
                # Conflict Detection & Resolution

                Heronix Scheduling System automatically detects and helps resolve scheduling conflicts.

                ## Types of Conflicts

                ### Teacher Conflicts
                - Double-booked teachers
                - Exceeding max periods per day
                - Back-to-back classes without breaks

                ### Room Conflicts
                - Double-booked rooms
                - Capacity exceeded
                - Equipment requirements not met

                ### Student Conflicts
                - Required courses at same time
                - Lunch period conflicts
                - IEP service conflicts

                ## Resolution Strategies

                ### Automatic Resolution
                The AI will suggest resolutions ranked by:
                - Minimal disruption score
                - Number of students affected
                - Teacher preference compliance

                ### Manual Resolution
                1. View conflict in **Conflict Dashboard**
                2. Click on conflict to see options
                3. Select preferred resolution
                4. Apply changes
                """)
            .keywords(List.of("conflict", "resolution", "double-book", "overlap", "problem", "fix"))
            .build());

        // IEP Management
        topics.put("iep-management", DocumentationTopic.builder()
            .id("iep-management")
            .title("IEP & 504 Management")
            .category("Special Education")
            .content("""
                # IEP & 504 Plan Management

                Comprehensive support for special education scheduling requirements.

                ## IEP Features

                ### Service Tracking
                - Track mandated minutes per service type
                - Monitor compliance percentage
                - Alert on under-scheduled services

                ### IEP Wizard
                1. Navigate to **Students** → select student → **IEP**
                2. Enter service requirements
                3. Set provider preferences
                4. Generate compliant schedule

                ## 504 Accommodations

                Track and apply accommodations:
                - Extended time
                - Preferential seating
                - Modified assignments
                - Testing accommodations

                ## Compliance Dashboard

                View real-time compliance metrics:
                - % of IEP minutes scheduled
                - Students at risk of non-compliance
                - Service provider availability
                """)
            .keywords(List.of("iep", "504", "special", "education", "sped", "accommodation", "compliance"))
            .build());

        // Analytics
        topics.put("analytics", DocumentationTopic.builder()
            .id("analytics")
            .title("Analytics & Reports")
            .category("Analytics")
            .content("""
                # Analytics & Reports

                Heronix Scheduling System provides comprehensive analytics for schedule optimization.

                ## Available Dashboards

                ### Teacher Analytics
                - Burnout risk scores
                - Workload distribution
                - Prep time equity
                - Back-to-back class analysis

                ### Student Analytics
                - IEP compliance tracking
                - Course enrollment trends
                - Conflict frequency

                ### Room Analytics
                - Utilization rates
                - Peak usage times
                - Underutilized spaces

                ## Predictive Analytics

                ### Enrollment Forecasting
                - ML-based enrollment predictions
                - Capacity planning alerts
                - Trend analysis

                ### What-If Scenarios
                - Teacher removal impact
                - Room closure simulation
                - Enrollment change modeling

                ## Export Options
                - PDF reports
                - Excel spreadsheets
                - CSV data exports
                """)
            .keywords(List.of("analytics", "report", "dashboard", "metrics", "statistics", "burnout", "utilization"))
            .build());

        // Multi-Campus
        topics.put("multi-campus", DocumentationTopic.builder()
            .id("multi-campus")
            .title("Multi-Campus Federation")
            .category("Administration")
            .content("""
                # Multi-Campus Federation

                Manage multiple campuses within a district.

                ## Federation Features

                ### District Management
                - Create and manage districts
                - Configure federation policies
                - Set calendar types

                ### Campus Operations
                - Add/remove campuses
                - Track enrollment by campus
                - Monitor capacity utilization

                ### Cross-Campus Features

                #### Shared Teachers
                - Assign teachers to multiple campuses
                - Track teaching loads across locations
                - Coordinate schedules

                #### Cross-Campus Enrollment
                - Allow students to take courses at other campuses
                - Track transportation requirements
                - Manage course availability

                ## Capacity Analysis
                - District-wide capacity view
                - Overcrowded/underutilized alerts
                - Resource sharing recommendations
                """)
            .keywords(List.of("campus", "district", "federation", "multi", "shared", "cross"))
            .build());

        // Keyboard Shortcuts
        topics.put("keyboard-shortcuts", DocumentationTopic.builder()
            .id("keyboard-shortcuts")
            .title("Keyboard Shortcuts")
            .category("Reference")
            .content("""
                # Keyboard Shortcuts

                Quick access keys for common operations.

                ## Navigation
                | Shortcut | Action |
                |----------|--------|
                | Ctrl+H | Go to Dashboard |
                | Ctrl+S | Save current view |
                | Ctrl+Z | Undo last action |
                | Ctrl+Y | Redo action |
                | Ctrl+F | Global search |
                | Escape | Close dialog |

                ## Schedule Editing
                | Shortcut | Action |
                |----------|--------|
                | Ctrl+G | Generate schedule |
                | Ctrl+D | Duplicate slot |
                | Delete | Remove selected slot |
                | Ctrl+Arrow | Move slot |

                ## Data Management
                | Shortcut | Action |
                |----------|--------|
                | Ctrl+I | Import data |
                | Ctrl+E | Export data |
                | Ctrl+P | Print |
                | Ctrl+N | New record |
                """)
            .keywords(List.of("keyboard", "shortcut", "hotkey", "key", "quick", "access"))
            .build());

        // Troubleshooting
        topics.put("troubleshooting", DocumentationTopic.builder()
            .id("troubleshooting")
            .title("Troubleshooting Guide")
            .category("Support")
            .content("""
                # Troubleshooting Guide

                Common issues and solutions.

                ## Schedule Generation Issues

                ### "No feasible solution found"
                - Check for hard constraint violations
                - Verify teacher availability covers all courses
                - Ensure room capacity is sufficient
                - Review IEP requirements for conflicts

                ### Generation takes too long
                - Reduce optimization time limit
                - Use partial scheduling
                - Consider constraint relaxation

                ## Import Issues

                ### CSV import fails
                - Verify column headers match expected format
                - Check for special characters in data
                - Ensure dates are in correct format
                - Remove empty rows

                ### Duplicate records
                - Use unique identifiers
                - Enable "Update Existing" option

                ## Performance Issues

                ### Application runs slowly
                - Close unused dashboards
                - Clear browser cache
                - Increase Java heap size
                - Check database connections

                ## Getting Help
                - Email: support@eduscheduler.com
                - Report issues: github.com/eduscheduler/issues
                """)
            .keywords(List.of("troubleshoot", "problem", "error", "issue", "help", "fix", "solution"))
            .build());
    }

    /**
     * Build search index for quick lookups
     */
    private void buildSearchIndex() {
        // ✅ NULL SAFE: Validate topics map
        if (topics == null || topics.values() == null) {
            log.warn("Topics map is null, cannot build search index");
            return;
        }

        for (DocumentationTopic topic : topics.values()) {
            // ✅ NULL SAFE: Skip null topics
            if (topic == null || topic.getId() == null) {
                continue;
            }

            // Index title words
            // ✅ NULL SAFE: Validate title before processing
            if (topic.getTitle() != null) {
                for (String word : topic.getTitle().toLowerCase().split("\\s+")) {
                    if (word != null && !word.isEmpty()) {
                        searchIndex.computeIfAbsent(word, k -> new ArrayList<>()).add(topic.getId());
                    }
                }
            }

            // Index keywords
            // ✅ NULL SAFE: Validate keywords list
            if (topic.getKeywords() != null) {
                for (String keyword : topic.getKeywords()) {
                    if (keyword != null && !keyword.isEmpty()) {
                        searchIndex.computeIfAbsent(keyword.toLowerCase(), k -> new ArrayList<>())
                            .add(topic.getId());
                    }
                }
            }

            // Index content words (simplified)
            // ✅ NULL SAFE: Validate content before processing
            if (topic.getContent() != null) {
                for (String word : topic.getContent().toLowerCase().split("\\s+")) {
                    if (word != null && word.length() > 3) {
                        word = word.replaceAll("[^a-z]", "");
                        if (!word.isEmpty()) {
                            searchIndex.computeIfAbsent(word, k -> new ArrayList<>()).add(topic.getId());
                        }
                    }
                }
            }
        }
    }

    /**
     * Get all documentation topics
     */
    public List<DocumentationTopic> getAllTopics() {
        return new ArrayList<>(topics.values());
    }

    /**
     * Get topics by category
     */
    public List<DocumentationTopic> getTopicsByCategory(String category) {
        // ✅ NULL SAFE: Validate category parameter
        if (category == null) {
            log.warn("Cannot get topics for null category");
            return Collections.emptyList();
        }

        return topics.values().stream()
            // ✅ NULL SAFE: Filter null topics and validate category
            .filter(t -> t != null && t.getCategory() != null && t.getCategory().equalsIgnoreCase(category))
            .collect(Collectors.toList());
    }

    /**
     * Get all categories
     */
    public List<String> getCategories() {
        return topics.values().stream()
            // ✅ NULL SAFE: Filter null topics and null categories
            .filter(t -> t != null && t.getCategory() != null)
            .map(DocumentationTopic::getCategory)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Get topic by ID
     */
    public Optional<DocumentationTopic> getTopic(String topicId) {
        return Optional.ofNullable(topics.get(topicId));
    }

    /**
     * Search documentation
     */
    public List<SearchResult> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Integer> scores = new HashMap<>();
        String[] words = query.toLowerCase().split("\\s+");

        // ✅ NULL SAFE: Validate words array
        if (words == null) {
            return Collections.emptyList();
        }

        for (String word : words) {
            // ✅ NULL SAFE: Skip null or empty words
            if (word == null || word.isEmpty()) {
                continue;
            }

            List<String> matchingTopics = searchIndex.getOrDefault(word, Collections.emptyList());
            // ✅ NULL SAFE: Validate matching topics list
            if (matchingTopics != null) {
                for (String topicId : matchingTopics) {
                    if (topicId != null) {
                        scores.merge(topicId, 1, Integer::sum);
                    }
                }
            }

            // Partial match
            for (String indexWord : searchIndex.keySet()) {
                // ✅ NULL SAFE: Validate index word before comparison
                if (indexWord != null && (indexWord.startsWith(word) || word.startsWith(indexWord))) {
                    List<String> partialTopics = searchIndex.get(indexWord);
                    // ✅ NULL SAFE: Validate partial topics list
                    if (partialTopics != null) {
                        for (String topicId : partialTopics) {
                            if (topicId != null) {
                                scores.merge(topicId, 1, Integer::sum);
                            }
                        }
                    }
                }
            }
        }

        return scores.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .map(e -> {
                DocumentationTopic topic = topics.get(e.getKey());
                // ✅ NULL SAFE: Validate topic lookup result
                if (topic == null) {
                    log.warn("Topic not found for ID: {}", e.getKey());
                    return null;
                }

                return SearchResult.builder()
                    .topicId(e.getKey())
                    .title(topic.getTitle() != null ? topic.getTitle() : "Unknown")
                    .category(topic.getCategory() != null ? topic.getCategory() : "Unknown")
                    .relevanceScore(e.getValue())
                    .snippet(extractSnippet(topic.getContent(), query))
                    .build();
            })
            // ✅ NULL SAFE: Filter null search results
            .filter(result -> result != null)
            .collect(Collectors.toList());
    }

    /**
     * Get context-sensitive help for a screen
     */
    public Optional<DocumentationTopic> getContextHelp(String screenId) {
        // ✅ NULL SAFE: Validate screenId parameter
        if (screenId == null) {
            log.warn("Screen ID is null, returning default help");
            return getTopic("getting-started");
        }

        return switch (screenId.toLowerCase()) {
            case "dashboard", "home" -> getTopic("getting-started");
            case "schedule", "generator" -> getTopic("schedule-generation");
            case "conflicts" -> getTopic("conflict-resolution");
            case "iep", "504", "sped" -> getTopic("iep-management");
            case "analytics", "reports" -> getTopic("analytics");
            case "federation", "campus" -> getTopic("multi-campus");
            default -> getTopic("getting-started");
        };
    }

    private String extractSnippet(String content, String query) {
        // ✅ NULL SAFE: Validate parameters
        if (content == null || query == null) {
            return "";
        }

        // ✅ NULL SAFE: Handle empty content
        if (content.isEmpty()) {
            return "";
        }

        String lowerContent = content.toLowerCase();
        String lowerQuery = query.toLowerCase();

        int idx = lowerContent.indexOf(lowerQuery);
        if (idx < 0) {
            // Find first query word
            String[] queryWords = lowerQuery.split("\\s+");
            // ✅ NULL SAFE: Validate query words array
            if (queryWords != null) {
                for (String word : queryWords) {
                    if (word != null && !word.isEmpty()) {
                        idx = lowerContent.indexOf(word);
                        if (idx >= 0) break;
                    }
                }
            }
        }

        if (idx < 0) {
            return content.substring(0, Math.min(150, content.length())) + "...";
        }

        int start = Math.max(0, idx - 50);
        int end = Math.min(content.length(), idx + 100);

        String snippet = content.substring(start, end);
        if (start > 0) snippet = "..." + snippet;
        if (end < content.length()) snippet = snippet + "...";

        return snippet.replaceAll("\\s+", " ").trim();
    }

    // ========================================================================
    // DTOs
    // ========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentationTopic {
        private String id;
        private String title;
        private String category;
        private String content;
        private List<String> keywords;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private String topicId;
        private String title;
        private String category;
        private int relevanceScore;
        private String snippet;
    }
}
