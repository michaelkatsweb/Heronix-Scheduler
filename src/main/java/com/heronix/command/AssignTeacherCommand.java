package com.heronix.command;

import com.heronix.model.domain.CourseSection;
import com.heronix.model.domain.Teacher;
import com.heronix.repository.CourseSectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command to assign a teacher to a course section
 *
 * Location: src/main/java/com/eduscheduler/command/AssignTeacherCommand.java
 */
public class AssignTeacherCommand implements Command {

    private static final Logger logger = LoggerFactory.getLogger(AssignTeacherCommand.class);

    private final CourseSectionRepository sectionRepository;
    private final Long sectionId;
    private final Teacher newTeacher;
    private Teacher previousTeacher;

    public AssignTeacherCommand(CourseSectionRepository sectionRepository,
                               Long sectionId,
                               Teacher newTeacher) {
        this.sectionRepository = sectionRepository;
        this.sectionId = sectionId;
        this.newTeacher = newTeacher;
    }

    @Override
    public void execute() {
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));

        previousTeacher = section.getAssignedTeacher();
        section.setAssignedTeacher(newTeacher);
        sectionRepository.save(section);

        logger.debug("Assigned teacher {} to section {}",
                newTeacher != null ? newTeacher.getLastName() : "None", sectionId);
    }

    @Override
    public void undo() {
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));

        section.setAssignedTeacher(previousTeacher);
        sectionRepository.save(section);

        logger.debug("Restored previous teacher {} to section {}",
                previousTeacher != null ? previousTeacher.getLastName() : "None", sectionId);
    }

    @Override
    public String getDescription() {
        String teacherName = newTeacher != null ? newTeacher.getLastName() : "None";
        return "Assign teacher " + teacherName + " to section " + sectionId;
    }

    @Override
    public CommandType getType() {
        return CommandType.TEACHER_ASSIGNMENT;
    }
}
