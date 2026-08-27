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
public class AiAnomalyCheckRequest {
    private BigDecimal currentAmount;
    private String category;
    private List<BigDecimal> historicalAmounts;
}
