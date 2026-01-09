package com.heronix.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home Controller - Handles root URL mapping
 * Location: src/main/java/com/eduscheduler/controller/HomeController.java
 * 
 * Redirects root URL (/) to H2 console or a welcome page
 * Prevents 404/403 errors when accessing http://localhost:8080
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-18
 */
@Controller
public class HomeController {

    /**
     * Root URL handler - Redirects to H2 Console for now
     * 
     * Later you can create a proper welcome/dashboard page here
     */
    @GetMapping("/")
    public String home() {
        // Redirect to H2 Console (for development)
        return "redirect:/h2-console";

        // OR later, return a proper view:
        // return "index"; // Would look for src/main/resources/templates/index.html
    }

    /**
     * Alternative welcome page (if you create one)
     */
    @GetMapping("/welcome")
    public String welcome() {
        return "welcome"; // Would need src/main/resources/templates/welcome.html
    }
}