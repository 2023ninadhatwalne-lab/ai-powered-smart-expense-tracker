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
public class BudgetSummaryDto {
    private BigDecimal totalBudgetLimit;
    private BigDecimal totalBudgetSpent;
    private BigDecimal totalBudgetRemaining;
    private Double overallPercentageUsed;
    private String overallStatus;
    private Integer activeBudgetsCount;
    private Integer warningBudgetsCount;
    private Integer exceededBudgetsCount;
    private Integer month;
    private Integer year;
    private List<BudgetResponse> budgets;
}
