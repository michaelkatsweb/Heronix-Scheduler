package com.heronix.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Client Configuration DTO
 *
 * Data Transfer Object for client application configuration
 * Sent from backend to frontend applications (Student, Teacher)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 13, 2025
 */
@Data
public class ClientConfigurationDTO {

    // ========================================================================
    // SERVER CONFIGURATION
    // ========================================================================

    /**
     * Primary server URL
     */
    private String primaryServerUrl;

    /**
     * Fallback server URLs (for high availability)
     */
    private List<String> fallbackServers = new ArrayList<>();

    /**
     * API base path
     */
    private String apiBasePath;

    // ========================================================================
    // CONNECTION SETTINGS
    // ========================================================================

    /**
     * Connection timeout (milliseconds)
     */
    private Integer connectionTimeout;

    /**
     * Read timeout (milliseconds)
     */
    private Integer readTimeout;

    /**
     * Max retry attempts
     */
    private Integer maxRetries;

    /**
     * Retry delay (seconds)
     */
    private Integer retryDelay;

    // ========================================================================
    // NETWORK MODE
    // ========================================================================

    /**
     * Network mode (AUTO, ONLINE_ONLY, OFFLINE_ONLY, HYBRID)
     */
    private String networkMode;

    /**
     * Enable offline mode
     */
    private Boolean offlineModeEnabled;

    /**
     * Auto-sync when connection restored
     */
    private Boolean autoSyncEnabled;

    /**
     * Sync interval (seconds)
     */
    private Integer syncInterval;

    // ========================================================================
    // PROXY SETTINGS
    // ========================================================================

    /**
     * Enable proxy
     */
    private Boolean proxyEnabled;

    /**
     * Proxy host
     */
    private String proxyHost;

    /**
     * Proxy port
     */
    private Integer proxyPort;

    /**
     * Proxy username (if required)
     */
    private String proxyUsername;

    /**
     * Proxy password (if required)
     */
    private String proxyPassword;

    // ========================================================================
    // SECURITY SETTINGS
    // ========================================================================

    /**
     * Require HTTPS (SSL/TLS)
     */
    private Boolean requireHttps;

    /**
     * Trust self-signed certificates (development only)
     */
    private Boolean trustSelfSignedCertificates;

    /**
     * Custom CA certificate path
     */
    private String customCaCertPath;

    // ========================================================================
    // SCHOOL INFORMATION
    // ========================================================================

    /**
     * School/organization name
     */
    private String schoolName;

    /**
     * District name (for multi-school deployments)
     */
    private String districtName;

    /**
     * School code (unique identifier)
     */
    private String schoolCode;

    /**
     * IT department contact email
     */
    private String itContactEmail;

    /**
     * IT department contact phone
     */
    private String itContactPhone;

    /**
     * Deployment environment (PRODUCTION, TESTING, DEVELOPMENT)
     */
    private String environment;

    // ========================================================================
    // METADATA
    // ========================================================================

    /**
     * Configuration version
     */
    private String configVersion;

    /**
     * Server version
     */
    private String serverVersion;

    /**
     * Application type (student, teacher)
     */
    private String applicationType;

    /**
     * Timestamp when configuration was generated
     */
    private Long timestamp = System.currentTimeMillis();

    // ========================================================================
    // OPTIONAL FEATURES
    // ========================================================================

    /**
     * Enable QR code login
     */
    private Boolean qrCodeLoginEnabled = true;

    /**
     * Enable facial recognition
     */
    private Boolean facialRecognitionEnabled = true;

    /**
     * Require PIN for all students (can be overridden per-student)
     */
    private Boolean pinRequiredByDefault = false;

    /**
     * PIN lockout duration (minutes)
     */
    private Integer pinLockoutDuration = 30;

    /**
     * Max PIN attempts before lockout
     */
    private Integer maxPinAttempts = 3;

    // ========================================================================
    // UI SETTINGS
    // ========================================================================

    /**
     * Application theme (light, dark, auto)
     */
    private String theme = "auto";

    /**
     * Language/locale
     */
    private String locale = "en-US";

    /**
     * Show welcome screen on first launch
     */
    private Boolean showWelcomeScreen = true;

    /**
     * Auto-update enabled
     */
    private Boolean autoUpdateEnabled = true;
}
