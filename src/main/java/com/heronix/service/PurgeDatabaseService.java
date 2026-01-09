package com.heronix.service;

import com.heronix.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

// Additional repository imports for Phase 5 lunch features
import com.heronix.repository.LunchWaveRepository;
import com.heronix.repository.StudentLunchAssignmentRepository;
import com.heronix.repository.TeacherLunchAssignmentRepository;
import com.heronix.repository.OptimizationResultRepository;

/**
 * Service for purging all data from the database.
 *
 * This is a nuclear option that deletes ALL data from the system.
 * Use with extreme caution!
 *
 * @author Heronix Scheduler Team
 */
@Service
public class PurgeDatabaseService {

    private static final Logger log = LoggerFactory.getLogger(PurgeDatabaseService.class);

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SubstituteRepository substituteRepository;

    @Autowired
    private ParaprofessionalRepository paraprofessionalRepository;

    @Autowired
    private CoTeacherRepository coTeacherRepository;

    @Autowired
    private SpecialDutyAssignmentRepository specialDutyAssignmentRepository;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private IEPRepository iepRepository;

    @Autowired
    private OptimizationResultRepository optimizationResultRepository;

    @Autowired
    private LunchWaveRepository lunchWaveRepository;

    @Autowired
    private StudentLunchAssignmentRepository studentLunchAssignmentRepository;

    @Autowired
    private TeacherLunchAssignmentRepository teacherLunchAssignmentRepository;

    /**
     * Purges ALL data from the database.
     *
     * This method deletes data in the correct order to respect foreign key constraints.
     *
     * @param progressCallback Callback for progress updates (optional)
     * @throws Exception if purge fails
     */
    @Transactional
    public void purgeAllData(Consumer<String> progressCallback) throws Exception {
        log.warn("========================================");
        log.warn("DATABASE PURGE INITIATED - ALL DATA WILL BE DELETED");
        log.warn("========================================");

        try {
            // Delete in correct order to respect foreign key constraints

            // 1. Delete schedule-related data first (child tables before parent)
            updateProgress(progressCallback, "Deleting schedule-dependent data...");

            // Delete optimization results (references schedules)
            optimizationResultRepository.deleteAll();
            log.info("Deleted all optimization results");

            // Delete lunch assignments (reference lunch waves)
            studentLunchAssignmentRepository.deleteAll();
            log.info("Deleted all student lunch assignments");

            teacherLunchAssignmentRepository.deleteAll();
            log.info("Deleted all teacher lunch assignments");

            // Delete lunch waves (references schedules)
            lunchWaveRepository.deleteAll();
            log.info("Deleted all lunch waves");

            // Now safe to delete schedules
            updateProgress(progressCallback, "Deleting schedules...");
            scheduleRepository.deleteAll();
            log.info("Deleted all schedules");

            // 2. Delete events
            updateProgress(progressCallback, "Deleting events...");
            eventRepository.deleteAll();
            log.info("Deleted all events");

            // 3. Delete substitute-related data
            updateProgress(progressCallback, "Deleting substitute assignments...");
            substituteRepository.deleteAll();
            log.info("Deleted all substitutes");

            updateProgress(progressCallback, "Deleting paraprofessional assignments...");
            paraprofessionalRepository.deleteAll();
            log.info("Deleted all paraprofessionals");

            updateProgress(progressCallback, "Deleting co-teacher assignments...");
            coTeacherRepository.deleteAll();
            log.info("Deleted all co-teachers");

            updateProgress(progressCallback, "Deleting special duty assignments...");
            specialDutyAssignmentRepository.deleteAll();
            log.info("Deleted all special duty assignments");

            // 4. Delete waitlists (references students)
            updateProgress(progressCallback, "Deleting course waitlists...");
            waitlistRepository.deleteAll();
            log.info("Deleted all waitlists");

            // 5. Delete IEPs (references students)
            updateProgress(progressCallback, "Deleting IEP records...");
            iepRepository.deleteAll();
            log.info("Deleted all IEP records");

            // 6. Delete students (includes enrollments via cascade)
            updateProgress(progressCallback, "Deleting students and enrollments...");
            long studentCount = studentRepository.count();
            studentRepository.deleteAll();
            log.info("Deleted {} students", studentCount);

            // 7. Delete courses (includes prerequisites via cascade)
            updateProgress(progressCallback, "Deleting courses and prerequisites...");
            long courseCount = courseRepository.count();
            courseRepository.deleteAll();
            log.info("Deleted {} courses", courseCount);

            // 8. Delete rooms
            updateProgress(progressCallback, "Deleting rooms...");
            long roomCount = roomRepository.count();
            roomRepository.deleteAll();
            log.info("Deleted {} rooms", roomCount);

            // 9. Delete teachers (includes certifications via cascade)
            updateProgress(progressCallback, "Deleting teachers and certifications...");
            long teacherCount = teacherRepository.countByActiveTrue();
            teacherRepository.deleteAll();
            log.info("Deleted {} teachers", teacherCount);

            updateProgress(progressCallback, "Database purge complete!");

            log.warn("========================================");
            log.warn("DATABASE PURGE COMPLETED SUCCESSFULLY");
            log.warn("All data has been deleted from the database");
            log.warn("========================================");

        } catch (Exception e) {
            log.error("Database purge failed", e);
            throw new Exception("Failed to purge database: " + e.getMessage(), e);
        }
    }

    /**
     * Updates progress via callback if provided.
     *
     * @param callback Progress callback
     * @param message Progress message
     */
    private void updateProgress(Consumer<String> callback, String message) {
        if (callback != null) {
            callback.accept(message);
        }
    }

    /**
     * Returns the total count of all data entities in the database.
     *
     * @return Total entity count
     */
    public long getTotalEntityCount() {
        long count = 0;
        count += teacherRepository.countByActiveTrue();
        count += studentRepository.count();
        count += courseRepository.count();
        count += roomRepository.count();
        count += scheduleRepository.count();
        count += eventRepository.count();
        count += substituteRepository.count();
        count += paraprofessionalRepository.count();
        count += coTeacherRepository.count();
        count += specialDutyAssignmentRepository.count();
        count += waitlistRepository.count();
        count += iepRepository.count();
        count += optimizationResultRepository.count();
        count += lunchWaveRepository.count();
        count += studentLunchAssignmentRepository.count();
        count += teacherLunchAssignmentRepository.count();
        return count;
    }

    /**
     * Checks if the database is empty (no data entities).
     *
     * @return true if database has no data
     */
    public boolean isDatabaseEmpty() {
        return getTotalEntityCount() == 0;
    }
}
