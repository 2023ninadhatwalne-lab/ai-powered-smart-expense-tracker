package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal currentSavings;
    private Double savingsRate;
    private Long expenseCount;
    private Long incomeCount;
    private Long anomalyCount;
    private BigDecimal monthlyBudget;
    private Double budgetUsedPercentage;

    private List<CategoryBreakdownDto> categoryBreakdown;
    private List<MonthlyTrendDto> monthlyTrends;
    private List<SpendingInsightDto> aiInsights;
    private List<ExpenseResponse> recentTransactions;
    private List<ExpenseResponse> unusualTransactions;
    private List<BudgetResponse> categoryBudgets;
}
