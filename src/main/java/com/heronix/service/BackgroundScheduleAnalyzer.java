package com.heronix.service;

import com.heronix.model.domain.Course;
import com.heronix.model.domain.Teacher;
import com.heronix.model.dto.AIAnalysisResult;
import com.heronix.model.dto.AIIssue;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.TeacherRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * Background Schedule Analyzer
 *
 * Automatically analyzes schedules using AI and detects issues.
 * Runs on a configurable schedule (default: every 10 minutes).
 *
 * Features:
 * - Automatic schedule health checks
 * - AI-powered issue detection
 * - Proactive administrator alerts
 * - Issue history tracking
 * - Performance metrics
 *
 * @since Phase 2 - Background Analysis
 * @version 1.0.0
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.ollama.background-analysis.enabled", havingValue = "true")
public class BackgroundScheduleAnalyzer {

    @Autowired
    private OllamaAIService aiService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ScheduleIssueDetector issueDetector;

    // Track last analysis results
    private AIAnalysisResult lastAnalysis;
    private LocalDateTime lastAnalysisTime;
    private int consecutiveFailures = 0;

    // Issue history (keep last 50 analyses)
    private final List<AIAnalysisResult> analysisHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 50;

    /**
     * Scheduled analysis - runs every X minutes (configured in application.yml)
     * Default: Every 10 minutes
     */
    @Scheduled(fixedDelayString = "${ai.ollama.background-analysis.interval:600000}")
    public void analyzeScheduleInBackground() {
        log.info("=== Starting background schedule analysis ===");

        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Check if AI is available
            if (!aiService.isOllamaAvailable()) {
                log.warn("Ollama AI not available - skipping analysis");
                consecutiveFailures++;

                if (consecutiveFailures >= 3) {
                    log.error("AI has been unavailable for {} consecutive checks. Please verify Ollama is running.",
                        consecutiveFailures);
                }
                return;
            }

            // Reset failure counter on success
            consecutiveFailures = 0;

            // Step 2: Build schedule snapshot
            String scheduleData = buildScheduleSnapshot();

            // Step 3: Detect issues using rule-based detector
            List<AIIssue> detectedIssues = issueDetector.detectAllIssues();

            // Step 4: Get AI analysis
            Optional<String> aiAnalysis = aiService.analyzeSchedule(scheduleData);

            // Step 5: Build analysis result
            AIAnalysisResult result = buildAnalysisResult(
                aiAnalysis.orElse("AI analysis unavailable"),
                detectedIssues,
                System.currentTimeMillis() - startTime
            );

            // Step 6: Store results
            lastAnalysis = result;
            lastAnalysisTime = LocalDateTime.now();
            addToHistory(result);

            // Step 7: Log results
            logAnalysisResults(result);

            // Step 8: Trigger alerts if needed
            if (result.hasCriticalIssues()) {
                triggerCriticalAlert(result);
            }

            log.info("=== Background analysis complete in {}ms ===",
                System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Error during background schedule analysis", e);
            consecutiveFailures++;
        }
    }

    /**
     * Build snapshot of current schedule for AI analysis
     */
    private String buildScheduleSnapshot() {
        StringBuilder snapshot = new StringBuilder();
        snapshot.append("=== SCHEDULE SNAPSHOT ===\n");
        snapshot.append("Generated: ").append(LocalDateTime.now()).append("\n\n");

        // Course statistics
        List<Course> allCourses = courseRepository.findAll();
        long totalCourses = allCourses.size();
        long fullyAssigned = allCourses.stream().filter(Course::isFullyAssigned).count();
        long partiallyAssigned = allCourses.stream().filter(Course::isPartiallyAssigned).count();
        long unassigned = allCourses.stream().filter(Course::isUnassigned).count();

        snapshot.append("COURSE SUMMARY:\n");
        snapshot.append(String.format("- Total: %d courses\n", totalCourses));
        snapshot.append(String.format("- Fully Assigned: %d (%.1f%%)\n",
            fullyAssigned, totalCourses > 0 ? (fullyAssigned * 100.0 / totalCourses) : 0));
        snapshot.append(String.format("- Partially Assigned: %d (%.1f%%)\n",
            partiallyAssigned, totalCourses > 0 ? (partiallyAssigned * 100.0 / totalCourses) : 0));
        snapshot.append(String.format("- Unassigned: %d (%.1f%%)\n\n",
            unassigned, totalCourses > 0 ? (unassigned * 100.0 / totalCourses) : 0));

        // Teacher workload analysis
        List<Teacher> allTeachers = teacherRepository.findAllWithCourses();
        snapshot.append("TEACHER WORKLOAD:\n");

        Map<String, Long> workloadDistribution = new HashMap<>();
        workloadDistribution.put("0 courses", allTeachers.stream().filter(t -> t.getCourseCount() == 0).count());
        workloadDistribution.put("1-3 courses", allTeachers.stream().filter(t -> t.getCourseCount() >= 1 && t.getCourseCount() <= 3).count());
        workloadDistribution.put("4-5 courses", allTeachers.stream().filter(t -> t.getCourseCount() >= 4 && t.getCourseCount() <= 5).count());
        workloadDistribution.put("6+ courses", allTeachers.stream().filter(t -> t.getCourseCount() >= 6).count());

        for (Map.Entry<String, Long> entry : workloadDistribution.entrySet()) {
            snapshot.append(String.format("- %s: %d teachers\n", entry.getKey(), entry.getValue()));
        }

        // Overloaded teachers detail
        List<Teacher> overloaded = allTeachers.stream()
            .filter(t -> t.getCourseCount() > 6)
            .sorted(Comparator.comparingInt(Teacher::getCourseCount).reversed())
            .limit(5)
            .collect(Collectors.toList());

        if (!overloaded.isEmpty()) {
            snapshot.append("\nOVERLOADED TEACHERS (Top 5):\n");
            for (Teacher teacher : overloaded) {
                snapshot.append(String.format("- %s: %d courses (limit: 6)\n",
                    teacher.getName(), teacher.getCourseCount()));
            }
        }

        // Underutilized teachers
        List<Teacher> underutilized = allTeachers.stream()
            .filter(t -> t.getActive() && t.getCourseCount() <= 2)
            .sorted(Comparator.comparingInt(Teacher::getCourseCount))
            .limit(5)
            .collect(Collectors.toList());

        if (!underutilized.isEmpty()) {
            snapshot.append("\nUNDERUTILIZED TEACHERS (Top 5):\n");
            for (Teacher teacher : underutilized) {
                snapshot.append(String.format("- %s: %d courses (capacity available)\n",
                    teacher.getName(), teacher.getCourseCount()));
            }
        }

        // Recent changes (if any tracking exists)
        snapshot.append("\n=== END SNAPSHOT ===");

        return snapshot.toString();
    }

