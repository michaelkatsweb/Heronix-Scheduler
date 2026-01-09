package com.heronix.service;

import com.heronix.model.dto.CleanupResult;
import com.heronix.model.dto.DatabaseStats;

public interface DataManagementService {
    CleanupResult deleteAllStudents();
    CleanupResult deleteAllTeachers();
    CleanupResult deleteAllCourses();
    CleanupResult deleteAllRooms();
    CleanupResult deleteAllEvents();
    CleanupResult deleteStudentsByGradeLevel(String gradeLevel);
    CleanupResult deleteTeachersByDepartment(String department);
    CleanupResult deleteInactiveStudents();
    CleanupResult deleteInactiveTeachers();
    DatabaseStats getDatabaseStats();
}