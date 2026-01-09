package com.heronix.controller;

import com.heronix.model.domain.SubjectArea;
import com.heronix.repository.SubjectAreaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SubjectAreaManagementController
 * Tests subject area management business logic
 *
 * Note: This is a JavaFX controller. We test the business logic
 * (subject area management, hierarchy, filtering) rather than UI interactions.
 *
 * @author Heronix Scheduling System Team
 * @since December 20, 2025
 */
@SpringBootTest(classes = com.heronix.HeronixSchedulerApiApplication.class)
@ActiveProfiles("test")
@Transactional
public class SubjectAreaManagementControllerTest {

    @Autowired
    private SubjectAreaRepository subjectAreaRepository;

    private SubjectArea mathSubject;
    private SubjectArea algebraSubject;
    private SubjectArea scienceSubject;

    @BeforeEach
    public void setup() {
        // Create top-level subject area - Mathematics
        mathSubject = new SubjectArea();
        mathSubject.setCode("MATH");
        mathSubject.setName("Mathematics");
        mathSubject.setDepartment("Math Department");
        mathSubject.setDescription("Mathematics courses and curriculum");
        mathSubject.setDisplayColor("#3498DB");
        mathSubject.setActive(true);
        mathSubject = subjectAreaRepository.save(mathSubject);

        // Create child subject area - Algebra (under Mathematics)
        algebraSubject = new SubjectArea();
        algebraSubject.setCode("ALG");
        algebraSubject.setName("Algebra");
        algebraSubject.setDepartment("Math Department");
        algebraSubject.setDescription("Algebra courses");
        algebraSubject.setDisplayColor("#2ECC71");
        algebraSubject.setParentSubject(mathSubject);
        algebraSubject.setActive(true);
        algebraSubject = subjectAreaRepository.save(algebraSubject);

        // Create another top-level subject area - Science
        scienceSubject = new SubjectArea();
        scienceSubject.setCode("SCI");
        scienceSubject.setName("Science");
        scienceSubject.setDepartment("Science Department");
        scienceSubject.setDescription("Science courses and curriculum");
        scienceSubject.setDisplayColor("#E74C3C");
        scienceSubject.setActive(true);
        scienceSubject = subjectAreaRepository.save(scienceSubject);
    }

    // ========== SUBJECT AREA CRUD ==========

    @Test
    public void testCreateSubjectArea_ShouldSaveSuccessfully() {
        // Arrange
        SubjectArea newSubject = new SubjectArea();
        newSubject.setCode("ENG");
        newSubject.setName("English");
        newSubject.setDepartment("English Department");
        newSubject.setDescription("English Language Arts courses");
        newSubject.setDisplayColor("#9B59B6");
        newSubject.setActive(true);

        // Act
        SubjectArea saved = subjectAreaRepository.save(newSubject);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("ENG", saved.getCode());
        assertEquals("English", saved.getName());
        assertEquals("English Department", saved.getDepartment());
        assertTrue(saved.getActive());

        System.out.println("✓ Subject area created successfully");
        System.out.println("  - Code: " + saved.getCode());
        System.out.println("  - Name: " + saved.getName());
    }

    @Test
    public void testFindAllSubjectAreas_ShouldReturnList() {
        // Act
        List<SubjectArea> subjects = subjectAreaRepository.findAll();

        // Assert
        assertNotNull(subjects);
        assertTrue(subjects.size() >= 3); // At least MATH, ALG, SCI
        assertTrue(subjects.stream().anyMatch(s -> "MATH".equals(s.getCode())));
        assertTrue(subjects.stream().anyMatch(s -> "ALG".equals(s.getCode())));
        assertTrue(subjects.stream().anyMatch(s -> "SCI".equals(s.getCode())));

        System.out.println("✓ Found " + subjects.size() + " subject area(s)");
    }

    @Test
    public void testFindActiveSubjectAreas_ShouldReturnOnlyActive() {
        // Arrange - Create inactive subject
        SubjectArea inactiveSubject = new SubjectArea();
        inactiveSubject.setCode("OLD");
        inactiveSubject.setName("Old Subject");
        inactiveSubject.setActive(false);
        subjectAreaRepository.save(inactiveSubject);

        // Act
        List<SubjectArea> activeSubjects = subjectAreaRepository.findByActiveTrue();

        // Assert
        assertNotNull(activeSubjects);
        assertTrue(activeSubjects.stream().allMatch(SubjectArea::getActive));
        assertFalse(activeSubjects.stream().anyMatch(s -> "OLD".equals(s.getCode())));

        System.out.println("✓ Found " + activeSubjects.size() + " active subject area(s)");
    }

