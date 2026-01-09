package com.heronix.service;

import com.heronix.model.domain.Student;
import com.heronix.model.domain.Teacher;
import com.heronix.model.domain.Course;
import com.heronix.model.domain.Room;
import com.heronix.repository.StudentRepository;
import com.heronix.repository.TeacherRepository;
import com.heronix.repository.CourseRepository;
import com.heronix.repository.RoomRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global Search Service - Searches across all entities
 * Powers the Command Palette (Ctrl+K) feature
 *
 * @author Heronix Scheduler Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private static final Logger log = LoggerFactory.getLogger(GlobalSearchService.class);

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final RoomRepository roomRepository;

    /**
     * Search Result DTO - represents a single search result
     */
    @Data
    public static class SearchResult {
        private String type;           // "STUDENT", "TEACHER", "COURSE", "ROOM", "ACTION"
        private String icon;           // Emoji icon
        private String primaryText;    // Main display text
        private String secondaryText;  // Subtitle/context text
        private Object data;           // The actual entity or action data
        private double relevanceScore; // 0.0 - 1.0, for ranking results

        public SearchResult(String type, String icon, String primaryText, String secondaryText, Object data, double relevanceScore) {
            this.type = type;
            this.icon = icon;
            this.primaryText = primaryText;
            this.secondaryText = secondaryText;
            this.data = data;
            this.relevanceScore = relevanceScore;
        }

        public String getDisplayText() {
            return icon + "  " + primaryText;
        }

        public String getFullDisplayText() {
            if (secondaryText != null && !secondaryText.isEmpty()) {
                return icon + "  " + primaryText + "\n   " + secondaryText;
            }
            return getDisplayText();
        }
    }

    /**
     * Main search method - searches across all entities
     *
     * @param query Search query string
     * @param category Filter category ("ALL", "STUDENTS", "TEACHERS", "COURSES", "ROOMS", "ACTIONS")
     * @return List of search results sorted by relevance
     */
    public List<SearchResult> search(String query, String category) {
        if (query == null || query.trim().isEmpty()) {
            return getDefaultSuggestions(category);
        }

        String normalizedQuery = query.toLowerCase().trim();
        List<SearchResult> results = new ArrayList<>();

        // Search based on category filter
        if ("ALL".equals(category) || "STUDENTS".equals(category)) {
            results.addAll(searchStudents(normalizedQuery));
        }

        if ("ALL".equals(category) || "TEACHERS".equals(category)) {
            results.addAll(searchTeachers(normalizedQuery));
        }

        if ("ALL".equals(category) || "COURSES".equals(category)) {
            results.addAll(searchCourses(normalizedQuery));
        }

        if ("ALL".equals(category) || "ROOMS".equals(category)) {
            results.addAll(searchRooms(normalizedQuery));
        }

        if ("ALL".equals(category) || "ACTIONS".equals(category)) {
            results.addAll(searchActions(normalizedQuery));
        }

        // Sort by relevance score (highest first)
        results.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));

        return results;
    }

    /**
     * Search students by ID, name, email, grade
     */
    private List<SearchResult> searchStudents(String query) {
        List<SearchResult> results = new ArrayList<>();

        try {
            List<Student> students = studentRepository.findAll();

            for (Student student : students) {
                double score = calculateStudentRelevance(student, query);
                if (score > 0) {
                    String primaryText = String.format("%s %s (ID: %s)",
                            student.getFirstName(),
                            student.getLastName(),
                            student.getStudentId());

                    String secondaryText = String.format("Grade %s • %s • %s",
                            student.getGradeLevel() != null ? student.getGradeLevel() : "N/A",
                            student.getEmail() != null ? student.getEmail() : "No email",
                            student.isActive() ? "Active" : "Inactive");

                    results.add(new SearchResult(
                            "STUDENT",
                            "👨‍🎓",
                            primaryText,
                            secondaryText,
                            student,
                            score
                    ));
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the search
            log.error("Error searching students: {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * Calculate relevance score for student
     */
    private double calculateStudentRelevance(Student student, String query) {
        double score = 0.0;

        // Exact match on student ID (highest priority)
        if (student.getStudentId() != null && student.getStudentId().toLowerCase().equals(query)) {
            score = 1.0;
        }
        // Partial match on student ID
        else if (student.getStudentId() != null && student.getStudentId().toLowerCase().contains(query)) {
            score = 0.9;
        }
        // Full name match
        else if ((student.getFirstName() + " " + student.getLastName()).toLowerCase().contains(query)) {
            score = 0.8;
        }
        // First name match
        else if (student.getFirstName() != null && student.getFirstName().toLowerCase().contains(query)) {
            score = 0.7;
        }
        // Last name match
        else if (student.getLastName() != null && student.getLastName().toLowerCase().contains(query)) {
            score = 0.7;
        }
        // Email match
        else if (student.getEmail() != null && student.getEmail().toLowerCase().contains(query)) {
            score = 0.6;
        }
        // Grade level match
        else if (student.getGradeLevel() != null && student.getGradeLevel().toLowerCase().contains(query)) {
            score = 0.5;
        }

        return score;
    }

    /**
     * Search teachers by name, email, department
     */
    private List<SearchResult> searchTeachers(String query) {
        List<SearchResult> results = new ArrayList<>();

        try {
            List<Teacher> teachers = teacherRepository.findAllActive();

            for (Teacher teacher : teachers) {
                double score = calculateTeacherRelevance(teacher, query);
                if (score > 0) {
                    String primaryText = String.format("%s %s",
                            teacher.getFirstName(),
                            teacher.getLastName());

                    String secondaryText = String.format("%s • %s",
                            teacher.getEmail() != null ? teacher.getEmail() : "No email",
                            teacher.getDepartment() != null ? teacher.getDepartment() : "No department");

                    results.add(new SearchResult(
                            "TEACHER",
                            "👨‍🏫",
                            primaryText,
                            secondaryText,
                            teacher,
                            score
                    ));
                }
            }
        } catch (Exception e) {
            log.error("Error searching teachers: {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * Calculate relevance score for teacher
     */
    private double calculateTeacherRelevance(Teacher teacher, String query) {
        double score = 0.0;

        String fullName = (teacher.getFirstName() + " " + teacher.getLastName()).toLowerCase();

        if (fullName.equals(query)) {
            score = 1.0;
        } else if (fullName.contains(query)) {
            score = 0.8;
        } else if (teacher.getFirstName() != null && teacher.getFirstName().toLowerCase().contains(query)) {
            score = 0.7;
        } else if (teacher.getLastName() != null && teacher.getLastName().toLowerCase().contains(query)) {
            score = 0.7;
        } else if (teacher.getEmail() != null && teacher.getEmail().toLowerCase().contains(query)) {
            score = 0.6;
        } else if (teacher.getDepartment() != null &&
                   teacher.getDepartment().toLowerCase().contains(query)) {
            score = 0.5;
        }

        return score;
    }

    /**
     * Search courses by code, name, description
     */
    private List<SearchResult> searchCourses(String query) {
        List<SearchResult> results = new ArrayList<>();

        try {
            List<Course> courses = courseRepository.findAll();

            for (Course course : courses) {
                double score = calculateCourseRelevance(course, query);
                if (score > 0) {
                    String primaryText = String.format("%s - %s",
                            course.getCourseCode(),
                            course.getCourseName());

                    String secondaryText = String.format("%s • %d min",
                            course.getSubject() != null ? course.getSubject() : "No subject",
                            course.getDurationMinutes() != null ? course.getDurationMinutes() : 0);

                    results.add(new SearchResult(
                            "COURSE",
                            "📚",
                            primaryText,
                            secondaryText,
                            course,
                            score
                    ));
                }
            }
        } catch (Exception e) {
            log.error("Error searching courses: {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * Calculate relevance score for course
     */
    private double calculateCourseRelevance(Course course, String query) {
        double score = 0.0;

        if (course.getCourseCode() != null && course.getCourseCode().toLowerCase().equals(query)) {
            score = 1.0;
        } else if (course.getCourseCode() != null && course.getCourseCode().toLowerCase().contains(query)) {
            score = 0.9;
        } else if (course.getCourseName() != null && course.getCourseName().toLowerCase().contains(query)) {
            score = 0.8;
        } else if (course.getDescription() != null && course.getDescription().toLowerCase().contains(query)) {
            score = 0.6;
        } else if (course.getSubject() != null && course.getSubject().toLowerCase().contains(query)) {
            score = 0.5;
        }

        return score;
    }

    /**
     * Search rooms by room number, building, capacity
     */
    private List<SearchResult> searchRooms(String query) {
        List<SearchResult> results = new ArrayList<>();

        try {
            List<Room> rooms = roomRepository.findAll();

            for (Room room : rooms) {
                double score = calculateRoomRelevance(room, query);
                if (score > 0) {
                    String primaryText = String.format("Room %s",
                            room.getRoomNumber());

                    String secondaryText = String.format("%s • Capacity: %d • %s",
                            room.getBuilding() != null ? room.getBuilding() : "No building",
                            room.getCapacity() != null ? room.getCapacity() : 0,
                            room.getType() != null ? room.getType().toString() : "Standard");

                    results.add(new SearchResult(
                            "ROOM",
                            "🚪",
                            primaryText,
                            secondaryText,
                            room,
                            score
                    ));
                }
            }
        } catch (Exception e) {
            log.error("Error searching rooms: {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * Calculate relevance score for room
     */
    private double calculateRoomRelevance(Room room, String query) {
        double score = 0.0;

        if (room.getRoomNumber() != null && room.getRoomNumber().toLowerCase().equals(query)) {
            score = 1.0;
        } else if (room.getRoomNumber() != null && room.getRoomNumber().toLowerCase().contains(query)) {
            score = 0.9;
        } else if (room.getBuilding() != null && room.getBuilding().toLowerCase().contains(query)) {
            score = 0.7;
        } else if (room.getType() != null && room.getType().toString().toLowerCase().contains(query)) {
            score = 0.6;
        }

        return score;
    }

    /**
     * Search available actions (quick actions the user can perform)
     */
    private List<SearchResult> searchActions(String query) {
        List<SearchResult> actions = getAvailableActions();

        return actions.stream()
                .filter(action -> action.getPrimaryText().toLowerCase().contains(query) ||
                                  action.getSecondaryText().toLowerCase().contains(query))
                .collect(Collectors.toList());
    }

    /**
     * Get default suggestions when search is empty
     */
    private List<SearchResult> getDefaultSuggestions(String category) {
        List<SearchResult> suggestions = new ArrayList<>();

        if ("ALL".equals(category) || "ACTIONS".equals(category)) {
            suggestions.addAll(getAvailableActions());
        }

        return suggestions;
    }

    /**
     * Get list of available quick actions
     */
    private List<SearchResult> getAvailableActions() {
        List<SearchResult> actions = new ArrayList<>();

        actions.add(new SearchResult("ACTION", "➕", "Add New Student", "Create a new student record", "add_student", 0.95));
        actions.add(new SearchResult("ACTION", "➕", "Add New Teacher", "Create a new teacher record", "add_teacher", 0.95));
        actions.add(new SearchResult("ACTION", "➕", "Add New Course", "Create a new course", "add_course", 0.95));
        actions.add(new SearchResult("ACTION", "➕", "Add New Room", "Create a new classroom", "add_room", 0.95));
        actions.add(new SearchResult("ACTION", "📅", "Generate Master Schedule", "Create academic schedule", "generate_schedule", 0.90));
        actions.add(new SearchResult("ACTION", "📋", "Create IEP", "Start IEP wizard", "create_iep", 0.90));
        actions.add(new SearchResult("ACTION", "🏥", "Medical Records", "Manage student medical records", "medical_records", 0.90));
        actions.add(new SearchResult("ACTION", "📊", "View Dashboard", "Go to main dashboard", "dashboard", 0.85));
        actions.add(new SearchResult("ACTION", "📥", "Import Data", "Import CSV data", "import_data", 0.85));
        actions.add(new SearchResult("ACTION", "📤", "Export Data", "Export data to CSV", "export_data", 0.85));
        actions.add(new SearchResult("ACTION", "⚙️", "Settings", "Open application settings", "settings", 0.80));
        actions.add(new SearchResult("ACTION", "🔄", "Refresh Data", "Reload all data", "refresh", 0.80));

        return actions;
    }
}
