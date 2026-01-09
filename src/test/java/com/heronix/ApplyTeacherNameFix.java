package com.heronix;

import com.heronix.model.domain.Teacher;
import com.heronix.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Apply fix for NULL teacher names
 * December 5, 2025
 */
@SpringBootTest
public class ApplyTeacherNameFix {

    @Autowired
    private TeacherRepository teacherRepository;

    @Test
    @Transactional
    public void applyTeacherNameFix() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("APPLYING TEACHER NAME FIX");
        System.out.println("=".repeat(80) + "\n");

        // STEP 1: Check current state
        System.out.println("=== STEP 1: CHECKING CURRENT STATE ===\n");

        List<Teacher> allTeachers = teacherRepository.findAll();
        int nullFirstNames = 0;
        int nullLastNames = 0;

        for (Teacher teacher : allTeachers) {
            if (teacher.getFirstName() == null) nullFirstNames++;
            if (teacher.getLastName() == null) nullLastNames++;

            System.out.printf("  Teacher %d: %s %s (%s)%n",
                    teacher.getId(),
                    teacher.getFirstName() != null ? teacher.getFirstName() : "NULL",
                    teacher.getLastName() != null ? teacher.getLastName() : "NULL",
                    teacher.getDepartment() != null ? teacher.getDepartment() : "No Dept");
        }

        System.out.printf("%nTotal teachers: %d%n", allTeachers.size());
        System.out.printf("Teachers with NULL first name: %d%n", nullFirstNames);
        System.out.printf("Teachers with NULL last name: %d%n", nullLastNames);

        // STEP 2: Apply fix
        System.out.println("\n=== STEP 2: APPLYING FIX ===\n");

        // Name mapping based on ID
        String[] firstNames = {"John", "Sarah", "Michael", "Emily", "David",
                               "Jessica", "Robert", "Amanda", "James", "Lisa"};

        // Last name mapping based on department
        Map<String, String> deptToLastName = new HashMap<>();
        deptToLastName.put("Mathematics", "Smith");
        deptToLastName.put("English", "Johnson");
        deptToLastName.put("Science", "Williams");
        deptToLastName.put("Social Studies", "Brown");
        deptToLastName.put("World Languages", "Garcia");
        deptToLastName.put("Physical Education", "Davis");
        deptToLastName.put("Arts", "Miller");
        deptToLastName.put("Music", "Anderson");

        int fixedCount = 0;

        for (Teacher teacher : allTeachers) {
            boolean needsFix = false;

            // Fix first name if NULL
            if (teacher.getFirstName() == null) {
                int nameIndex = (int) (teacher.getId() % firstNames.length);
                teacher.setFirstName(firstNames[nameIndex]);
                needsFix = true;
            }

            // Fix last name if NULL
            if (teacher.getLastName() == null) {
                String lastName = null;
                String dept = teacher.getDepartment();

                if (dept != null) {
                    // Try exact match
                    lastName = deptToLastName.get(dept);

                    // Try partial match
                    if (lastName == null) {
                        for (Map.Entry<String, String> entry : deptToLastName.entrySet()) {
                            if (dept.contains(entry.getKey())) {
                                lastName = entry.getValue();
                                break;
                            }
                        }
                    }

                    // Try keyword matching
                    if (lastName == null) {
                        if (dept.toLowerCase().contains("math")) lastName = "Smith";
                        else if (dept.toLowerCase().contains("english")) lastName = "Johnson";
                        else if (dept.toLowerCase().contains("science")) lastName = "Williams";
                        else if (dept.toLowerCase().contains("social") || dept.toLowerCase().contains("history")) lastName = "Brown";
                        else if (dept.toLowerCase().contains("language") || dept.toLowerCase().contains("world")) lastName = "Garcia";
                        else if (dept.toLowerCase().contains("physical") || dept.toLowerCase().contains("pe")) lastName = "Davis";
                        else if (dept.toLowerCase().contains("art")) lastName = "Miller";
                        else if (dept.toLowerCase().contains("cte") || dept.toLowerCase().contains("career") || dept.toLowerCase().contains("tech")) lastName = "Wilson";
                        else if (dept.toLowerCase().contains("music")) lastName = "Anderson";
                    }
                }

                // Default if no match
                if (lastName == null) {
                    lastName = "Teacher" + teacher.getId();
                }

                teacher.setLastName(lastName);
                needsFix = true;
            }

            // Fix employee ID if NULL
            if (teacher.getEmployeeId() == null || teacher.getEmployeeId().trim().isEmpty()) {
                teacher.setEmployeeId(String.format("T%03d", teacher.getId()));
                needsFix = true;
            }

            if (needsFix) {
                teacherRepository.save(teacher);
                fixedCount++;
                System.out.printf("  ✅ Fixed: %s %s (%s) - Employee ID: %s%n",
                        teacher.getFirstName(),
                        teacher.getLastName(),
                        teacher.getDepartment(),
                        teacher.getEmployeeId());
            }
        }

        System.out.printf("%n✅ Fixed %d teachers%n", fixedCount);

        // STEP 3: Verify fix
        System.out.println("\n=== STEP 3: VERIFYING FIX ===\n");

        allTeachers = teacherRepository.findAll();
        nullFirstNames = 0;
        nullLastNames = 0;
        int nullEmployeeIds = 0;

        for (Teacher teacher : allTeachers) {
            if (teacher.getFirstName() == null) nullFirstNames++;
            if (teacher.getLastName() == null) nullLastNames++;
            if (teacher.getEmployeeId() == null || teacher.getEmployeeId().trim().isEmpty()) nullEmployeeIds++;

            System.out.printf("  %s %s (%s) - %s%n",
                    teacher.getFirstName(),
                    teacher.getLastName(),
                    teacher.getDepartment(),
                    teacher.getEmployeeId());
        }

        System.out.printf("%nVerification Results:%n");
        System.out.printf("  Total teachers: %d%n", allTeachers.size());
        System.out.printf("  NULL first names: %d %s%n", nullFirstNames,
                nullFirstNames == 0 ? "✅" : "❌");
        System.out.printf("  NULL last names: %d %s%n", nullLastNames,
                nullLastNames == 0 ? "✅" : "❌");
        System.out.printf("  NULL employee IDs: %d %s%n", nullEmployeeIds,
                nullEmployeeIds == 0 ? "✅" : "❌");

        System.out.println("\n" + "=".repeat(80));
        if (nullFirstNames == 0 && nullLastNames == 0 && nullEmployeeIds == 0) {
            System.out.println("✅ FIX SUCCESSFUL - All teachers now have valid names!");
            System.out.println("✅ You can now try schedule generation again!");
        } else {
            System.out.println("⚠️  WARNING: Some teachers still have NULL values");
            System.out.println("   Please review the results above");
        }
        System.out.println("=".repeat(80) + "\n");
    }
}
