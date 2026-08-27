package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendingInsightDto {
    private String type;        // WARNING, INFO, SUCCESS, ANOMALY
    private String title;
    private String message;
    private String category;
    private Double percentageChange;
}
