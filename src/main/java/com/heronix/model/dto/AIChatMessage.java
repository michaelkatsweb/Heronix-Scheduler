package com.heronix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for AI chat messages
 *
 * @since Phase 4 - AI Integration
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIChatMessage {

    public enum Role {
        USER,
        ASSISTANT,
        SYSTEM
    }

    /**
     * Message ID
     */
    private String id;

    /**
     * Message role (user, assistant, system)
     */
    private Role role;

    /**
     * Message content
     */
    private String content;

    /**
     * Timestamp
     */
    private LocalDateTime timestamp;

    /**
     * Session ID
     */
    private String sessionId;

    /**
     * Token count (if available)
     */
    private Integer tokenCount;

    /**
     * Model used for response (for assistant messages)
     */
    private String model;

    /**
     * Check if message is from user
     */
    public boolean isUserMessage() {
        return role == Role.USER;
    }

    /**
     * Check if message is from assistant
     */
    public boolean isAssistantMessage() {
        return role == Role.ASSISTANT;
    }

    /**
     * Get display name for role
     */
    public String getRoleDisplayName() {
        return switch (role) {
            case USER -> "You";
            case ASSISTANT -> "AI Assistant";
            case SYSTEM -> "System";
        };
    }

    /**
     * Get role icon
     */
    public String getRoleIcon() {
        return switch (role) {
            case USER -> "👤";
            case ASSISTANT -> "🤖";
            case SYSTEM -> "⚙️";
        };
    }
}
