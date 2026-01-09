package com.heronix.repository;

import com.heronix.model.domain.Waitlist;
import com.heronix.model.domain.Student;
import com.heronix.model.domain.Course;
import com.heronix.model.domain.ScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    List<Waitlist> findByStudentAndStatus(Student student, Waitlist.WaitlistStatus status);

    List<Waitlist> findByCourseAndStatusOrderByPositionAsc(Course course, Waitlist.WaitlistStatus status);

    List<Waitlist> findByScheduleSlotAndStatusOrderByPositionAsc(ScheduleSlot slot, Waitlist.WaitlistStatus status);

    Optional<Waitlist> findByStudentAndCourseAndStatus(Student student, Course course, Waitlist.WaitlistStatus status);

    @Query("SELECT w FROM Waitlist w WHERE " +
           "w.scheduleSlot = :slot AND w.status = 'ACTIVE' " +
           "ORDER BY w.priorityWeight DESC, w.position ASC")
    List<Waitlist> findActiveWaitlistForSlot(@Param("slot") ScheduleSlot slot);

    @Query("SELECT COUNT(w) FROM Waitlist w WHERE " +
           "w.course = :course AND w.status = 'ACTIVE'")
    int countActiveWaitlistForCourse(@Param("course") Course course);

    @Query("SELECT w FROM Waitlist w WHERE " +
           "w.status = 'ACTIVE' AND w.notificationSent = false")
    List<Waitlist> findPendingNotifications();
}
