package com.heronix.repository;

import com.heronix.model.domain.*;
import com.heronix.testutil.Phase1TestDataGenerator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Repository Tests for Phase 1 Multi-Level Monitoring
 *
 * Tests all three Phase 1 repositories:
 * - ClassroomGradeEntryRepository
 * - BehaviorIncidentRepository
 * - TeacherObservationNoteRepository
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 Repository Testing
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase1RepositoryTests {

    @Autowired
    private Phase1TestDataGenerator testDataGenerator;

    @Autowired
    private ClassroomGradeEntryRepository gradeEntryRepository;

    @Autowired
    private BehaviorIncidentRepository behaviorIncidentRepository;

    @Autowired
    private TeacherObservationNoteRepository observationNoteRepository;

    private static Phase1TestDataGenerator.TestDataSet testData;

    @BeforeAll
    public static void setUpClass() {
        System.out.println("\n========================================================================");
        System.out.println("PHASE 1 REPOSITORY TESTING - Starting Test Suite");
        System.out.println("========================================================================\n");
    }

    @BeforeEach
    @Transactional
    public void setUp() {
        // Clean up before each test
        testDataGenerator.cleanupTestData();
    }

    @AfterAll
    public static void tearDownClass() {
        System.out.println("\n========================================================================");
        System.out.println("PHASE 1 REPOSITORY TESTING - Test Suite Complete");
        System.out.println("========================================================================\n");
    }

    // ========================================================================
    // CLASSROOM GRADE ENTRY REPOSITORY TESTS
    // ========================================================================

    @Test
    @Order(1)
    @Transactional
    public void testGradeEntryRepository_DataGeneration() {
        System.out.println("\n[TEST 1] Testing Grade Entry Data Generation");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        assertNotNull(testData);
        assertNotNull(testData.gradeEntries);
        assertTrue(testData.gradeEntries.size() > 0);

        System.out.println("✓ Generated " + testData.gradeEntries.size() + " grade entries");
        System.out.println("✓ Test student: " + testData.student.getFullName());
        System.out.println("✓ Test course: " + testData.course.getCourseName());
    }

    @Test
    @Order(2)
    @Transactional
    public void testGradeEntryRepository_FindByStudent() {
        System.out.println("\n[TEST 2] Testing findByStudent Query");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        List<ClassroomGradeEntry> entries = gradeEntryRepository.findByStudent(testData.student);

        assertNotNull(entries);
        assertTrue(entries.size() > 0);
        assertTrue(entries.stream().allMatch(e -> e.getStudent().equals(testData.student)));

        System.out.println("✓ Found " + entries.size() + " grade entries for student");
    }

    @Test
    @Order(3)
    @Transactional
    public void testGradeEntryRepository_FindByStudentAndCourse() {
        System.out.println("\n[TEST 3] Testing findByStudentAndCourse Query");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        List<ClassroomGradeEntry> entries =
            gradeEntryRepository.findByStudentAndCourse(testData.student, testData.course);

        assertNotNull(entries);
        assertTrue(entries.size() > 0);
        assertTrue(entries.stream().allMatch(e ->
            e.getStudent().equals(testData.student) && e.getCourse().equals(testData.course)));

        System.out.println("✓ Found " + entries.size() + " grade entries for student in course");
    }

    @Test
    @Order(4)
    @Transactional
    public void testGradeEntryRepository_FindByDateRange() {
        System.out.println("\n[TEST 4] Testing findByStudentAndAssignmentDateBetween Query");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        List<ClassroomGradeEntry> entries =
            gradeEntryRepository.findByStudentAndAssignmentDateBetween(
                testData.student, startDate, endDate);

        assertNotNull(entries);
        assertTrue(entries.stream().allMatch(e ->
            !e.getAssignmentDate().isBefore(startDate) &&
            !e.getAssignmentDate().isAfter(endDate)));

        System.out.println("✓ Found " + entries.size() + " entries in past 30 days");
    }

    @Test
    @Order(5)
    @Transactional
    public void testGradeEntryRepository_CountMissingWork() {
        System.out.println("\n[TEST 5] Testing countByStudentAndIsMissingWorkAndDateBetween Query");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(14);

        Long count = gradeEntryRepository.countByStudentAndIsMissingWorkAndDateBetween(
            testData.student, startDate, endDate);

        assertNotNull(count);
        assertTrue(count >= 0);

        System.out.println("✓ Found " + count + " missing assignments in past 14 days");
    }

    @Test
    @Order(6)
    @Transactional
    public void testGradeEntryRepository_FindFailingGrades() {
        System.out.println("\n[TEST 6] Testing findFailingGradesByStudentAndCourse Query");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        List<ClassroomGradeEntry> failingGrades =
            gradeEntryRepository.findFailingGradesByStudentAndCourse(
                testData.student, testData.course);

        assertNotNull(failingGrades);
        assertTrue(failingGrades.stream().allMatch(e ->
            e.getPercentageGrade() != null && e.getPercentageGrade() < 60.0));

        System.out.println("✓ Found " + failingGrades.size() + " failing grades");
    }

    @Test
    @Order(7)
    @Transactional
    public void testGradeEntryRepository_AutoGradeCalculation() {
        System.out.println("\n[TEST 7] Testing Automatic Grade Calculation");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        // Get a grade entry with points
        ClassroomGradeEntry entry = testData.gradeEntries.stream()
            .filter(e -> e.getPointsEarned() != null)
            .findFirst()
            .orElse(null);

        assertNotNull(entry);
        assertNotNull(entry.getPercentageGrade());
        assertNotNull(entry.getLetterGrade());

        double expected = (entry.getPointsEarned() / entry.getPointsPossible()) * 100.0;
        assertEquals(expected, entry.getPercentageGrade(), 0.01);

        System.out.println("✓ Grade calculation verified: " +
            entry.getPointsEarned() + "/" + entry.getPointsPossible() +
            " = " + entry.getPercentageGrade() + "% (" + entry.getLetterGrade() + ")");
    }

    // ========================================================================
    // BEHAVIOR INCIDENT REPOSITORY TESTS
    // ========================================================================

    @Test
    @Order(8)
    @Transactional
    public void testBehaviorIncidentRepository_FindByStudent() {
        System.out.println("\n[TEST 8] Testing Behavior Incident findByStudent Query");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        List<BehaviorIncident> incidents = behaviorIncidentRepository.findByStudent(testData.student);

        assertNotNull(incidents);
        assertTrue(incidents.size() > 0);
        assertTrue(incidents.stream().allMatch(i -> i.getStudent().equals(testData.student)));

        long positiveCount = incidents.stream()
            .filter(i -> i.getBehaviorType() == BehaviorIncident.BehaviorType.POSITIVE)
            .count();
        long negativeCount = incidents.stream()
            .filter(i -> i.getBehaviorType() == BehaviorIncident.BehaviorType.NEGATIVE)
            .count();

        System.out.println("✓ Found " + incidents.size() + " total incidents");
        System.out.println("  - " + positiveCount + " positive");
        System.out.println("  - " + negativeCount + " negative");
    }

    @Test
    @Order(9)
    @Transactional
    public void testBehaviorIncidentRepository_FindByType() {
        System.out.println("\n[TEST 9] Testing findByStudentAndBehaviorType Query");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        List<BehaviorIncident> negativeIncidents =
            behaviorIncidentRepository.findByStudentAndBehaviorType(
                testData.student, BehaviorIncident.BehaviorType.NEGATIVE);

        assertNotNull(negativeIncidents);
        assertTrue(negativeIncidents.stream().allMatch(i ->
            i.getBehaviorType() == BehaviorIncident.BehaviorType.NEGATIVE));

        System.out.println("✓ Found " + negativeIncidents.size() + " negative incidents");
    }

    @Test
    @Order(10)
    @Transactional
    public void testBehaviorIncidentRepository_FindCriticalIncidents() {
        System.out.println("\n[TEST 10] Testing findCriticalIncidentsSince Query");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        LocalDate sinceDate = LocalDate.now().minusDays(30);
        List<BehaviorIncident> criticalIncidents =
            behaviorIncidentRepository.findCriticalIncidentsSince(testData.student, sinceDate);

        assertNotNull(criticalIncidents);
        assertTrue(criticalIncidents.stream().allMatch(i ->
            i.getSeverityLevel() == BehaviorIncident.SeverityLevel.MAJOR ||
            i.getAdminReferralRequired()));

        System.out.println("✓ Found " + criticalIncidents.size() + " critical incidents");
    }

    @Test
    @Order(11)
    @Transactional
    public void testBehaviorIncidentRepository_CountByTypeAndDate() {
        System.out.println("\n[TEST 11] Testing countByStudentAndBehaviorTypeAndDateBetween Query");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(14);

        Long negativeCount = behaviorIncidentRepository.countByStudentAndBehaviorTypeAndDateBetween(
            testData.student, BehaviorIncident.BehaviorType.NEGATIVE, startDate, endDate);

        assertNotNull(negativeCount);
        assertTrue(negativeCount >= 0);

        System.out.println("✓ Found " + negativeCount + " negative incidents in past 14 days");
    }

    // ========================================================================
    // TEACHER OBSERVATION NOTE REPOSITORY TESTS
    // ========================================================================

    @Test
    @Order(12)
    @Transactional
    public void testObservationNoteRepository_FindByStudent() {
        System.out.println("\n[TEST 12] Testing Observation Note findByStudent Query");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        List<TeacherObservationNote> notes = observationNoteRepository.findByStudent(testData.student);

        assertNotNull(notes);
        assertTrue(notes.size() > 0);
        assertTrue(notes.stream().allMatch(n -> n.getStudent().equals(testData.student)));

        System.out.println("✓ Found " + notes.size() + " observation notes");
    }

    @Test
    @Order(13)
    @Transactional
    public void testObservationNoteRepository_FindByInterventionFlag() {
        System.out.println("\n[TEST 13] Testing findByStudentAndIsFlagForIntervention Query");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        List<TeacherObservationNote> flaggedNotes =
            observationNoteRepository.findByStudentAndIsFlagForIntervention(testData.student, true);

        assertNotNull(flaggedNotes);
        assertTrue(flaggedNotes.stream().allMatch(n -> n.getIsFlagForIntervention()));

        System.out.println("✓ Found " + flaggedNotes.size() + " notes flagged for intervention");
    }

    @Test
    @Order(14)
    @Transactional
    public void testObservationNoteRepository_FindConcernObservations() {
        System.out.println("\n[TEST 14] Testing findConcernObservationsSince Query");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        LocalDate sinceDate = LocalDate.now().minusDays(30);
        List<TeacherObservationNote> concernNotes =
            observationNoteRepository.findConcernObservationsSince(testData.student, sinceDate);

        assertNotNull(concernNotes);
        assertTrue(concernNotes.stream().allMatch(n ->
            n.getObservationRating() == TeacherObservationNote.ObservationRating.CONCERN));

        System.out.println("✓ Found " + concernNotes.size() + " concern-level observations");
    }

    @Test
    @Order(15)
    @Transactional
    public void testObservationNoteRepository_FindByRating() {
        System.out.println("\n[TEST 15] Testing findByStudentAndObservationRating Query");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        List<TeacherObservationNote> excellentNotes =
            observationNoteRepository.findByStudentAndObservationRating(
                testData.student, TeacherObservationNote.ObservationRating.EXCELLENT);

        assertNotNull(excellentNotes);
        assertTrue(excellentNotes.stream().allMatch(n ->
            n.getObservationRating() == TeacherObservationNote.ObservationRating.EXCELLENT));

        System.out.println("✓ Found " + excellentNotes.size() + " excellent observations");
    }

    // ========================================================================
    // INTEGRATION TESTS
    // ========================================================================

    @Test
    @Order(16)
    @Transactional
    public void testIntegration_MultipleDataSources() {
        System.out.println("\n[TEST 16] Testing Multi-Source Data Integration");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        // Verify all data sources have data for the same student
        List<ClassroomGradeEntry> grades = gradeEntryRepository.findByStudent(testData.student);
        List<BehaviorIncident> incidents = behaviorIncidentRepository.findByStudent(testData.student);
        List<TeacherObservationNote> notes = observationNoteRepository.findByStudent(testData.student);

        assertTrue(grades.size() > 0, "Should have grade entries");
        assertTrue(incidents.size() > 0, "Should have behavior incidents");
        assertTrue(notes.size() > 0, "Should have observation notes");

        System.out.println("✓ Student has data in all 3 monitoring sources:");
        System.out.println("  - " + grades.size() + " grade entries");
        System.out.println("  - " + incidents.size() + " behavior incidents");
        System.out.println("  - " + notes.size() + " observation notes");
        System.out.println("✓ Multi-source integration verified");
    }

    @Test
    @Order(17)
    @Transactional
    public void testCleanup_AllDataDeleted() {
        System.out.println("\n[TEST 17] Testing Data Cleanup");
        System.out.println("-----------------------------------------------------");

        testData = testDataGenerator.generateCompleteDataset();

        long gradesBefore = gradeEntryRepository.count();
        long incidentsBefore = behaviorIncidentRepository.count();
        long notesBefore = observationNoteRepository.count();

        testDataGenerator.cleanupTestData();

        long gradesAfter = gradeEntryRepository.count();
        long incidentsAfter = behaviorIncidentRepository.count();
        long notesAfter = observationNoteRepository.count();

        assertEquals(0, gradesAfter, "All grade entries should be deleted");
        assertEquals(0, incidentsAfter, "All behavior incidents should be deleted");
        assertEquals(0, notesAfter, "All observation notes should be deleted");

        System.out.println("✓ Cleanup verified:");
        System.out.println("  - Grade entries: " + gradesBefore + " → " + gradesAfter);
        System.out.println("  - Behavior incidents: " + incidentsBefore + " → " + incidentsAfter);
        System.out.println("  - Observation notes: " + notesBefore + " → " + notesAfter);
    }
}
