package com.heronix.testutil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for service layer unit tests
 * Provides common setup and utilities for testing services with mocked dependencies
 *
 * Usage:
 * <pre>
 * {@code
 * @ExtendWith(MockitoExtension.class)
 * class MyServiceTest extends BaseServiceTest {
 *
 *     @Mock
 *     private MyRepository repository;
 *
 *     @InjectMocks
 *     private MyServiceImpl service;
 *
 *     @Test
 *     void testMethod() {
 *         // Test logic here
 *     }
 * }
 * }
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseServiceTest {

    /**
     * Setup method run before each test
     * Override this to add custom setup logic
     */
    @BeforeEach
    void setUp() {
        // Common setup can be added here
    }

    /**
     * Helper method to verify no exceptions are thrown
     * Useful for null safety testing
     *
     * @param runnable the code to execute
     */
    protected void assertDoesNotThrowNPE(Runnable runnable) {
        try {
            runnable.run();
        } catch (NullPointerException e) {
            throw new AssertionError("Expected no NullPointerException but got: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to verify a specific exception is thrown
     *
     * @param expectedType the expected exception class
     * @param runnable the code to execute
     * @param <T> the exception type
     * @return the caught exception for further assertions
     */
    protected <T extends Throwable> T assertThrows(Class<T> expectedType, Runnable runnable) {
        try {
            runnable.run();
            throw new AssertionError("Expected " + expectedType.getSimpleName() + " but nothing was thrown");
        } catch (Throwable actual) {
            if (expectedType.isInstance(actual)) {
                return expectedType.cast(actual);
            }
            throw new AssertionError("Expected " + expectedType.getSimpleName() +
                                   " but got " + actual.getClass().getSimpleName(), actual);
        }
    }
}
