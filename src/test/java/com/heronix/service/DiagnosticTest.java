package com.heronix.service;

import com.heronix.model.domain.*;
import com.heronix.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class DiagnosticTest {

    @Autowired
    private CourseRequestRepository courseRequestRepository;

    @Autowired
    private ConflictMatrixRepository conflictMatrixRepository;

    @Test
    void diagnoseCourseRequests() {
        System.out.println("\n========== DIAGNOSTIC: Course Requests ==========\n");

        List<CourseRequest> allRequests = courseRequestRepository.findAll();
        System.out.println("Total course requests in DB: " + allRequests.size());

        for (CourseRequest req : allRequests) {
            System.out.println("  - Student: " + req.getStudent().getFirstName() +
                             ", Course: " + req.getCourse().getCourseName() +
                             ", Year: " + req.getRequestYear() +
                             ", Status: " + req.getRequestStatus());
        }

        System.out.println("\nRequests for year 2026:");
        List<CourseRequest> requests2026 = courseRequestRepository.findPendingRequestsForYear(2026);
        System.out.println("  Found: " + requests2026.size());

        System.out.println("\nRequests for year 2027:");
        List<CourseRequest> requests2027 = courseRequestRepository.findPendingRequestsForYear(2027);
        System.out.println("  Found: " + requests2027.size());

        System.out.println("\n========== DIAGNOSTIC: Conflict Matrix ==========\n");

        List<ConflictMatrix> allConflicts = conflictMatrixRepository.findAll();
        System.out.println("Total conflicts in DB: " + allConflicts.size());

        for (ConflictMatrix conflict : allConflicts) {
            System.out.println("  - " + conflict.getCourse1().getCourseName() +
                             " ↔ " + conflict.getCourse2().getCourseName() +
                             ": " + conflict.getConflictCount() +
                             " (year: " + conflict.getScheduleYear() +
                             ", singleton: " + conflict.getIsSingletonConflict() + ")");
        }

        System.out.println("\n========== END DIAGNOSTIC ==========\n");
    }
}
