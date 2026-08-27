package com.expensetracker.repository;

import com.expensetracker.model.Expense;
import com.expensetracker.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserOrderByDateDescCreatedAtDesc(User user);

    Page<Expense> findByUserOrderByDateDescCreatedAtDesc(User user, Pageable pageable);

    List<Expense> findByUserAndDateBetweenOrderByDateDesc(User user, LocalDate startDate, LocalDate endDate);

    List<Expense> findByUserAndIsAnomalyTrueOrderByDateDesc(User user);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user")
    BigDecimal getTotalExpenseAmountByUser(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user AND e.date BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpenseAmountByUserAndDateBetween(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.user = :user")
    Long countByUser(@Param("user") User user);

    @Query("SELECT e.category.name, SUM(e.amount), e.category.color, e.category.icon " +
           "FROM Expense e WHERE e.user = :user AND e.date BETWEEN :startDate AND :endDate " +
           "GROUP BY e.category.name, e.category.color, e.category.icon " +
           "ORDER BY SUM(e.amount) DESC")
    List<Object[]> getCategoryBreakdownByUserAndDateBetween(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT e.category.name, SUM(e.amount), e.category.color, e.category.icon " +
           "FROM Expense e WHERE e.user = :user " +
           "GROUP BY e.category.name, e.category.color, e.category.icon " +
           "ORDER BY SUM(e.amount) DESC")
    List<Object[]> getAllTimeCategoryBreakdownByUser(@Param("user") User user);

    @Query("SELECT e.amount FROM Expense e WHERE e.user = :user ORDER BY e.date DESC")
    List<BigDecimal> findExpenseAmountsByUser(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user AND e.category.id = :categoryId AND e.date BETWEEN :startDate AND :endDate")
    BigDecimal getSpentAmountByUserAndCategoryIdAndDateBetween(
            @Param("user") User user,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
