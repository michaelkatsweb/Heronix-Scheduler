package com.heronix.repository;

import com.heronix.model.domain.PortalUser;
import com.heronix.model.domain.PortalUser.PortalUserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PortalUser entities
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 16 - Parent/Student Portal
 */
@Repository
public interface PortalUserRepository extends JpaRepository<PortalUser, Long> {

    Optional<PortalUser> findByUsername(String username);

    Optional<PortalUser> findByEmail(String email);

    Optional<PortalUser> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<PortalUser> findByUserType(PortalUserType userType);

    List<PortalUser> findByActiveTrue();

    @Query("SELECT p FROM PortalUser p JOIN p.linkedStudents s WHERE s.id = :studentId")
    List<PortalUser> findByLinkedStudentId(@Param("studentId") Long studentId);

    Optional<PortalUser> findByPasswordResetToken(String token);

    // TODO: Uncomment when needed for portal user cleanup
    // Date arithmetic in JPQL is database-specific and complex
    // For now, this functionality can be implemented in the service layer
    // @Query("SELECT p FROM PortalUser p WHERE p.emailVerified = false " +
    //        "AND p.createdAt < FUNCTION('DATEADD', 'DAY', -7, CURRENT_TIMESTAMP)")
    // List<PortalUser> findUnverifiedAccountsOlderThan7Days();

    @Query("SELECT COUNT(p) FROM PortalUser p WHERE p.active = true " +
           "AND p.userType = :userType")
    long countActiveByUserType(@Param("userType") PortalUserType userType);
}
