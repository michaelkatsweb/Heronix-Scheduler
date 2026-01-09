package com.heronix.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for HomeController
 * Tests MVC controller mappings and redirects
 *
 * @author Heronix Scheduling System Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HomeController homeController;

    // ========== ROOT URL ==========

    @Test
    public void testHome_ShouldRedirectToH2Console() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/h2-console"));

        System.out.println("✓ Root URL redirects to H2 console");
    }

    // ========== WELCOME PAGE ==========

    @Test
    public void testWelcome_ShouldReturnWelcomeViewName() throws Exception {
        // Note: This test verifies the view name is returned correctly.
        // Since there's no actual welcome.html template, we just verify
        // the controller returns the correct view name.

        // Act
        String viewName = homeController.welcome();

        // Assert
        assertEquals("welcome", viewName);

        System.out.println("✓ Welcome endpoint returns 'welcome' view name");
    }
}
