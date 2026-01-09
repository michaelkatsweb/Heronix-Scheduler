package com.heronix.repository;

import com.heronix.model.domain.ClassWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ClassWallet transactions
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-08
 */
@Repository
public interface ClassWalletRepository extends JpaRepository<ClassWallet, Long> {

    /**
     * Find all transactions for a student
     */
    List<ClassWallet> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    /**
     * Find transactions for a student updated after a specific time
     * (for incremental sync)
     */
    List<ClassWallet> findByStudentIdAndUpdatedAtAfter(Long studentId, LocalDateTime since);

    /**
     * Find all transactions of a specific type
     */
    List<ClassWallet> findByTransactionType(ClassWallet.TransactionType transactionType);

    /**
     * Find transactions by category
     */
    List<ClassWallet> findByCategory(String category);

    /**
     * Find pending (unapproved) transactions
     */
    List<ClassWallet> findByApprovedFalseOrderByCreatedAtDesc();

    /**
     * Find most recent transaction for a student (to get current balance)
     */
    ClassWallet findTopByStudentIdOrderByCreatedAtDesc(Long studentId);

    /**
     * Get total earned by student (sum of positive amounts)
     */
    @Query("SELECT COALESCE(SUM(cw.amount), 0) FROM ClassWallet cw " +
           "WHERE cw.student.id = :studentId AND cw.amount > 0")
    BigDecimal getTotalEarnedByStudent(@Param("studentId") Long studentId);

    /**
     * Get total spent by student (sum of negative amounts, absolute value)
     */
    @Query("SELECT COALESCE(SUM(ABS(cw.amount)), 0) FROM ClassWallet cw " +
           "WHERE cw.student.id = :studentId AND cw.amount < 0")
    BigDecimal getTotalSpentByStudent(@Param("studentId") Long studentId);

    /**
     * Get current balance for a student (balance_after of most recent transaction)
     */
    @Query("SELECT cw.balanceAfter FROM ClassWallet cw " +
           "WHERE cw.student.id = :studentId " +
           "ORDER BY cw.createdAt DESC LIMIT 1")
    BigDecimal getCurrentBalance(@Param("studentId") Long studentId);

    /**
     * Count transactions for a student
     */
    Long countByStudentId(Long studentId);

    /**
     * Find all transactions updated after a specific time (for sync)
     */
    List<ClassWallet> findByUpdatedAtAfter(LocalDateTime since);

    /**
     * Find transactions for a teacher
     */
    List<ClassWallet> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    /**
     * Get top earners (students with highest balances)
     */
    @Query("SELECT DISTINCT cw.student.id as studentId, " +
           "       cw.student.firstName as firstName, " +
           "       cw.student.lastName as lastName, " +
           "       (SELECT cw2.balanceAfter FROM ClassWallet cw2 " +
           "        WHERE cw2.student.id = cw.student.id " +
           "        ORDER BY cw2.createdAt DESC LIMIT 1) as balance " +
           "FROM ClassWallet cw " +
           "ORDER BY balance DESC")
    List<Object[]> getTopEarners();
}
