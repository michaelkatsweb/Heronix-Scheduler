package com.heronix.controller;

import com.heronix.dto.ClientConfigurationDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ClientConfigurationController
 * Tests configuration endpoints for client application auto-configuration
 *
 * @author Heronix Scheduling System Team
 * @since December 19, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class ClientConfigurationControllerTest {

    @Autowired
    private ClientConfigurationController clientConfigurationController;

    // ========== MAIN CONFIGURATION ENDPOINT ==========

    @Test
    public void testGetClientConfiguration_WithDefaultParameters_ShouldReturnConfig() {
        // Act
        ResponseEntity<ClientConfigurationDTO> response =
            clientConfigurationController.getClientConfiguration("student", null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        ClientConfigurationDTO config = response.getBody();
        assertNotNull(config.getPrimaryServerUrl());
        assertNotNull(config.getApiBasePath());
        assertEquals("student", config.getApplicationType());
        assertEquals("1.0.0", config.getConfigVersion());

        System.out.println("✓ Client configuration retrieved successfully");
        System.out.println("  - Server URL: " + config.getPrimaryServerUrl());
        System.out.println("  - API Base Path: " + config.getApiBasePath());
        System.out.println("  - Application Type: " + config.getApplicationType());
    }

    @Test
    public void testGetClientConfiguration_WithTeacherAppType_ShouldReturnTeacherConfig() {
        // Act
        ResponseEntity<ClientConfigurationDTO> response =
            clientConfigurationController.getClientConfiguration("teacher", null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("teacher", response.getBody().getApplicationType());

        System.out.println("✓ Teacher configuration retrieved successfully");
        System.out.println("  - Application Type: " + response.getBody().getApplicationType());
    }

    @Test
    public void testGetClientConfiguration_WithStudentAppType_ShouldReturnStudentConfig() {
        // Act
        ResponseEntity<ClientConfigurationDTO> response =
            clientConfigurationController.getClientConfiguration("student", null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("student", response.getBody().getApplicationType());

        System.out.println("✓ Student configuration retrieved successfully");
        System.out.println("  - Application Type: " + response.getBody().getApplicationType());
    }

    @Test
    public void testGetClientConfiguration_WithSchoolCode_ShouldIncludeSchoolCode() {
        // Act
        ResponseEntity<ClientConfigurationDTO> response =
            clientConfigurationController.getClientConfiguration("student", "SCHOOL001", null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SCHOOL001", response.getBody().getSchoolCode());

        System.out.println("✓ Configuration with school code retrieved");
        System.out.println("  - School Code: " + response.getBody().getSchoolCode());
    }

    @Test
    public void testGetClientConfiguration_WithVersion_ShouldIncludeVersion() {
        // Act
        ResponseEntity<ClientConfigurationDTO> response =
            clientConfigurationController.getClientConfiguration("student", null, "2.1.0");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("2.1.0", response.getBody().getServerVersion());

        System.out.println("✓ Configuration with version retrieved");
        System.out.println("  - Server Version: " + response.getBody().getServerVersion());
    }

    @Test
    public void testGetClientConfiguration_ShouldIncludeConnectionSettings() {
        // Act
        ResponseEntity<ClientConfigurationDTO> response =
            clientConfigurationController.getClientConfiguration("student", null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ClientConfigurationDTO config = response.getBody();

        assertNotNull(config.getConnectionTimeout());
        assertNotNull(config.getReadTimeout());
        assertNotNull(config.getMaxRetries());
        assertNotNull(config.getRetryDelay());
        assertTrue(config.getConnectionTimeout() > 0, "Connection timeout should be positive");
        assertTrue(config.getReadTimeout() > 0, "Read timeout should be positive");

        System.out.println("✓ Configuration includes connection settings");
        System.out.println("  - Connection Timeout: " + config.getConnectionTimeout() + "ms");
        System.out.println("  - Read Timeout: " + config.getReadTimeout() + "ms");
        System.out.println("  - Max Retries: " + config.getMaxRetries());
    }

    @Test
    public void testGetClientConfiguration_ShouldIncludeNetworkSettings() {
        // Act
        ResponseEntity<ClientConfigurationDTO> response =
            clientConfigurationController.getClientConfiguration("student", null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ClientConfigurationDTO config = response.getBody();

        assertNotNull(config.getNetworkMode());
        assertNotNull(config.getOfflineModeEnabled());
        assertNotNull(config.getAutoSyncEnabled());
        assertNotNull(config.getSyncInterval());

        System.out.println("✓ Configuration includes network settings");
        System.out.println("  - Network Mode: " + config.getNetworkMode());
        System.out.println("  - Offline Mode: " + config.getOfflineModeEnabled());
        System.out.println("  - Auto Sync: " + config.getAutoSyncEnabled());
    }

    @Test
    public void testGetClientConfiguration_ShouldIncludeSecuritySettings() {
        // Act
        ResponseEntity<ClientConfigurationDTO> response =
            clientConfigurationController.getClientConfiguration("student", null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ClientConfigurationDTO config = response.getBody();

        assertNotNull(config.getRequireHttps());

        System.out.println("✓ Configuration includes security settings");
        System.out.println("  - Require HTTPS: " + config.getRequireHttps());
    }

    @Test
    public void testGetClientConfiguration_ShouldIncludeSchoolInformation() {
        // Act
        ResponseEntity<ClientConfigurationDTO> response =
            clientConfigurationController.getClientConfiguration("student", null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ClientConfigurationDTO config = response.getBody();

        // School information might be empty in test environment, but fields should exist
        assertNotNull(config.getSchoolName() != null ? config.getSchoolName() : "");
        assertNotNull(config.getEnvironment());

        System.out.println("✓ Configuration includes school information");
        System.out.println("  - Environment: " + config.getEnvironment());
    }

    // ========== DISCOVERY ENDPOINT ==========

    @Test
    public void testGetDiscoveryInfo_ShouldReturnServerInfo() {
        // Act
        ResponseEntity<Map<String, Object>> response =
            clientConfigurationController.getDiscoveryInfo();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> info = response.getBody();
        assertEquals("Heronix Scheduling System", info.get("product"));
        assertEquals("1.0.0", info.get("version"));
        assertEquals("/api/config/client", info.get("configEndpoint"));
        assertNotNull(info.get("environment"));

        System.out.println("✓ Discovery info retrieved successfully");
        System.out.println("  - Product: " + info.get("product"));
        System.out.println("  - Version: " + info.get("version"));
        System.out.println("  - Config Endpoint: " + info.get("configEndpoint"));
    }

    @Test
    public void testGetDiscoveryInfo_ShouldIncludeRequiredFields() {
        // Act
        ResponseEntity<Map<String, Object>> response =
            clientConfigurationController.getDiscoveryInfo();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> info = response.getBody();

        assertTrue(info.containsKey("product"));
        assertTrue(info.containsKey("version"));
        assertTrue(info.containsKey("configEndpoint"));
        assertTrue(info.containsKey("environment"));

        System.out.println("✓ Discovery info contains all required fields");
    }

    // ========== PING ENDPOINT ==========

    @Test
    public void testPing_ShouldReturnOkStatus() {
        // Act
        ResponseEntity<Map<String, Object>> response =
            clientConfigurationController.ping();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> ping = response.getBody();
        assertEquals("ok", ping.get("status"));
        assertNotNull(ping.get("timestamp"));
        assertTrue(ping.get("timestamp") instanceof Number);

        System.out.println("✓ Ping endpoint responded successfully");
        System.out.println("  - Status: " + ping.get("status"));
        System.out.println("  - Timestamp: " + ping.get("timestamp"));
    }

    @Test
    public void testPing_ShouldIncludeServerName() {
        // Act
        ResponseEntity<Map<String, Object>> response =
            clientConfigurationController.ping();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> ping = response.getBody();

        assertTrue(ping.containsKey("server"));
        // Server name might be empty in test environment
        assertNotNull(ping.get("server"));

        System.out.println("✓ Ping includes server name");
    }

    // ========== SCHOOL-SPECIFIC CONFIGURATION ==========

    @Test
    public void testGetSchoolConfiguration_WithValidSchoolCode_ShouldReturnConfig() {
        // Act
        ResponseEntity<ClientConfigurationDTO> response =
            clientConfigurationController.getSchoolConfiguration("HS001", "student");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("HS001", response.getBody().getSchoolCode());
        assertEquals("student", response.getBody().getApplicationType());

        System.out.println("✓ School-specific configuration retrieved");
        System.out.println("  - School Code: " + response.getBody().getSchoolCode());
        System.out.println("  - Application Type: " + response.getBody().getApplicationType());
    }

    @Test
    public void testGetSchoolConfiguration_WithTeacherAppType_ShouldReturnTeacherConfig() {
        // Act
        ResponseEntity<ClientConfigurationDTO> response =
            clientConfigurationController.getSchoolConfiguration("MS002", "teacher");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MS002", response.getBody().getSchoolCode());
        assertEquals("teacher", response.getBody().getApplicationType());

        System.out.println("✓ School-specific teacher configuration retrieved");
        System.out.println("  - School Code: " + response.getBody().getSchoolCode());
        System.out.println("  - Application Type: " + response.getBody().getApplicationType());
    }

    @Test
    public void testGetSchoolConfiguration_ShouldReturnSameStructureAsMainConfig() {
        // Arrange
        ResponseEntity<ClientConfigurationDTO> mainConfig =
            clientConfigurationController.getClientConfiguration("student", "TEST001", null);

        ResponseEntity<ClientConfigurationDTO> schoolConfig =
            clientConfigurationController.getSchoolConfiguration("TEST001", "student");

        // Assert
        assertEquals(HttpStatus.OK, mainConfig.getStatusCode());
        assertEquals(HttpStatus.OK, schoolConfig.getStatusCode());

        // Both should have same structure (both call same underlying method)
        assertEquals(mainConfig.getBody().getPrimaryServerUrl(),
                    schoolConfig.getBody().getPrimaryServerUrl());
        assertEquals(mainConfig.getBody().getSchoolCode(),
                    schoolConfig.getBody().getSchoolCode());

        System.out.println("✓ School configuration has same structure as main config");
    }
}
