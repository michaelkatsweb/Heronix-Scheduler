package com.heronix.command;

import com.heronix.model.domain.CourseSection;
import com.heronix.model.domain.Room;
import com.heronix.repository.CourseSectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collection of common schedule modification commands
 *
 * Location: src/main/java/com/eduscheduler/command/ScheduleCommands.java
 */
public class ScheduleCommands {

    /**
     * Command to assign a room to a section
     */
    public static class AssignRoomCommand implements Command {
        private static final Logger logger = LoggerFactory.getLogger(AssignRoomCommand.class);

        private final CourseSectionRepository sectionRepository;
        private final Long sectionId;
        private final Room newRoom;
        private Room previousRoom;

        public AssignRoomCommand(CourseSectionRepository sectionRepository, Long sectionId, Room newRoom) {
            this.sectionRepository = sectionRepository;
            this.sectionId = sectionId;
            this.newRoom = newRoom;
        }

        @Override
        public void execute() {
            CourseSection section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));
            previousRoom = section.getAssignedRoom();
            section.setAssignedRoom(newRoom);
            sectionRepository.save(section);
        }

        @Override
        public void undo() {
            CourseSection section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));
            section.setAssignedRoom(previousRoom);
            sectionRepository.save(section);
        }

        @Override
        public String getDescription() {
            String roomName = newRoom != null ? newRoom.getRoomNumber() : "None";
            return "Assign room " + roomName + " to section " + sectionId;
        }

        @Override
        public CommandType getType() {
            return CommandType.ROOM_ASSIGNMENT;
        }
    }

    /**
     * Command to assign a period to a section
     */
    public static class AssignPeriodCommand implements Command {
        private static final Logger logger = LoggerFactory.getLogger(AssignPeriodCommand.class);

        private final CourseSectionRepository sectionRepository;
        private final Long sectionId;
        private final Integer newPeriod;
        private Integer previousPeriod;

        public AssignPeriodCommand(CourseSectionRepository sectionRepository, Long sectionId, Integer newPeriod) {
            this.sectionRepository = sectionRepository;
            this.sectionId = sectionId;
            this.newPeriod = newPeriod;
        }

        @Override
        public void execute() {
            CourseSection section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));
            previousPeriod = section.getAssignedPeriod();
            section.setAssignedPeriod(newPeriod);
            sectionRepository.save(section);
        }

        @Override
        public void undo() {
            CourseSection section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));
            section.setAssignedPeriod(previousPeriod);
            sectionRepository.save(section);
        }

        @Override
        public String getDescription() {
            return "Assign period " + newPeriod + " to section " + sectionId;
        }

        @Override
        public CommandType getType() {
            return CommandType.PERIOD_ASSIGNMENT;
        }
    }

    /**
     * Command to update section capacity
     */
    public static class UpdateSectionCapacityCommand implements Command {
        private static final Logger logger = LoggerFactory.getLogger(UpdateSectionCapacityCommand.class);

        private final CourseSectionRepository sectionRepository;
        private final Long sectionId;
        private final Integer newCapacity;
        private Integer previousCapacity;

        public UpdateSectionCapacityCommand(CourseSectionRepository sectionRepository, Long sectionId, Integer newCapacity) {
            this.sectionRepository = sectionRepository;
            this.sectionId = sectionId;
            this.newCapacity = newCapacity;
        }

        @Override
        public void execute() {
            CourseSection section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));
            previousCapacity = section.getMaxEnrollment();
            section.setMaxEnrollment(newCapacity);
            sectionRepository.save(section);
        }

        @Override
        public void undo() {
            CourseSection section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));
            section.setMaxEnrollment(previousCapacity);
            sectionRepository.save(section);
        }

        @Override
        public String getDescription() {
            return "Update section " + sectionId + " capacity to " + newCapacity;
        }

        @Override
        public CommandType getType() {
            return CommandType.SECTION_ASSIGNMENT;
        }
    }

    /**
     * Composite command that executes multiple commands as a single unit
     */
    public static class CompositeCommand implements Command {
        private static final Logger logger = LoggerFactory.getLogger(CompositeCommand.class);

        private final java.util.List<Command> commands;
        private final String description;

        public CompositeCommand(String description, java.util.List<Command> commands) {
            this.description = description;
            this.commands = new java.util.ArrayList<>(commands);
        }

        @Override
        public void execute() {
            for (Command command : commands) {
                command.execute();
            }
        }

        @Override
        public void undo() {
            // Undo in reverse order
            for (int i = commands.size() - 1; i >= 0; i--) {
                commands.get(i).undo();
            }
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public CommandType getType() {
            return CommandType.BULK_OPERATION;
        }
    }
}
