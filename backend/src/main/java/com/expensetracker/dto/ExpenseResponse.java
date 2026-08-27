package com.expensetracker.dto;

import com.expensetracker.model.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private BigDecimal amount;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private String categoryIcon;
    private LocalDate date;
    private PaymentMethod paymentMethod;
    private String merchant;
    private Boolean isAnomaly;
    private String anomalyReason;
    private Boolean aiCategorized;
    private LocalDateTime createdAt;
}
