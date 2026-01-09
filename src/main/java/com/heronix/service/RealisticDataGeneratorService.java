package com.heronix.service;

import com.heronix.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to generate realistic test data for a complete high school
 *
 * TEMPORARILY DISABLED - Use CSV Import Instead
 *
 * The programmatic data generator has been temporarily disabled due to
 * entity structure mismatches that need to be resolved.
 *
 * RECOMMENDED ALTERNATIVE:
 * Use CSV file import for loading test data:
 *   Location: H:\Heronix Scheduler\sample_data\
 *   Files available:
 *     - rooms.csv (82 rooms)
 *     - teachers.csv (75 teachers)
 *     - courses.csv (271 course sections)
 *     - students.csv (1,500 students)
 *     - events.csv (27 calendar events)
 *
 * Import via: File → Import → [Select Type] → Choose CSV file
 *
 * See documentation: TEST_DATA_CSV_IMPORT_GUIDE.md
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RealisticDataGeneratorService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final RoomRepository roomRepository;
    private final IEPRepository iepRepository;
    private final EventRepository eventRepository;
    private final CoTeacherRepository coTeacherRepository;
    private final ParaprofessionalRepository paraprofessionalRepository;

    /**
     * Generate all realistic test data
     *
     * TEMPORARILY DISABLED - Entity structure mismatches need to be resolved
     * Use CSV import from H:\Heronix Scheduler\sample_data\ instead
     */
    @Transactional
    public void generateAllData() {
        log.warn("=".repeat(80));
        log.warn("PROGRAMMATIC DATA GENERATOR TEMPORARILY DISABLED");
        log.warn("=".repeat(80));
        log.warn("");
        log.warn("The programmatic data generator has been temporarily disabled due to");
        log.warn("entity structure mismatches that need to be resolved.");
        log.warn("");
        log.warn("RECOMMENDED ALTERNATIVE:");
        log.warn("Use CSV file import instead for loading test data:");
        log.warn("  Location: H:\\Heronix Scheduler\\sample_data\\");
        log.warn("  Files available:");
        log.warn("    - rooms.csv (82 rooms)");
        log.warn("    - teachers.csv (75 teachers)");
        log.warn("    - courses.csv (271 course sections)");
        log.warn("    - students.csv (1,500 students)");
        log.warn("    - events.csv (27 calendar events)");
        log.warn("");
        log.warn("Import via: File -> Import -> [Select Type] -> Choose CSV file");
        log.warn("");
        log.warn("See documentation: TEST_DATA_CSV_IMPORT_GUIDE.md");
        log.warn("=".repeat(80));

        throw new UnsupportedOperationException(
            "Programmatic data generator is temporarily disabled. " +
            "Please use CSV import from H:\\Heronix Scheduler\\sample_data\\ instead. " +
            "See TEST_DATA_CSV_IMPORT_GUIDE.md for instructions."
        );
    }
}
