package com.heronix.model.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Historical record of student course enrollments.
 *
 * Archives student enrollments when progressing to next academic year,
 * allowing historical tracking and reporting.
 *
 * @author Heronix Scheduler Team
 */
@Entity
@Table(name = "student_enrollment_history")
public class StudentEnrollmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(nullable = false)
    private Integer gradeLevel;  // Student's grade when enrolled

    @Column(nullable = false)
    private LocalDate enrolledDate;

    @Column(length = 20)
    private String status;  // COMPLETED, DROPPED, IN_PROGRESS, etc.

    @Column(length = 5)
    private String finalGrade;  // A, B, C, D, F, etc. (optional)

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Constructors

    public StudentEnrollmentHistory() {
    }

    public StudentEnrollmentHistory(Student student, Course course, AcademicYear academicYear, Integer gradeLevel) {
        this.student = student;
        this.course = course;
        this.academicYear = academicYear;
        this.gradeLevel = gradeLevel;
        this.enrolledDate = LocalDate.now();
        this.status = "COMPLETED";
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(AcademicYear academicYear) {
        this.academicYear = academicYear;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public LocalDate getEnrolledDate() {
        return enrolledDate;
    }

    public void setEnrolledDate(LocalDate enrolledDate) {
        this.enrolledDate = enrolledDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFinalGrade() {
        return finalGrade;
    }

    public void setFinalGrade(String finalGrade) {
        this.finalGrade = finalGrade;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "EnrollmentHistory{" +
                "student=" + (student != null ? student.getStudentId() : "null") +
                ", course=" + (course != null ? course.getCourseCode() : "null") +
                ", year=" + (academicYear != null ? academicYear.getYearName() : "null") +
                ", grade=" + gradeLevel +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentEnrollmentHistory that = (StudentEnrollmentHistory) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
