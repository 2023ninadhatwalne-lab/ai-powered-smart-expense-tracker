package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrendDto {
    private String month;       // e.g. "2026-03" or "March 2026"
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal netSavings;
}
