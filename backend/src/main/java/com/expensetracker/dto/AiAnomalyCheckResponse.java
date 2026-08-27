package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnomalyCheckResponse {
    private Boolean isAnomaly;
    private Double anomalyScore;
    private String reason;
    private String method;      // "Isolation Forest" or "IQR / Statistical Z-Score Fallback"
}
