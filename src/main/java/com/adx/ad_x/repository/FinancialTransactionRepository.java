package com.adx.ad_x.repository;

import com.adx.ad_x.model.FinancialTransaction;
import com.adx.ad_x.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {

    // Find transactions by user
    List<FinancialTransaction> findByUserOrderByTransactionDateDesc(User user);

    // Find transactions by type
    List<FinancialTransaction> findByTypeOrderByTransactionDateDesc(String type);

    // Find transactions by date range
    @Query("SELECT ft FROM FinancialTransaction ft WHERE ft.transactionDate BETWEEN :startDate AND :endDate ORDER BY ft.transactionDate DESC")
    List<FinancialTransaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    // Calculate total amount by type and date range
    @Query("SELECT COALESCE(SUM(ft.amount), 0) FROM FinancialTransaction ft WHERE ft.type = :type AND ft.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalByTypeAndDateRange(@Param("type") String type,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);
}