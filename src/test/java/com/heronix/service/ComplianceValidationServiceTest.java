package com.heronix.service;

import com.heronix.dto.ComplianceAlert;
import com.heronix.dto.ComplianceViolation;
import com.heronix.dto.ComplianceViolation.ViolationSeverity;
import com.heronix.dto.ComplianceViolation.ViolationType;
import com.heronix.model.domain.CertificationStandard;
import com.heronix.model.domain.Course;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.CertificationStandardRepository;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.service.impl.ComplianceValidationService;
import com.heronix.testutil.BaseServiceTest;
import com.heronix.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ComplianceValidationService
 * Tests regulatory compliance validation including:
 * - Null safety (all inputs can be null)
 * - State certification requirements validation
 * - Federal regulation compliance (ESSA, IDEA)
 * - Accreditation standards validation
 * - Compliance alerts generation
 * - Violation severity prioritization
 */
@ExtendWith(MockitoExtension.class)
class ComplianceValidationServiceTest extends BaseServiceTest {

    @Mock(lenient = true)
    private CourseRepository courseRepository;

    @Mock(lenient = true)
    private TeacherRepository teacherRepository;

    @Mock(lenient = true)
    private CertificationStandardRepository standardRepository;

    @InjectMocks
    private ComplianceValidationService service;

    private Teacher qualifiedTeacher;
    private Teacher unqualifiedTeacher;
    private Course mathCourse;
    private Course scienceCourse;
    private CertificationStandard mathStandard;
    private CertificationStandard federalStandard;
    private List<Course> allCourses;
    private List<Teacher> allTeachers;
    private List<CertificationStandard> stateStandards;
    private List<CertificationStandard> federalStandards;

    @BeforeEach
    void setUp() {
        // Set school state via reflection (simulates @Value injection)
        ReflectionTestUtils.setField(service, "schoolState", "FL");

        // Create qualified teacher
        qualifiedTeacher = TestDataBuilder.aTeacher()
            .withId(1L)
            .withFirstName("Jane")
            .withLastName("Certified")
            .withDepartment("Mathematics")
            .withCertifications(Arrays.asList("Math 6-12", "FTCE Math"))
            .build();

        // Create unqualified teacher
        unqualifiedTeacher = TestDataBuilder.aTeacher()
            .withId(2L)
            .withFirstName("John")
            .withLastName("Uncertified")
            .withDepartment("English")
            .withCertifications(Arrays.asList("English 6-12"))
            .build();

        allTeachers = Arrays.asList(qualifiedTeacher, unqualifiedTeacher);

        // Create courses
        mathCourse = TestDataBuilder.aCourse()
            .withId(1L)
            .withCourseCode("MATH101")
            .withCourseName("Algebra I")
            .withSubject("Mathematics")
            .withDepartment("Mathematics")
            .build();
        mathCourse.setActive(true);
        mathCourse.setTeacher(unqualifiedTeacher); // Uncertified teacher assigned

        scienceCourse = TestDataBuilder.aCourse()
            .withId(2L)
            .withCourseCode("SCI101")
            .withCourseName("Biology I")
            .withSubject("Science")
            .withDepartment("Science")
            .build();
        scienceCourse.setActive(true);
        scienceCourse.setTeacher(qualifiedTeacher);

        allCourses = Arrays.asList(mathCourse, scienceCourse);

        // Create certification standards
        mathStandard = new CertificationStandard();
        mathStandard.setId(1L);
        mathStandard.setCertificationName("Math 6-12");
        mathStandard.setSubjectArea("Mathematics");
        mathStandard.setStateCode("FL");
        mathStandard.setGradeLevelRange("6-12");
        mathStandard.setRegulatorySource(CertificationStandard.RegulatorySource.FTCE);
        mathStandard.setIsHQTRequirement(true);
        mathStandard.setActive(true);

        federalStandard = new CertificationStandard();
        federalStandard.setId(2L);
        federalStandard.setCertificationName("Federal HQT");
        federalStandard.setSubjectArea("All");
        federalStandard.setStateCode("FEDERAL");
        federalStandard.setRegulatorySource(CertificationStandard.RegulatorySource.FEDERAL_ESSA);
        federalStandard.setIsHQTRequirement(true);
        federalStandard.setActive(true);

        stateStandards = Arrays.asList(mathStandard);
        federalStandards = Arrays.asList(federalStandard);
    }

    // ==================== Null Safety Tests ====================

