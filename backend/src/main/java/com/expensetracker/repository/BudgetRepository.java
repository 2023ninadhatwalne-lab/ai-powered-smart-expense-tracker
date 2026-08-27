package com.expensetracker.repository;

import com.expensetracker.model.Budget;
import com.expensetracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserAndMonthAndYear(
            User user,
            Integer month,
            Integer year);

    Optional<Budget> findByUserAndCategoryIdAndMonthAndYear(
            User user,
            Long categoryId,
            Integer month,
            Integer year);
}