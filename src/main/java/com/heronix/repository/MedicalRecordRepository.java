package com.heronix.repository;

import com.heronix.model.domain.MedicalRecord;
import com.heronix.model.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Medical Record Repository
 * Location: src/main/java/com/eduscheduler/repository/MedicalRecordRepository.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 */
@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    /**
     * Find medical record by student
     */
    Optional<MedicalRecord> findByStudent(Student student);

    /**
     * Find medical record by student ID
     */
    @Query("SELECT m FROM MedicalRecord m WHERE m.student.id = :studentId")
    Optional<MedicalRecord> findByStudentId(Long studentId);

    /**
     * Find all students with medical conditions
     * (students who have any allergies, chronic conditions, or medications)
     */
    @Query("SELECT m FROM MedicalRecord m WHERE " +
           "m.foodAllergies IS NOT NULL OR " +
           "m.medicationAllergies IS NOT NULL OR " +
           "m.environmentalAllergies IS NOT NULL OR " +
           "m.hasDiabetes = true OR " +
           "m.hasAsthma = true OR " +
           "m.hasSeizureDisorder = true OR " +
           "m.hasHeartCondition = true OR " +
           "m.otherConditions IS NOT NULL OR " +
           "m.currentMedications IS NOT NULL")
    List<MedicalRecord> findAllWithMedicalConditions();

    /**
     * Find students with severe allergies
     */
    @Query("SELECT m FROM MedicalRecord m WHERE m.allergySeverity IN ('SEVERE', 'LIFE_THREATENING')")
    List<MedicalRecord> findStudentsWithSevereAllergies();

    /**
     * Find students with EpiPens
     */
    List<MedicalRecord> findByHasEpiPenTrue();

    /**
     * Find diabetic students
     */
    List<MedicalRecord> findByHasDiabetesTrue();

    /**
     * Find students with asthma
     */
    List<MedicalRecord> findByHasAsthmaTrue();

    /**
     * Find students with seizure disorders
     */
    List<MedicalRecord> findByHasSeizureDisorderTrue();

    /**
     * Find students requiring medication during school hours
     */
    @Query("SELECT m FROM MedicalRecord m WHERE m.medicationSchedule IS NOT NULL")
    List<MedicalRecord> findStudentsRequiringMedication();

    /**
     * Find medical records needing review
     * (next review date is past or within next 30 days)
     */
    @Query("SELECT m FROM MedicalRecord m WHERE m.nextReviewDate <= :date")
    List<MedicalRecord> findRecordsNeedingReview(LocalDate date);

    /**
     * Find overdue medical reviews
     */
    @Query("SELECT m FROM MedicalRecord m WHERE m.nextReviewDate < CURRENT_DATE")
    List<MedicalRecord> findOverdueReviews();

    /**
     * Find unverified medical records
     */
    List<MedicalRecord> findByNurseVerifiedFalse();

    /**
     * Find records without parent signature
     */
    List<MedicalRecord> findByParentSignatureOnFileFalse();

    /**
     * Count students with medical conditions
     */
    @Query("SELECT COUNT(m) FROM MedicalRecord m WHERE " +
           "m.foodAllergies IS NOT NULL OR " +
           "m.medicationAllergies IS NOT NULL OR " +
           "m.environmentalAllergies IS NOT NULL OR " +
           "m.hasDiabetes = true OR " +
           "m.hasAsthma = true OR " +
           "m.hasSeizureDisorder = true OR " +
           "m.hasHeartCondition = true OR " +
           "m.otherConditions IS NOT NULL OR " +
           "m.currentMedications IS NOT NULL")
    long countStudentsWithMedicalConditions();

    /**
     * Search medical records by keyword in allergies or conditions
     */
    @Query("SELECT m FROM MedicalRecord m WHERE " +
           "LOWER(m.foodAllergies) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.medicationAllergies) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.environmentalAllergies) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.otherConditions) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.currentMedications) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.medicalAlert) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<MedicalRecord> searchByKeyword(String keyword);
}