    /**
     * Build comprehensive analysis result
     */
    private AIAnalysisResult buildAnalysisResult(String aiSummary, List<AIIssue> detectedIssues, long analysisTime) {
        AIAnalysisResult result = AIAnalysisResult.builder()
            .timestamp(LocalDateTime.now())
            .model("mistral:7b-instruct")
            .summary(aiSummary)
            .analysisTimeMs(analysisTime)
            .aiAvailable(true)
            .build();

        // Categorize issues by severity
        for (AIIssue issue : detectedIssues) {
            switch (issue.getSeverity()) {
                case CRITICAL -> result.getCriticalIssues().add(issue);
                case WARNING -> result.getWarnings().add(issue);
                case INFO -> result.getSuggestions().add(issue);
            }
        }

        // Calculate health score (0-100)
        int healthScore = calculateHealthScore(detectedIssues);
        result.setHealthScore(healthScore);

        return result;
    }

    /**
     * Calculate overall schedule health score (0-100)
     *
     * Scoring:
     * - Start at 100
     * - Subtract 15 points per critical issue
     * - Subtract 5 points per warning
     * - Subtract 1 point per suggestion
     * - Minimum score: 0
     */
    private int calculateHealthScore(List<AIIssue> issues) {
        int score = 100;

        for (AIIssue issue : issues) {
            switch (issue.getSeverity()) {
                case CRITICAL -> score -= 15;
                case WARNING -> score -= 5;
                case INFO -> score -= 1;
            }
        }

        return Math.max(0, score);
    }

    /**
     * Add result to history (maintain max size)
     */
    private void addToHistory(AIAnalysisResult result) {
        analysisHistory.add(result);

        // Keep only last MAX_HISTORY_SIZE results
        if (analysisHistory.size() > MAX_HISTORY_SIZE) {
            analysisHistory.remove(0);
        }
    }

    /**
     * Log analysis results to console
     */
    private void logAnalysisResults(AIAnalysisResult result) {
        log.info("Analysis Results:");
        log.info("- Health Score: {}/100", result.getHealthScore());
        log.info("- Critical Issues: {}", result.getCriticalIssues().size());
        log.info("- Warnings: {}", result.getWarnings().size());
        log.info("- Suggestions: {}", result.getSuggestions().size());
        log.info("- Analysis Time: {}ms", result.getAnalysisTimeMs());

        if (!result.getCriticalIssues().isEmpty()) {
            log.warn("=== CRITICAL ISSUES DETECTED ===");
            for (AIIssue issue : result.getCriticalIssues()) {
                log.warn("  {} {}: {}", issue.getSeverityIcon(), issue.getType(), issue.getDescription());
            }
        }

        if (!result.getWarnings().isEmpty() && log.isDebugEnabled()) {
            log.debug("=== WARNINGS ===");
            for (AIIssue issue : result.getWarnings()) {
                log.debug("  {} {}: {}", issue.getSeverityIcon(), issue.getType(), issue.getDescription());
            }
        }
    }

