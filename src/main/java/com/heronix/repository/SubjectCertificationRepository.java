package com.heronix.repository;

import com.heronix.model.domain.SubjectCertification;
import com.heronix.model.domain.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Subject Certification Repository
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-06
 */
@Repository
public interface SubjectCertificationRepository extends JpaRepository<SubjectCertification, Long> {

    /**
     * Find all certifications for a specific teacher
     */
    List<SubjectCertification> findByTeacher(Teacher teacher);

    /**
     * Find all certifications for a teacher ID
     */
    List<SubjectCertification> findByTeacherId(Long teacherId);

    /**
     * Find all active certifications for a teacher
     */
    List<SubjectCertification> findByTeacherIdAndActiveTrue(Long teacherId);

    /**
     * Find all certifications for a specific subject
     */
    List<SubjectCertification> findBySubject(String subject);

    /**
     * Find all active certifications for a subject
     */
    List<SubjectCertification> findBySubjectAndActiveTrue(String subject);

    /**
     * Find certifications expiring before a certain date
     */
    @Query("SELECT sc FROM SubjectCertification sc WHERE sc.expirationDate <= :date AND sc.active = true")
    List<SubjectCertification> findExpiringBefore(@Param("date") LocalDate date);

    /**
     * Find certifications expiring soon (within 90 days)
     */
    @Query("SELECT sc FROM SubjectCertification sc WHERE sc.expirationDate BETWEEN :now AND :ninetyDaysFromNow AND sc.active = true")
    List<SubjectCertification> findExpiringSoon(
            @Param("now") LocalDate now,
            @Param("ninetyDaysFromNow") LocalDate ninetyDaysFromNow);

    /**
     * Find expired certifications
     */
    @Query("SELECT sc FROM SubjectCertification sc WHERE sc.expirationDate < :now AND sc.active = true")
    List<SubjectCertification> findExpired(@Param("now") LocalDate now);

    /**
     * Find all valid (active and not expired) certifications
     */
    @Query("SELECT sc FROM SubjectCertification sc WHERE sc.active = true AND (sc.expirationDate IS NULL OR sc.expirationDate >= :now)")
    List<SubjectCertification> findAllValid(@Param("now") LocalDate now);

    /**
     * Count valid certifications for a teacher
     */
    @Query("SELECT COUNT(sc) FROM SubjectCertification sc WHERE sc.teacher.id = :teacherId AND sc.active = true AND (sc.expirationDate IS NULL OR sc.expirationDate >= :now)")
    long countValidByTeacherId(@Param("teacherId") Long teacherId, @Param("now") LocalDate now);

    /**
     * Find teachers certified for a specific subject
     */
    @Query("SELECT DISTINCT sc.teacher FROM SubjectCertification sc WHERE sc.subject = :subject AND sc.active = true AND (sc.expirationDate IS NULL OR sc.expirationDate >= :now)")
    List<Teacher> findTeachersCertifiedForSubject(@Param("subject") String subject, @Param("now") LocalDate now);
}
