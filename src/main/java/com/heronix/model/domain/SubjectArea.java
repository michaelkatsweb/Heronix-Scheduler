package com.heronix.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Subject Area Entity
 *
 * Represents academic subject areas and their hierarchical relationships.
 *
 * Examples:
 * - Mathematics (parent)
 *   ├── Algebra (child)
 *   ├── Geometry (child)
 *   └── Calculus (child)
 *
 * - Science (parent)
 *   ├── Biology (child)
 *   ├── Chemistry (child)
 *   └── Physics (child)
 *
 * Location: src/main/java/com/eduscheduler/model/domain/SubjectArea.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 1 - December 6, 2025 - Subject Area Enhancement
 */
@Entity
@Table(name = "subject_areas", uniqueConstraints = {
    @UniqueConstraint(columnNames = "code")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Subject name (e.g., "Mathematics", "English", "Science")
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Subject code (e.g., "MATH", "ENG", "SCI")
     */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /**
     * Department this subject belongs to
     * (e.g., "Math & Science Department", "Humanities Department")
     */
    @Column(length = 100)
    private String department;

    /**
     * Parent subject area for hierarchical organization
     * Example: "Algebra" has parent "Mathematics"
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_subject_id")
    private SubjectArea parentSubject;

    /**
     * Child subject areas
     * Example: "Mathematics" has children "Algebra", "Geometry", "Calculus"
     */
    @OneToMany(mappedBy = "parentSubject", cascade = CascadeType.ALL)
    @Builder.Default
    private List<SubjectArea> childSubjects = new ArrayList<>();

    /**
     * Detailed description of this subject area
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Is this subject area currently active?
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Icon or color code for UI display (optional)
     * Example: "#3498db" for blue (Mathematics)
     */
    @Column(length = 50)
    private String displayColor;

    /**
     * Courses that belong to this subject area
     */
    @OneToMany(mappedBy = "subjectArea", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Course> courses = new ArrayList<>();

    /**
     * Related subject areas (for recommendation engine)
     */
    @OneToMany(mappedBy = "subject1")
    @Builder.Default
    private List<SubjectRelationship> relatedSubjects = new ArrayList<>();

    /**
     * When this subject area was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this subject area was last modified
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Check if this is a top-level subject (no parent)
     */
    public boolean isTopLevel() {
        return parentSubject == null;
    }

    /**
     * Check if this subject has children
     */
    public boolean hasChildren() {
        return childSubjects != null && !childSubjects.isEmpty();
    }

    /**
     * Get full path (e.g., "Mathematics > Algebra")
     */
    public String getFullPath() {
        if (isTopLevel()) {
            return name;
        }
        return parentSubject.getFullPath() + " > " + name;
    }

    /**
     * Get all courses in this subject and its children (recursive)
     */
    public List<Course> getAllCoursesIncludingChildren() {
        List<Course> allCourses = new ArrayList<>(courses);

        if (hasChildren()) {
            for (SubjectArea child : childSubjects) {
                allCourses.addAll(child.getAllCoursesIncludingChildren());
            }
        }

        return allCourses;
    }

    /**
     * Get hierarchy level (0 = top level, 1 = first child, etc.)
     */
    public int getHierarchyLevel() {
        if (isTopLevel()) {
            return 0;
        }
        return 1 + parentSubject.getHierarchyLevel();
    }

    /**
     * Get display string for UI
     */
    public String getDisplayString() {
        return String.format("%s (%s)", name, code);
    }

    /**
     * Get course count (direct children only, not recursive)
     */
    public int getCourseCount() {
        return courses != null ? courses.size() : 0;
    }

    /**
     * Get total course count including all child subjects (recursive)
     */
    public int getTotalCourseCount() {
        int count = getCourseCount();

        if (hasChildren()) {
            for (SubjectArea child : childSubjects) {
                count += child.getTotalCourseCount();
            }
        }

        return count;
    }

    @Override
    public String toString() {
        return String.format("SubjectArea{id=%d, code='%s', name='%s', parent=%s, courses=%d}",
            id, code, name,
            parentSubject != null ? parentSubject.getCode() : "none",
            getCourseCount());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubjectArea)) return false;
        SubjectArea that = (SubjectArea) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
