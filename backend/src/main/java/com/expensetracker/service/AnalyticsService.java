package com.expensetracker.service;

import com.expensetracker.dto.*;
import com.expensetracker.model.Expense;
import com.expensetracker.model.User;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final AuthService authService;
    private final ExpenseService expenseService;
    private final BudgetService budgetService;

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getDashboardSummary(LocalDate startDate, LocalDate endDate) {
        User user = authService.getCurrentAuthenticatedUser();

        LocalDate now = LocalDate.now();
        LocalDate start = (startDate != null) ? startDate : now.withDayOfMonth(1);
        LocalDate end = (endDate != null) ? endDate : now.withDayOfMonth(now.lengthOfMonth());

        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();
        List<BudgetResponse> categoryBudgets = budgetService.getBudgetsForUser(currentMonth, currentYear);

        // Aggregate Totals for the specified range (or current month)
        BigDecimal totalIncome = incomeRepository.getTotalIncomeAmountByUserAndDateBetween(user, start, end);
        BigDecimal totalExpense = expenseRepository.getTotalExpenseAmountByUserAndDateBetween(user, start, end);
        BigDecimal currentSavings = totalIncome.subtract(totalExpense);

        Double savingsRate = 0.0;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = currentSavings.divide(totalIncome, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
        }

        Long expenseCount = expenseRepository.countByUser(user);
        List<Expense> anomalies = expenseRepository.findByUserAndIsAnomalyTrueOrderByDateDesc(user);
        Long anomalyCount = (long) anomalies.size();

        // Monthly Budget Calculation
        BigDecimal monthlyBudget = user.getMonthlyBudget() != null ? user.getMonthlyBudget() : BigDecimal.ZERO;
        Double budgetUsedPercentage = 0.0;
        if (monthlyBudget.compareTo(BigDecimal.ZERO) > 0) {
            budgetUsedPercentage = totalExpense.divide(monthlyBudget, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
        }

        // Category Breakdown
        List<CategoryBreakdownDto> categoryBreakdown = getCategoryBreakdown(user, start, end, totalExpense);

        // Monthly Trends (Last 6 Months)
        List<MonthlyTrendDto> monthlyTrends = getMonthlyTrends(user, 6);

        // AI Insights
        List<SpendingInsightDto> aiInsights = generateInsights(user, totalIncome, totalExpense, monthlyBudget, categoryBreakdown, anomalies, categoryBudgets);

        // Recent Transactions (top 8)
        List<ExpenseResponse> recentExpenses = expenseRepository.findByUserOrderByDateDescCreatedAtDesc(user)
                .stream().limit(8).map(expenseService::mapToResponse).collect(Collectors.toList());

        // Unusual Transactions
        List<ExpenseResponse> unusualTransactions = anomalies.stream()
                .limit(5).map(expenseService::mapToResponse).collect(Collectors.toList());

        return AnalyticsSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .currentSavings(currentSavings)
                .savingsRate(Math.round(savingsRate * 10.0) / 10.0)
                .expenseCount(expenseCount)
                .incomeCount((long) incomeRepository.findByUserOrderByDateDescCreatedAtDesc(user).size())
                .anomalyCount(anomalyCount)
                .monthlyBudget(monthlyBudget)
                .budgetUsedPercentage(Math.round(budgetUsedPercentage * 10.0) / 10.0)
                .categoryBreakdown(categoryBreakdown)
                .monthlyTrends(monthlyTrends)
                .aiInsights(aiInsights)
                .recentTransactions(recentExpenses)
                .unusualTransactions(unusualTransactions)
                .categoryBudgets(categoryBudgets)
                .build();
    }

    private List<CategoryBreakdownDto> getCategoryBreakdown(User user, LocalDate start, LocalDate end, BigDecimal totalExpense) {
        List<Object[]> rawList = expenseRepository.getCategoryBreakdownByUserAndDateBetween(user, start, end);
        if (rawList.isEmpty()) {
            rawList = expenseRepository.getAllTimeCategoryBreakdownByUser(user);
        }

        List<CategoryBreakdownDto> result = new ArrayList<>();
        BigDecimal sum = totalExpense.compareTo(BigDecimal.ZERO) > 0 ? totalExpense :
                rawList.stream().map(r -> (BigDecimal) r[1]).reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Object[] row : rawList) {
            String name = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            String color = (String) row[2];
            String icon = (String) row[3];

            double percentage = 0.0;
            if (sum.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.divide(sum, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            }

            result.add(CategoryBreakdownDto.builder()
                    .categoryName(name)
                    .amount(amount)
                    .percentage(Math.round(percentage * 10.0) / 10.0)
                    .color(color != null ? color : "#36A2EB")
                    .icon(icon != null ? icon : "fa-tag")
                    .build());
        }

        return result;
    }

    private List<MonthlyTrendDto> getMonthlyTrends(User user, int monthsBack) {
        List<MonthlyTrendDto> trends = new ArrayList<>();
        YearMonth currentYearMonth = YearMonth.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = monthsBack - 1; i >= 0; i--) {
            YearMonth ym = currentYearMonth.minusMonths(i);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            BigDecimal inc = incomeRepository.getTotalIncomeAmountByUserAndDateBetween(user, start, end);
            BigDecimal exp = expenseRepository.getTotalExpenseAmountByUserAndDateBetween(user, start, end);
            BigDecimal net = inc.subtract(exp);

            trends.add(MonthlyTrendDto.builder()
                    .month(ym.format(formatter))
                    .income(inc)
                    .expense(exp)
                    .netSavings(net)
                    .build());
        }

        return trends;
    }

    private List<SpendingInsightDto> generateInsights(User user,
                                                      BigDecimal totalIncome,
                                                      BigDecimal totalExpense,
                                                      BigDecimal monthlyBudget,
                                                      List<CategoryBreakdownDto> categoryBreakdown,
                                                      List<Expense> anomalies,
                                                      List<BudgetResponse> categoryBudgets) {
        List<SpendingInsightDto> insights = new ArrayList<>();

        // 1. Category Budget Status Alerts (< 80% NORMAL, 80-99% WARNING, >= 100% EXCEEDED)
        if (categoryBudgets != null) {
            for (BudgetResponse b : categoryBudgets) {
                if ("EXCEEDED".equalsIgnoreCase(b.getStatus())) {
                    insights.add(SpendingInsightDto.builder()
                            .type("WARNING")
                            .title(String.format("Budget Exceeded: %s", b.getCategoryName()))
                            .message(String.format("You have exceeded your %s budget of ₹%.2f (Spent ₹%.2f, %.1f%% used).",
                                    b.getCategoryName(), b.getMonthlyLimit(), b.getAmountSpent(), b.getPercentageUsed()))
                            .category(b.getCategoryName())
                            .percentageChange(b.getPercentageUsed())
                            .build());
                } else if ("WARNING".equalsIgnoreCase(b.getStatus())) {
                    insights.add(SpendingInsightDto.builder()
                            .type("WARNING")
                            .title(String.format("Budget Warning: %s", b.getCategoryName()))
                            .message(String.format("You are near your %s budget limit: ₹%.2f spent of ₹%.2f (%.1f%% used).",
                                    b.getCategoryName(), b.getAmountSpent(), b.getMonthlyLimit(), b.getPercentageUsed()))
                            .category(b.getCategoryName())
                            .percentageChange(b.getPercentageUsed())
                            .build());
                }
            }
        }

        // 1. Month-over-Month Category Trend
        YearMonth currentYm = YearMonth.now();
        YearMonth prevYm = currentYm.minusMonths(1);

        LocalDate currentStart = currentYm.atDay(1);
        LocalDate currentEnd = currentYm.atEndOfMonth();
        LocalDate prevStart = prevYm.atDay(1);
        LocalDate prevEnd = prevYm.atEndOfMonth();

        List<Object[]> prevBreakdown = expenseRepository.getCategoryBreakdownByUserAndDateBetween(user, prevStart, prevEnd);
        Map<String, BigDecimal> prevCatMap = new HashMap<>();
        for (Object[] r : prevBreakdown) {
            prevCatMap.put((String) r[0], (BigDecimal) r[1]);
        }

        for (CategoryBreakdownDto curCat : categoryBreakdown) {
            BigDecimal prevAmount = prevCatMap.getOrDefault(curCat.getCategoryName(), BigDecimal.ZERO);
            if (prevAmount.compareTo(BigDecimal.ZERO) > 0 && curCat.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal diff = curCat.getAmount().subtract(prevAmount);
                double pct = diff.divide(prevAmount, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
                double roundedPct = Math.round(Math.abs(pct) * 10.0) / 10.0;

                if (pct > 15.0) {
                    insights.add(SpendingInsightDto.builder()
                            .type("WARNING")
                            .title("Spending Spike Detected")
                            .message(String.format("%s spending increased by %.1f%% compared with last month (₹%.2f vs ₹%.2f).",
                                    curCat.getCategoryName(), roundedPct, curCat.getAmount(), prevAmount))
                            .category(curCat.getCategoryName())
                            .percentageChange(roundedPct)
                            .build());
                } else if (pct < -15.0) {
                    insights.add(SpendingInsightDto.builder()
                            .type("SUCCESS")
                            .title("Spending Reduction")
                            .message(String.format("Great progress! %s spending decreased by %.1f%% compared with last month.",
                                    curCat.getCategoryName(), roundedPct))
                            .category(curCat.getCategoryName())
                            .percentageChange(-roundedPct)
                            .build());
                }
            }
        }

        // 2. Budget Alert
        if (monthlyBudget.compareTo(BigDecimal.ZERO) > 0) {
            double budgetPct = totalExpense.divide(monthlyBudget, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            if (budgetPct >= 100.0) {
                insights.add(SpendingInsightDto.builder()
                        .type("WARNING")
                        .title("Budget Exceeded")
                        .message(String.format("You have exceeded your monthly budget of ₹%.2f by ₹%.2f (%.1f%% used).",
                                monthlyBudget, totalExpense.subtract(monthlyBudget), budgetPct))
                        .percentageChange(budgetPct)
                        .build());
            } else if (budgetPct >= 80.0) {
                insights.add(SpendingInsightDto.builder()
                        .type("WARNING")
                        .title("Budget Warning")
                        .message(String.format("You have utilized %.1f%% of your monthly budget (₹%.2f spent of ₹%.2f).",
                                budgetPct, totalExpense, monthlyBudget))
                        .percentageChange(budgetPct)
                        .build());
            }
        }

        // 3. Top Expense Category Insight
        if (!categoryBreakdown.isEmpty()) {
            CategoryBreakdownDto topCategory = categoryBreakdown.get(0);
            if (topCategory.getPercentage() >= 35.0) {
                insights.add(SpendingInsightDto.builder()
                        .type("INFO")
                        .title("Dominant Expense Category")
                        .message(String.format("%s accounts for %.1f%% (₹%.2f) of your total expenditure.",
                                topCategory.getCategoryName(), topCategory.getPercentage(), topCategory.getAmount()))
                        .category(topCategory.getCategoryName())
                        .build());
            }
        }

        // 4. Anomaly Alert
        if (!anomalies.isEmpty()) {
            insights.add(SpendingInsightDto.builder()
                    .type("ANOMALY")
                    .title("Unusual Activity Flagged")
                    .message(String.format("AI detected %d transaction(s) significantly deviating from your regular spending habits.", anomalies.size()))
                    .build());
        }

        // 5. Default General Tip if insights list is short
        if (insights.size() < 2) {
            insights.add(SpendingInsightDto.builder()
                    .type("INFO")
                    .title("Smart Savings Rule (50/30/20)")
                    .message("Aim to allocate 50% of your income to needs, 30% to wants, and at least 20% to savings and investments.")
                    .build());
        }

        return insights;
    }
}