    @Test
    public void testFindSubjectAreaByCode_ShouldReturnSubject() {
        // Act
        Optional<SubjectArea> found = subjectAreaRepository.findByCodeIgnoreCase("MATH");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Mathematics", found.get().getName());
        assertEquals("MATH", found.get().getCode());

        System.out.println("✓ Subject area found by code");
        System.out.println("  - Code: " + found.get().getCode());
        System.out.println("  - Name: " + found.get().getName());
    }

    @Test
    public void testUpdateSubjectArea_ShouldPersistChanges() {
        // Arrange
        mathSubject.setName("Advanced Mathematics");
        mathSubject.setDescription("Updated description for mathematics");
        mathSubject.setDisplayColor("#1ABC9C");

        // Act
        SubjectArea updated = subjectAreaRepository.save(mathSubject);

        // Assert
        assertEquals("Advanced Mathematics", updated.getName());
        assertEquals("Updated description for mathematics", updated.getDescription());
        assertEquals("#1ABC9C", updated.getDisplayColor());

        System.out.println("✓ Subject area updated successfully");
    }

    @Test
    public void testDeleteSubjectArea_ShouldDeactivate() {
        // Arrange
        mathSubject.setActive(false);

        // Act
        SubjectArea deactivated = subjectAreaRepository.save(mathSubject);

        // Assert
        assertFalse(deactivated.getActive());

        // Verify it's excluded from active list
        List<SubjectArea> activeSubjects = subjectAreaRepository.findByActiveTrue();
        assertFalse(activeSubjects.stream().anyMatch(s -> s.getId().equals(mathSubject.getId())));

        System.out.println("✓ Subject area deactivated (soft delete)");
    }

    // ========== SUBJECT AREA HIERARCHY ==========

    @Test
    public void testSubjectAreaHierarchy_ShouldEstablishParentChild() {
        // Assert - Algebra should have Mathematics as parent
        assertNotNull(algebraSubject.getParentSubject());
        assertEquals(mathSubject.getId(), algebraSubject.getParentSubject().getId());
        assertEquals("MATH", algebraSubject.getParentSubject().getCode());

        System.out.println("✓ Subject hierarchy established");
        System.out.println("  - Parent: " + mathSubject.getName());
        System.out.println("  - Child: " + algebraSubject.getName());
    }

    @Test
    public void testTopLevelSubject_ShouldHaveNoParent() {
        // Assert - Mathematics is top-level
        assertTrue(mathSubject.isTopLevel());
        assertNull(mathSubject.getParentSubject());

        // Assert - Algebra is NOT top-level
        assertFalse(algebraSubject.isTopLevel());
        assertNotNull(algebraSubject.getParentSubject());

        System.out.println("✓ Top-level status correct");
        System.out.println("  - MATH is top-level: " + mathSubject.isTopLevel());
        System.out.println("  - ALG is top-level: " + algebraSubject.isTopLevel());
    }

    @Test
    public void testHierarchyLevel_ShouldCalculateCorrectly() {
        // Assert
        assertEquals(0, mathSubject.getHierarchyLevel()); // Top-level
        assertEquals(1, algebraSubject.getHierarchyLevel()); // One level down

        System.out.println("✓ Hierarchy levels calculated correctly");
        System.out.println("  - MATH level: " + mathSubject.getHierarchyLevel());
        System.out.println("  - ALG level: " + algebraSubject.getHierarchyLevel());
    }

    @Test
    public void testFullPath_ShouldShowHierarchy() {
        // Assert
        assertEquals("Mathematics", mathSubject.getFullPath());
        assertEquals("Mathematics > Algebra", algebraSubject.getFullPath());

        System.out.println("✓ Full paths generated correctly");
        System.out.println("  - MATH: " + mathSubject.getFullPath());
        System.out.println("  - ALG: " + algebraSubject.getFullPath());
    }

    // ========== SUBJECT AREA FILTERING ==========

