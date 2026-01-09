package com.heronix.controller;

import com.heronix.dto.ClientConfigurationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Client Configuration Controller
 *
 * Provides centralized configuration for client applications (EduPro-Student, EduPro-Teacher)
 * This eliminates the need to manually configure thousands of computers.
 *
 * IT administrators configure settings once here, and all clients fetch on startup.
 *
 * Endpoints:
 * - GET /api/config/client - Get configuration for client application
 * - GET /api/config/discovery - Discovery endpoint for auto-configuration
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 13, 2025 - Enterprise Deployment System
 */
@RestController
@RequestMapping("/api/config")
@Slf4j
@CrossOrigin(origins = "*")
public class ClientConfigurationController {

    // Configuration values from application.properties
    // IT can edit these in the backend's application.properties file

    @Value("${heronix.client.server-url:http://localhost:8080}")
    private String primaryServerUrl;

    @Value("${heronix.client.fallback-servers:}")
    private String fallbackServersString;

    @Value("${heronix.client.api-base-path:/api}")
    private String apiBasePath;

    @Value("${heronix.client.connection-timeout:10000}")
    private Integer connectionTimeout;

    @Value("${heronix.client.read-timeout:30000}")
    private Integer readTimeout;

    @Value("${heronix.client.max-retries:3}")
    private Integer maxRetries;

    @Value("${heronix.client.retry-delay:2}")
    private Integer retryDelay;

    @Value("${heronix.client.network-mode:AUTO}")
    private String networkMode;

    @Value("${heronix.client.offline-mode-enabled:true}")
    private Boolean offlineModeEnabled;

    @Value("${heronix.client.auto-sync-enabled:true}")
    private Boolean autoSyncEnabled;

    @Value("${heronix.client.sync-interval:60}")
    private Integer syncInterval;

    @Value("${heronix.client.proxy-enabled:false}")
    private Boolean proxyEnabled;

    @Value("${heronix.client.proxy-host:}")
    private String proxyHost;

    @Value("${heronix.client.proxy-port:0}")
    private Integer proxyPort;

    @Value("${heronix.client.require-https:false}")
    private Boolean requireHttps;

    @Value("${heronix.school.name:}")
    private String schoolName;

    @Value("${heronix.school.district:}")
    private String districtName;

    @Value("${heronix.school.code:}")
    private String schoolCode;

    @Value("${heronix.it.contact-email:}")
    private String itContactEmail;

    @Value("${heronix.it.contact-phone:}")
    private String itContactPhone;

    @Value("${heronix.client.environment:PRODUCTION}")
    private String environment;

    /**
     * Main configuration endpoint
     *
     * Returns complete configuration for client applications
     *
     * Usage:
     * GET /api/config/client?appType=student
     * GET /api/config/client?appType=teacher
     */
    @GetMapping("/client")
    public ResponseEntity<ClientConfigurationDTO> getClientConfiguration(
            @RequestParam(required = false, defaultValue = "student") String appType,
            @RequestParam(required = false) String schoolCode,
            @RequestParam(required = false) String version
    ) {
        log.info("Client configuration requested - appType: {}, schoolCode: {}, version: {}",
                appType, schoolCode, version);

        ClientConfigurationDTO config = new ClientConfigurationDTO();

        // ================================================================
        // SERVER CONFIGURATION
        // ================================================================
        config.setPrimaryServerUrl(primaryServerUrl);

        // Parse fallback servers (comma-separated)
        if (fallbackServersString != null && !fallbackServersString.trim().isEmpty()) {
            config.setFallbackServers(
                Arrays.asList(fallbackServersString.split(","))
            );
        }

        config.setApiBasePath(apiBasePath);

        // ================================================================
        // CONNECTION SETTINGS
        // ================================================================
        config.setConnectionTimeout(connectionTimeout);
        config.setReadTimeout(readTimeout);
        config.setMaxRetries(maxRetries);
        config.setRetryDelay(retryDelay);

        // ================================================================
        // NETWORK MODE
        // ================================================================
        config.setNetworkMode(networkMode);
        config.setOfflineModeEnabled(offlineModeEnabled);
        config.setAutoSyncEnabled(autoSyncEnabled);
        config.setSyncInterval(syncInterval);

        // ================================================================
        // PROXY SETTINGS
        // ================================================================
        config.setProxyEnabled(proxyEnabled);
        if (proxyEnabled) {
            config.setProxyHost(proxyHost);
            config.setProxyPort(proxyPort);
        }

        // ================================================================
        // SECURITY SETTINGS
        // ================================================================
        config.setRequireHttps(requireHttps);

        // ================================================================
        // SCHOOL INFORMATION
        // ================================================================
        config.setSchoolName(schoolName);
        config.setDistrictName(districtName);
        config.setSchoolCode(schoolCode != null ? schoolCode : this.schoolCode);
        config.setItContactEmail(itContactEmail);
        config.setItContactPhone(itContactPhone);
        config.setEnvironment(environment);

        // ================================================================
        // METADATA
        // ================================================================
        config.setConfigVersion("1.0.0");
        config.setServerVersion(version != null ? version : "1.0.0");
        config.setApplicationType(appType);

        log.info("✓ Configuration sent to client - School: {}, Environment: {}",
                schoolName, environment);

        return ResponseEntity.ok(config);
    }

    /**
     * Discovery endpoint
     *
     * Allows clients to find this server automatically
     * Returns minimal info to verify this is an Heronix Scheduler server
     *
     * Usage:
     * GET /api/config/discovery
     */
    @GetMapping("/discovery")
    public ResponseEntity<Map<String, Object>> getDiscoveryInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("product", "Heronix Scheduling System");
        info.put("version", "1.0.0");
        info.put("configEndpoint", "/api/config/client");
        info.put("schoolName", schoolName);
        info.put("schoolCode", schoolCode);
        info.put("environment", environment);

        log.debug("Discovery info requested");

        return ResponseEntity.ok(info);
    }

    /**
     * Ping endpoint for connectivity testing
     *
     * Usage:
     * GET /api/config/ping
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("timestamp", System.currentTimeMillis());
        response.put("server", schoolName);
        return ResponseEntity.ok(response);
    }

    /**
     * Get available configuration for specific school
     * (For multi-school/district deployments)
     *
     * Usage:
     * GET /api/config/schools/{schoolCode}
     */
    @GetMapping("/schools/{code}")
    public ResponseEntity<ClientConfigurationDTO> getSchoolConfiguration(
            @PathVariable String code,
            @RequestParam(required = false, defaultValue = "student") String appType
    ) {
        log.info("School-specific configuration requested - code: {}, appType: {}", code, appType);

        // For now, return default config
        // In the future, this could query a database table with school-specific settings
        return getClientConfiguration(appType, code, null);
    }
}
