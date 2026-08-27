package com.expensetracker.service;

import com.expensetracker.dto.BudgetRequest;
import com.expensetracker.dto.BudgetResponse;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.User;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final AuthService authService;
    private final CategoryService categoryService;

    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {

        User user = authService.getCurrentAuthenticatedUser();

        int month = request.getMonth() != null
                ? request.getMonth()
                : LocalDate.now().getMonthValue();

        int year = request.getYear() != null
                ? request.getYear()
                : LocalDate.now().getYear();

        Category category = categoryService.getCategoryEntityById(request.getCategoryId());

        if (budgetRepository
                .findByUserAndCategoryIdAndMonthAndYear(
                        user,
                        category.getId(),
                        month,
                        year)
                .isPresent()) {

            throw new BadRequestException(
                    "Budget already exists for this category and month");
        }

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .monthlyLimit(request.getMonthlyLimit())
                .month(month)
                .year(year)
                .build();

        return buildResponse(budgetRepository.save(budget));
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getCurrentBudgets() {

        User user = authService.getCurrentAuthenticatedUser();

        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        return budgetRepository
                .findByUserAndMonthAndYear(user, month, year)
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    @Transactional
    public BudgetResponse updateBudget(Long id, BudgetRequest request) {

        User user = authService.getCurrentAuthenticatedUser();

        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Budget not found with ID: " + id));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new BadRequestException(
                    "You do not have permission to update this budget");
        }

        if (request.getMonthlyLimit() != null) {
            budget.setMonthlyLimit(request.getMonthlyLimit());
        }

        return buildResponse(budgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(Long id) {

        User user = authService.getCurrentAuthenticatedUser();

        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Budget not found with ID: " + id));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new BadRequestException(
                    "You do not have permission to delete this budget");
        }

        budgetRepository.delete(budget);
    }

    private BudgetResponse buildResponse(Budget budget) {

        BigDecimal spent = calculateSpent(
                budget.getUser(),
                budget.getCategory().getId(),
                budget.getMonth(),
                budget.getYear());

        BigDecimal remaining = budget.getMonthlyLimit().subtract(spent);

        double percentage = spent.divide(
                budget.getMonthlyLimit(),
                4,
                RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        String status;

        if (percentage >= 100) {
            status = "EXCEEDED";
        } else if (percentage >= 80) {
            status = "WARNING";
        } else {
            status = "NORMAL";
        }

        return BudgetResponse.builder()
                .id(budget.getId())
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getName())
                .categoryIcon(budget.getCategory().getIcon())
                .categoryColor(budget.getCategory().getColor())
                .monthlyLimit(budget.getMonthlyLimit())
                .amountSpent(spent)
                .remainingAmount(remaining)
                .percentageUsed(percentage)
                .status(status)
                .month(budget.getMonth())
                .year(budget.getYear())
                .build();
    }

    private BigDecimal calculateSpent(
            User user,
            Long categoryId,
            int month,
            int year) {

        LocalDate start = LocalDate.of(year, month, 1);

        LocalDate end = start.withDayOfMonth(
                start.lengthOfMonth());

        return expenseRepository
                .findByUserAndDateBetweenOrderByDateDesc(
                        user,
                        start,
                        end)
                .stream()
                .filter(expense -> expense.getCategory() != null
                        && expense.getCategory().getId().equals(categoryId))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsForUser(int month, int year) {

        User user = authService.getCurrentAuthenticatedUser();

        return budgetRepository.findByUserAndMonthAndYear(
                user,
                month,
                year)
                .stream()
                .map(this::buildResponse)
                .toList();
    }
}