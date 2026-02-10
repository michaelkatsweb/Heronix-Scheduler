package com.heronix.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * OpenAPI/Swagger Configuration
 *
 * Provides REST API documentation at:
 * - Swagger UI: http://localhost:9590/swagger-ui.html
 * - OpenAPI JSON: http://localhost:9590/v3/api-docs
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 15 - API Documentation
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI heronixSchedulerOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(servers())
            .tags(apiTags());
    }

    private Info apiInfo() {
        return new Info()
            .title("Heronix Scheduling System API")
            .description("""
                ## AI-Powered School Scheduling System REST API

                This API provides access to Heronix Scheduling System's scheduling, analytics,
                and student information management features.

                ### Features
                - **Schedule Management**: Create, update, and optimize class schedules
                - **Analytics**: Burnout risk, SPED compliance, predictive forecasting
                - **Federation**: Multi-campus support and resource sharing
                - **Student Management**: Attendance, transcripts, enrollment
                - **IEP/504 Support**: Special education scheduling compliance

                ### Authentication
                All endpoints require authentication via Spring Security.
                Use Basic Auth or JWT tokens as configured.

                ### Rate Limiting
                API requests are limited to 1000 requests per hour per client.
                """)
            .version("1.0.0")
            .contact(new Contact()
                .name("Heronix Educational Systems LLC")
                .email("support@heronixedu.com")
                .url("https://heronixedu.com"))
            .license(new License()
                .name("Proprietary")
                .url("https://heronixedu.com/license"));
    }

    private List<Server> servers() {
        return Arrays.asList(
            new Server()
                .url("http://localhost:" + serverPort)
                .description("Development Server"),
            new Server()
                .url("https://api.heronixedu.com")
                .description("Production Server")
        );
    }

    private List<Tag> apiTags() {
        return Arrays.asList(
            new Tag()
                .name("Schedule")
                .description("Schedule generation and management"),
            new Tag()
                .name("Analytics")
                .description("Advanced analytics and reporting"),
            new Tag()
                .name("Federation")
                .description("Multi-campus federation management"),
            new Tag()
                .name("Attendance")
                .description("Student attendance tracking"),
            new Tag()
                .name("Transcript")
                .description("Academic transcripts and GPA"),
            new Tag()
                .name("Portal")
                .description("Parent/Student portal access"),
            new Tag()
                .name("Mobile")
                .description("Mobile application API endpoints"),
            new Tag()
                .name("LMS")
                .description("Learning Management System integration")
        );
    }
}
