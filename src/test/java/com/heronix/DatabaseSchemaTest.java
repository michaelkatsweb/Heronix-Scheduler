package com.heronix;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Test class to initialize database schema.
 * Run this test to create all tables via Hibernate DDL.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.jpa.show-sql=true",
    "logging.level.org.hibernate.SQL=DEBUG"
})
public class DatabaseSchemaTest {

    @Test
    public void contextLoads() {
        // This test will initialize the Spring context and create/update the database schema
        System.out.println("Database schema initialized successfully!");
    }
}