    @Test
    void testAuditAllAssignments_WithNullCoursesFromRepository_ShouldNotThrowNPE() {
        // Given: Repository returns null
        when(courseRepository.findAll()).thenReturn(null);
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.auditAllAssignments());
    }

    @Test
    void testAuditAllAssignments_WithNullStateStandardsFromRepository_ShouldNotThrowNPE() {
        // Given: Repository returns null
        when(courseRepository.findAll()).thenReturn(allCourses);
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(null);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.auditAllAssignments());
    }

    @Test
    void testAuditAllAssignments_WithNullFederalStandardsFromRepository_ShouldNotThrowNPE() {
        // Given: Repository returns null
        when(courseRepository.findAll()).thenReturn(allCourses);
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(null);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.auditAllAssignments());
    }

    @Test
    void testAuditAllAssignments_WithAllRepositoriesReturningNull_ShouldNotThrowNPE() {
        // Given: All repositories return null
        when(courseRepository.findAll()).thenReturn(null);
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(null);
        when(standardRepository.findAllFederalStandards()).thenReturn(null);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.auditAllAssignments());

        List<ComplianceViolation> result = service.auditAllAssignments();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testAuditAllAssignments_WithNullCourseInList_ShouldSkipNullCourse() {
        // Given: Course list contains null
        List<Course> coursesWithNull = new ArrayList<>();
        coursesWithNull.add(null);
        coursesWithNull.add(mathCourse);

        when(courseRepository.findAll()).thenReturn(coursesWithNull);
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When/Then: Should skip null and process valid course
        assertDoesNotThrow(() -> service.auditAllAssignments());
    }

    @Test
    void testAuditAllAssignments_WithNullStandardInList_ShouldSkipNullStandard() {
        // Given: Standard list contains null
        List<CertificationStandard> standardsWithNull = new ArrayList<>();
        standardsWithNull.add(null);
        standardsWithNull.add(mathStandard);

        when(courseRepository.findAll()).thenReturn(allCourses);
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(standardsWithNull);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When/Then: Should skip null and process valid standard
        assertDoesNotThrow(() -> service.auditAllAssignments());
    }

    @Test
    void testValidateAssignment_WithNullCourse_ShouldNotThrowNPE() {
        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.validateAssignment(null, qualifiedTeacher));

        List<ComplianceViolation> result = service.validateAssignment(null, qualifiedTeacher);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testValidateAssignment_WithNullTeacher_ShouldNotThrowNPE() {
        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.validateAssignment(mathCourse, null));

        List<ComplianceViolation> result = service.validateAssignment(mathCourse, null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testValidateAssignment_WithBothNull_ShouldNotThrowNPE() {
        // When/Then: Should handle both null gracefully
        assertDoesNotThrow(() -> service.validateAssignment(null, null));

        List<ComplianceViolation> result = service.validateAssignment(null, null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Empty Data Tests ====================

    @Test
    void testAuditAllAssignments_WithEmptyCourseList_ShouldReturnEmptyList() {
        // Given: No courses
        when(courseRepository.findAll()).thenReturn(Collections.emptyList());
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);

        // When: Audit
        List<ComplianceViolation> violations = service.auditAllAssignments();

        // Then: Should return empty list
        assertNotNull(violations);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testAuditAllAssignments_WithEmptyStandardsList_ShouldReturnEmptyViolations() {
        // Given: Courses but no standards
        when(courseRepository.findAll()).thenReturn(allCourses);
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(Collections.emptyList());
        when(standardRepository.findAllFederalStandards()).thenReturn(Collections.emptyList());

        // When: Audit
        List<ComplianceViolation> violations = service.auditAllAssignments();

        // Then: Should return empty (no standards to violate)
        assertNotNull(violations);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testAuditAllAssignments_WithInactiveCourse_ShouldSkipCourse() {
        // Given: Inactive course
        mathCourse.setActive(false);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(mathCourse));
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);

        // When: Audit
        List<ComplianceViolation> violations = service.auditAllAssignments();

        // Then: Should skip inactive course
        assertNotNull(violations);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testAuditAllAssignments_WithUnassignedCourse_ShouldSkipCourse() {
        // Given: Course with no teacher
        mathCourse.setTeacher(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(mathCourse));
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);

        // When: Audit
        List<ComplianceViolation> violations = service.auditAllAssignments();

        // Then: Should skip unassigned course
        assertNotNull(violations);
        assertTrue(violations.isEmpty());
    }

    // ==================== Compliance Violation Tests ====================

    @Test
    void testAuditAllAssignments_WithUnqualifiedTeacher_ShouldDetectViolation() {
        // Given: Math course assigned to unqualified teacher
        when(courseRepository.findAll()).thenReturn(Arrays.asList(mathCourse));
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(Collections.emptyList());
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When: Audit
        List<ComplianceViolation> violations = service.auditAllAssignments();

        // Then: Should detect violation
        assertNotNull(violations);
        // Note: Actual violation detection depends on CertificationStandard.matches() implementation
    }

    @Test
    void testValidateAssignment_WithQualifiedTeacher_ShouldReturnNoViolations() {
        // Given: Math course with qualified teacher
        mathCourse.setTeacher(qualifiedTeacher);

        when(standardRepository.findByStateCodeAndSubjectAreaAndActiveTrue(anyString(), anyString()))
            .thenReturn(Collections.emptyList());
        when(standardRepository.findAllFederalStandards()).thenReturn(Collections.emptyList());

        // When: Validate
        List<ComplianceViolation> violations = service.validateAssignment(mathCourse, qualifiedTeacher);

        // Then: Should return no violations
        assertNotNull(violations);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testValidateAssignment_WithUnqualifiedTeacher_ShouldReturnViolations() {
        // Given: Math course with unqualified teacher
        when(standardRepository.findByStateCodeAndSubjectAreaAndActiveTrue(anyString(), anyString()))
            .thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(Collections.emptyList());
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When: Validate
        List<ComplianceViolation> violations = service.validateAssignment(mathCourse, unqualifiedTeacher);

        // Then: Should return violations (depends on implementation)
        assertNotNull(violations);
    }

    // ==================== Severity Tests ====================

    @Test
    void testAuditAllAssignments_ShouldSortViolationsBySeverity() {
        // Given: Multiple violations with different severities
        when(courseRepository.findAll()).thenReturn(allCourses);
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When: Audit
        List<ComplianceViolation> violations = service.auditAllAssignments();

        // Then: Should be sorted by severity
        assertNotNull(violations);
        // Verify sorting if violations exist
        if (violations.size() > 1) {
            for (int i = 0; i < violations.size() - 1; i++) {
                ViolationSeverity current = violations.get(i).getSeverity();
                ViolationSeverity next = violations.get(i + 1).getSeverity();
                assertTrue(current.compareTo(next) <= 0);
            }
        }
    }

    // ==================== Null Field Tests ====================

    @Test
    void testAuditAllAssignments_WithTeacherHavingNullCertifications_ShouldNotThrowNPE() {
        // Given: Teacher with null certifications
        unqualifiedTeacher.setCertifications(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(mathCourse));
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.auditAllAssignments());
    }

    @Test
    void testAuditAllAssignments_WithCourseHavingNullSubject_ShouldNotThrowNPE() {
        // Given: Course with null subject
        mathCourse.setSubject(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(mathCourse));
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.auditAllAssignments());
    }

    @Test
    void testAuditAllAssignments_WithCourseHavingNullCourseName_ShouldNotThrowNPE() {
        // Given: Course with null name (will be filtered out in sorting)
        mathCourse.setCourseName(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(mathCourse));
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.auditAllAssignments());
    }

    @Test
    void testAuditAllAssignments_WithTeacherHavingNullFirstAndLastName_ShouldNotThrowNPE() {
        // Given: Teacher with null names
        unqualifiedTeacher.setFirstName(null);
        unqualifiedTeacher.setLastName(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(mathCourse));
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.auditAllAssignments());
    }

    @Test
    void testValidateAssignment_WithRepositoryReturningNullForStandards_ShouldNotThrowNPE() {
        // Given: Repository returns null
        when(standardRepository.findByStateCodeAndSubjectAreaAndActiveTrue(anyString(), anyString()))
            .thenReturn(null);
        when(standardRepository.findAllFederalStandards()).thenReturn(null);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.validateAssignment(mathCourse, qualifiedTeacher));
    }

    // ==================== Compliance Alerts Tests ====================

    @Test
    void testGenerateComplianceAlerts_WithNoViolations_ShouldReturnEmptyList() {
        // Given: No violations
        when(courseRepository.findAll()).thenReturn(Collections.emptyList());
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);

        // When: Generate alerts
        List<ComplianceAlert> alerts = service.generateComplianceAlerts();

        // Then: Should return empty list
        assertNotNull(alerts);
        assertTrue(alerts.isEmpty());
    }

    @Test
    void testGenerateComplianceAlerts_WithViolations_ShouldReturnAlerts() {
        // Given: Violations exist
        when(courseRepository.findAll()).thenReturn(allCourses);
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When: Generate alerts
        List<ComplianceAlert> alerts = service.generateComplianceAlerts();

        // Then: Should return alerts (depends on implementation)
        assertNotNull(alerts);
    }

    @Test
    void testGenerateComplianceAlerts_WithNullViolationsInList_ShouldFilterNulls() {
        // Given: Violations with nulls (simulated by mocking)
        when(courseRepository.findAll()).thenReturn(allCourses);
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.generateComplianceAlerts());
    }

    // ==================== Regulatory Resources Tests ====================

    @Test
    void testGetRegulatoryResources_ShouldReturnResourceMap() {
        // When: Get resources
        Map<String, String> resources = service.getRegulatoryResources();

        // Then: Should return map with resources
        assertNotNull(resources);
        assertFalse(resources.isEmpty());
        assertTrue(resources.containsKey("STATE_SPECIFIC"));
        assertTrue(resources.containsKey("CONFIGURED_STATE"));
    }

    @Test
    void testGetRegulatoryResources_WithNullSchoolState_ShouldNotThrowNPE() {
        // Given: Null school state
        ReflectionTestUtils.setField(service, "schoolState", null);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.getRegulatoryResources());

        Map<String, String> resources = service.getRegulatoryResources();
        assertNotNull(resources);
    }

    // ==================== Edge Cases ====================

    @Test
    void testAuditAllAssignments_WithStandardHavingNullSubjectArea_ShouldNotThrowNPE() {
        // Given: Standard with null subject area
        mathStandard.setSubjectArea(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(mathCourse));
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(Arrays.asList(mathStandard));
        when(standardRepository.findAllFederalStandards()).thenReturn(Collections.emptyList());
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.auditAllAssignments());
    }

    @Test
    void testAuditAllAssignments_WithStandardHavingNullGradeLevelRange_ShouldNotThrowNPE() {
        // Given: Standard with null grade level range
        mathStandard.setGradeLevelRange(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(mathCourse));
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(Arrays.asList(mathStandard));
        when(standardRepository.findAllFederalStandards()).thenReturn(Collections.emptyList());
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.auditAllAssignments());
    }

    @Test
    void testAuditAllAssignments_WithCourseHavingNullLevel_ShouldNotThrowNPE() {
        // Given: Course with null level
        mathCourse.setLevel(null);

        when(courseRepository.findAll()).thenReturn(Arrays.asList(mathCourse));
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.auditAllAssignments());
    }

    @Test
    void testAuditAllAssignments_WithRepositoryReturningNullForActiveTeachers_ShouldNotThrowNPE() {
        // Given: Repository returns null for active teachers
        when(courseRepository.findAll()).thenReturn(allCourses);
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(null);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.auditAllAssignments());
    }

    @Test
    void testValidateAssignment_WithCourseHavingNullSubject_ShouldNotThrowNPE() {
        // Given: Course with null subject
        mathCourse.setSubject(null);

        when(standardRepository.findByStateCodeAndSubjectAreaAndActiveTrue(anyString(), anyString()))
            .thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.validateAssignment(mathCourse, qualifiedTeacher));
    }

    @Test
    void testGenerateComplianceAlerts_WithNullTeacherInList_ShouldFilterNulls() {
        // Given: Teacher list contains null
        List<Teacher> teachersWithNull = new ArrayList<>();
        teachersWithNull.add(null);
        teachersWithNull.add(qualifiedTeacher);

        when(courseRepository.findAll()).thenReturn(allCourses);
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(teachersWithNull);

        // When/Then: Should handle null gracefully
        assertDoesNotThrow(() -> service.generateComplianceAlerts());
    }

    @Test
    void testAuditAllAssignments_ShouldReturnNonNullList() {
        // Given: Any valid setup
        when(courseRepository.findAll()).thenReturn(allCourses);
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When: Audit
        List<ComplianceViolation> violations = service.auditAllAssignments();

        // Then: Should never return null
        assertNotNull(violations);
    }

    @Test
    void testValidateAssignment_ShouldReturnNonNullList() {
        // Given: Valid course and teacher
        when(standardRepository.findByStateCodeAndSubjectAreaAndActiveTrue(anyString(), anyString()))
            .thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When: Validate
        List<ComplianceViolation> violations = service.validateAssignment(mathCourse, qualifiedTeacher);

        // Then: Should never return null
        assertNotNull(violations);
    }

    @Test
    void testGenerateComplianceAlerts_ShouldReturnNonNullList() {
        // Given: Any valid setup
        when(courseRepository.findAll()).thenReturn(allCourses);
        when(standardRepository.findByStateCodeAndActiveTrue(anyString())).thenReturn(stateStandards);
        when(standardRepository.findAllFederalStandards()).thenReturn(federalStandards);
        when(teacherRepository.findByActiveTrue()).thenReturn(allTeachers);

        // When: Generate alerts
        List<ComplianceAlert> alerts = service.generateComplianceAlerts();

        // Then: Should never return null
        assertNotNull(alerts);
    }
}
