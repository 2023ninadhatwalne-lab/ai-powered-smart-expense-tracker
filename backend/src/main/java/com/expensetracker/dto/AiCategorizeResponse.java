package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCategorizeResponse {
    private String category;
    private Long categoryId;
    private Double confidence;
    private String modelName;   // e.g. "TF-IDF + Logistic Regression" or "Intelligent Rule Baseline"
    private String extractedMerchant;
}