    @Test
    public void testFindTopLevelSubjects_ShouldReturnOnlyTopLevel() {
        // Act
        List<SubjectArea> allSubjects = subjectAreaRepository.findAll();
        List<SubjectArea> topLevel = allSubjects.stream()
            .filter(SubjectArea::isTopLevel)
            .toList();

        // Assert
        assertNotNull(topLevel);
        assertTrue(topLevel.size() >= 2); // At least MATH and SCI
        assertTrue(topLevel.stream().allMatch(SubjectArea::isTopLevel));
        assertTrue(topLevel.stream().anyMatch(s -> "MATH".equals(s.getCode())));
        assertTrue(topLevel.stream().anyMatch(s -> "SCI".equals(s.getCode())));
        assertFalse(topLevel.stream().anyMatch(s -> "ALG".equals(s.getCode()))); // ALG is not top-level

        System.out.println("✓ Found " + topLevel.size() + " top-level subject(s)");
    }

    @Test
    public void testFindSubSubjects_ShouldReturnOnlyChildren() {
        // Act
        List<SubjectArea> allSubjects = subjectAreaRepository.findAll();
        List<SubjectArea> subSubjects = allSubjects.stream()
            .filter(s -> !s.isTopLevel())
            .toList();

        // Assert
        assertNotNull(subSubjects);
        assertTrue(subSubjects.size() >= 1); // At least ALG
        assertTrue(subSubjects.stream().noneMatch(SubjectArea::isTopLevel));
        assertTrue(subSubjects.stream().anyMatch(s -> "ALG".equals(s.getCode())));

        System.out.println("✓ Found " + subSubjects.size() + " sub-subject(s)");
    }

    @Test
    public void testSearchSubjects_ShouldFindByName() {
        // Act
        List<SubjectArea> allSubjects = subjectAreaRepository.findAll();
        List<SubjectArea> mathResults = allSubjects.stream()
            .filter(s -> s.getName().toLowerCase().contains("math"))
            .toList();

        // Assert
        assertNotNull(mathResults);
        assertTrue(mathResults.size() >= 1);
        assertTrue(mathResults.stream().anyMatch(s -> "Mathematics".equals(s.getName())));

        System.out.println("✓ Found " + mathResults.size() + " subject(s) matching 'math'");
    }

    @Test
    public void testSearchSubjects_ShouldFindByCode() {
        // Act
        List<SubjectArea> allSubjects = subjectAreaRepository.findAll();
        List<SubjectArea> sciResults = allSubjects.stream()
            .filter(s -> s.getCode().toLowerCase().contains("sci"))
            .toList();

        // Assert
        assertNotNull(sciResults);
        assertTrue(sciResults.size() >= 1);
        assertTrue(sciResults.stream().anyMatch(s -> "SCI".equals(s.getCode())));

        System.out.println("✓ Found " + sciResults.size() + " subject(s) matching 'SCI'");
    }

    // ========== SUBJECT AREA STATISTICS ==========

    @Test
    public void testCourseCount_ShouldReturnZeroForEmptySubject() {
        // Assert - newly created subjects have no courses
        assertEquals(0, mathSubject.getCourseCount());
        assertEquals(0, algebraSubject.getCourseCount());
        assertEquals(0, scienceSubject.getCourseCount());

        System.out.println("✓ Course counts correct for empty subjects");
    }

    @Test
    public void testDisplayString_ShouldFormatCorrectly() {
        // Assert
        assertEquals("Mathematics (MATH)", mathSubject.getDisplayString());
        assertEquals("Algebra (ALG)", algebraSubject.getDisplayString());
        assertEquals("Science (SCI)", scienceSubject.getDisplayString());

        System.out.println("✓ Display strings formatted correctly");
    }

    // ========== SUBJECT AREA VALIDATION ==========

    @Test
    public void testUniqueCode_ShouldPreventDuplicates() {
        // Arrange - Try to create duplicate code
        SubjectArea duplicate = new SubjectArea();
        duplicate.setCode("MATH"); // Same as mathSubject
        duplicate.setName("Duplicate Math");
        duplicate.setActive(true);

        // Act & Assert - Should throw exception due to unique constraint
        assertThrows(Exception.class, () -> {
            subjectAreaRepository.save(duplicate);
            subjectAreaRepository.flush(); // Force constraint check
        });

        System.out.println("✓ Unique code constraint enforced");
    }

    @Test
    public void testRequiredFields_ShouldBePresent() {
        // Assert - name and code are required
        assertNotNull(mathSubject.getName());
        assertNotNull(mathSubject.getCode());
        assertNotNull(algebraSubject.getName());
        assertNotNull(algebraSubject.getCode());

        System.out.println("✓ Required fields present");
    }
}
