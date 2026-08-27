package com.expensetracker.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetResponse {

    private Long id;

    private Long categoryId;

    private String categoryName;

    private String categoryIcon;

    private String categoryColor;

    private BigDecimal monthlyLimit;

    private BigDecimal amountSpent;

    private BigDecimal remainingAmount;

    private double percentageUsed;

    private String status;

    private Integer month;

    private Integer year;
}