    /**
     * Trigger critical alert (will be displayed to administrator)
     *
     * Future: Implement pop-up notification system
     * For now: Log prominently
     */
    private void triggerCriticalAlert(AIAnalysisResult result) {
        log.error("╔═══════════════════════════════════════════════════════════╗");
        log.error("║          🚨 CRITICAL SCHEDULE ISSUES DETECTED 🚨         ║");
        log.error("╚═══════════════════════════════════════════════════════════╝");

        for (AIIssue issue : result.getCriticalIssues()) {
            log.error("🔴 {}: {}", issue.getType(), issue.getDescription());
            if (issue.getSuggestedAction() != null) {
                log.error("   → Suggested Action: {}", issue.getSuggestedAction());
            }
        }

        log.error("Health Score: {}/100", result.getHealthScore());
        log.error("Review Dashboard for details and recommendations");
        log.error("═══════════════════════════════════════════════════════════");

        // Show JavaFX Alert dialog to administrator (runs on FX Application Thread)
        showCriticalAlertDialog(result);
    }

    /**
     * Display a JavaFX Alert dialog with critical issues to the administrator.
     * Uses Platform.runLater to ensure UI operations run on the FX Application Thread.
     *
     * @param result the analysis result containing critical issues
     */
    private void showCriticalAlertDialog(AIAnalysisResult result) {
        try {
            Platform.runLater(() -> {
                try {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("🚨 Schedule Issues Detected");
                    alert.setHeaderText("Critical issues found in schedule analysis");

                    // Build summary content
                    StringBuilder content = new StringBuilder();
                    content.append("Health Score: ").append(result.getHealthScore()).append("/100\n\n");
                    content.append("Critical Issues Found: ").append(result.getCriticalIssues().size()).append("\n");

                    alert.setContentText(content.toString());

                    // Create expandable content with full details
                    StringBuilder details = new StringBuilder();
                    details.append("=== CRITICAL ISSUES ===\n\n");

                    for (AIIssue issue : result.getCriticalIssues()) {
                        details.append("🔴 ").append(issue.getType()).append("\n");
                        details.append("   ").append(issue.getDescription()).append("\n");
                        if (issue.getSuggestedAction() != null) {
                            details.append("   → Action: ").append(issue.getSuggestedAction()).append("\n");
                        }
                        details.append("\n");
                    }

                    if (!result.getWarnings().isEmpty()) {
                        details.append("\n=== WARNINGS (").append(result.getWarnings().size()).append(") ===\n\n");
                        for (AIIssue warning : result.getWarnings()) {
                            details.append("⚠️ ").append(warning.getType()).append(": ");
                            details.append(warning.getDescription()).append("\n");
                        }
                    }

                    TextArea textArea = new TextArea(details.toString());
                    textArea.setEditable(false);
                    textArea.setWrapText(true);
                    textArea.setMaxWidth(Double.MAX_VALUE);
                    textArea.setMaxHeight(Double.MAX_VALUE);

                    GridPane expContent = new GridPane();
                    expContent.setMaxWidth(Double.MAX_VALUE);
                    expContent.add(textArea, 0, 0);
                    GridPane.setVgrow(textArea, Priority.ALWAYS);
                    GridPane.setHgrow(textArea, Priority.ALWAYS);

                    alert.getDialogPane().setExpandableContent(expContent);
                    alert.getDialogPane().setExpanded(true);

                    // Show non-blocking alert
                    alert.show();

                    log.info("Critical alert dialog displayed to administrator");
                } catch (Exception e) {
                    log.warn("Could not display JavaFX alert (UI may not be available): {}", e.getMessage());
                }
            });
        } catch (IllegalStateException e) {
            // JavaFX toolkit not initialized (running in headless mode or non-GUI context)
            log.debug("JavaFX toolkit not available for alert display: {}", e.getMessage());
        }
    }

    /**
     * Get last analysis result
     */
    public AIAnalysisResult getLastAnalysis() {
        return lastAnalysis;
    }

    /**
     * Get last analysis time
     */
    public LocalDateTime getLastAnalysisTime() {
        return lastAnalysisTime;
    }

    /**
     * Get analysis history
     */
    public List<AIAnalysisResult> getAnalysisHistory() {
        return new ArrayList<>(analysisHistory);
    }

    /**
     * Get analysis history for time range
     */
    public List<AIAnalysisResult> getAnalysisHistory(LocalDateTime start, LocalDateTime end) {
        return analysisHistory.stream()
            .filter(r -> r.getTimestamp().isAfter(start) && r.getTimestamp().isBefore(end))
            .collect(Collectors.toList());
    }

    /**
     * Trigger immediate analysis (on-demand)
     */
    public AIAnalysisResult analyzeNow() {
        log.info("Manual analysis triggered");
        analyzeScheduleInBackground();
        return lastAnalysis;
    }

    /**
     * Get health trend (last N analyses)
     */
    public List<Integer> getHealthTrend(int count) {
        return analysisHistory.stream()
            .skip(Math.max(0, analysisHistory.size() - count))
            .map(AIAnalysisResult::getHealthScore)
            .collect(Collectors.toList());
    }

    /**
     * Check if there are unresolved critical issues
     */
    public boolean hasUnresolvedCriticalIssues() {
        return lastAnalysis != null && lastAnalysis.hasCriticalIssues();
    }

    /**
     * Get current health score
     */
    public int getCurrentHealthScore() {
        return lastAnalysis != null ? lastAnalysis.getHealthScore() : 0;
    }
}
