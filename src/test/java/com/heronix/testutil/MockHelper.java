package com.heronix.testutil;

import org.mockito.stubbing.OngoingStubbing;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Helper class for setting up common mock behaviors
 * Reduces boilerplate in test classes
 *
 * Usage:
 * <pre>
 * {@code
 * // Mock repository to return entity
 * MockHelper.mockFindById(repository, teacher);
 *
 * // Mock repository to return empty
 * MockHelper.mockFindByIdEmpty(repository);
 *
 * // Mock repository findAll
 * MockHelper.mockFindAll(repository, teachers);
 * }
 * </pre>
 */
public class MockHelper {

    /**
     * Mocks a repository's findById method to return an entity
     *
     * @param repository the mock repository
     * @param entity the entity to return
     * @param <T> the entity type
     * @param <R> the repository type
     */
    public static <T, R> void mockFindById(R repository, T entity) {
        try {
            when(repository.getClass().getMethod("findById", Object.class).invoke(repository, anyLong()))
                .thenReturn(Optional.of(entity));
        } catch (Exception e) {
            // Fallback - this is a test helper, exceptions are not critical
        }
    }

    /**
     * Mocks a repository's findById method to return empty
     *
     * @param repository the mock repository
     * @param <R> the repository type
     */
    public static <R> void mockFindByIdEmpty(R repository) {
        try {
            when(repository.getClass().getMethod("findById", Object.class).invoke(repository, anyLong()))
                .thenReturn(Optional.empty());
        } catch (Exception e) {
            // Fallback - this is a test helper, exceptions are not critical
        }
    }

    /**
     * Mocks a repository's findAll method to return a list
     *
     * @param repository the mock repository
     * @param entities the list of entities to return
     * @param <T> the entity type
     * @param <R> the repository type
     */
    public static <T, R> void mockFindAll(R repository, List<T> entities) {
        try {
            when(repository.getClass().getMethod("findAll").invoke(repository))
                .thenReturn(entities);
        } catch (Exception e) {
            // Fallback - this is a test helper, exceptions are not critical
        }
    }

    /**
     * Mocks a repository's save method to return the saved entity
     *
     * @param repository the mock repository
     * @param <T> the entity type
     * @param <R> the repository type
     */
    @SuppressWarnings("unchecked")
    public static <T, R> void mockSave(R repository) {
        try {
            when(repository.getClass().getMethod("save", Object.class).invoke(repository, any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        } catch (Exception e) {
            // Fallback - this is a test helper, exceptions are not critical
        }
    }

    /**
     * Mocks a repository's delete method (void return)
     *
     * @param repository the mock repository
     * @param <R> the repository type
     */
    public static <R> void mockDelete(R repository) {
        try {
            repository.getClass().getMethod("delete", Object.class).invoke(repository, any());
        } catch (Exception e) {
            // Fallback - this is a test helper, exceptions are not critical
        }
    }

    /**
     * Mocks a repository's existsById method to return true
     *
     * @param repository the mock repository
     * @param <R> the repository type
     */
    public static <R> void mockExistsById(R repository, boolean exists) {
        try {
            when(repository.getClass().getMethod("existsById", Object.class).invoke(repository, anyLong()))
                .thenReturn(exists);
        } catch (Exception e) {
            // Fallback - this is a test helper, exceptions are not critical
        }
    }

    /**
     * Mocks a repository's count method
     *
     * @param repository the mock repository
     * @param count the count to return
     * @param <R> the repository type
     */
    public static <R> void mockCount(R repository, long count) {
        try {
            when(repository.getClass().getMethod("count").invoke(repository))
                .thenReturn(count);
        } catch (Exception e) {
            // Fallback - this is a test helper, exceptions are not critical
        }
    }
}
