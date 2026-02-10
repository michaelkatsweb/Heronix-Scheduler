package com.heronix;

import com.heronix.config.ApplicationProperties;
import com.heronix.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

/**
 * Heronix Scheduling System - REST API Server Entry Point
 *
 * This class starts the application as a REST API server (web server)
 * WITHOUT JavaFX desktop UI components.
 *
 * Use this for:
 * - EduPro-Student client connections
 * - EduPro-Teacher client connections
 * - REST API access
 * - Production deployment
 *
 * DO NOT use this for:
 * - Desktop administrator UI (use Heronix SchedulerApplication instead)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-13
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableCaching
public class HeronixSchedulerApiApplication {

    public static void main(String[] args) {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║   Heronix Scheduling System - REST API Server                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");
        log.info("");
        log.info("Starting in REST API mode (no JavaFX UI)");
        log.info("This exposes REST endpoints for client applications");
        log.info("");

        SpringApplication app = new SpringApplication(HeronixSchedulerApiApplication.class);

        // CRITICAL: Set web application type to SERVLET (not NONE)
        // This enables the embedded Tomcat server and REST endpoints
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.SERVLET);

        app.run(args);

        log.info("");
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║   Heronix Scheduling System REST API Server - READY                   ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");
        log.info("");
        log.info("REST API Endpoints:");
        log.info("  • Health:      http://localhost:9590/actuator/health");
        log.info("  • Discovery:   http://localhost:9590/api/config/discovery");
        log.info("  • Config:      http://localhost:9590/api/config/client");
        log.info("");
        log.info("H2 Console:");
        log.info("  • URL:         http://localhost:9590/h2-console");
        log.info("  • JDBC URL:    jdbc:h2:file:./data/eduscheduler");
        log.info("  • Username:    sa");
        log.info("  • Password:    (blank)");
        log.info("");
        log.info("Press Ctrl+C to stop the server");
        log.info("");
    }

    /**
     * Initialize default users on startup
     */
    @Bean
    public CommandLineRunner initializeDefaultUsers(UserService userService) {
        return args -> {
            log.info("Initializing default users...");
            userService.initializeDefaultUsers();
            log.info("Default users initialized successfully!");
        };
    }
}
