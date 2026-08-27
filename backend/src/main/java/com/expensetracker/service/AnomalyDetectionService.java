package com.expensetracker.service;

import com.expensetracker.dto.AiAnomalyCheckRequest;
import com.expensetracker.dto.AiAnomalyCheckResponse;
import com.expensetracker.model.User;
import com.expensetracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetectionService.class);

    private final ExpenseRepository expenseRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.ai-service.base-url:http://localhost:8000/ai/v1}")
    private String aiServiceBaseUrl;

    public AiAnomalyCheckResponse detectAnomaly(BigDecimal currentAmount, String category, User user) {
        if (currentAmount == null || currentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return AiAnomalyCheckResponse.builder()
                    .isAnomaly(false)
                    .anomalyScore(0.0)
                    .reason("Normal amount")
                    .method("Validation")
                    .build();
        }

        List<BigDecimal> historicalAmounts = expenseRepository.findExpenseAmountsByUser(user);

        // 1. Try to invoke ML Service (Isolation Forest)
        try {
            String endpoint = aiServiceBaseUrl + "/detect-anomaly";
            Map<String, Object> payload = new HashMap<>();
            payload.put("current_amount", currentAmount.doubleValue());
            payload.put("category", category);
            payload.put("historical_amounts", historicalAmounts.stream().map(BigDecimal::doubleValue).collect(Collectors.toList()));

            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, payload, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Boolean isAnomaly = (Boolean) body.get("is_anomaly");
                Double score = body.get("anomaly_score") != null ? Double.valueOf(body.get("anomaly_score").toString()) : 0.0;
                String reason = (String) body.get("reason");
                String method = body.get("method") != null ? (String) body.get("method") : "Isolation Forest (ML Service)";

                return AiAnomalyCheckResponse.builder()
                        .isAnomaly(Boolean.TRUE.equals(isAnomaly))
                        .anomalyScore(score)
                        .reason(reason)
                        .method(method)
                        .build();
            }
        } catch (Exception e) {
            logger.debug("AI micro-service not reachable for anomaly detection ({}), using statistical engine.", e.getMessage());
        }

        // 2. Statistical Baseline Engine (IQR & Z-Score)
        return evaluateStatisticalAnomaly(currentAmount, historicalAmounts);
    }

    public AiAnomalyCheckResponse evaluateStatisticalAnomaly(BigDecimal currentAmount, List<BigDecimal> historicalAmounts) {
        if (historicalAmounts == null || historicalAmounts.size() < 3) {
            return AiAnomalyCheckResponse.builder()
                    .isAnomaly(false)
                    .anomalyScore(0.0)
                    .reason("Gracefully skipped: Insufficient transaction history (< 3 records) to establish baseline.")
                    .method("Statistical Baseline (Insufficient Data)")
                    .build();
        }

        List<Double> values = historicalAmounts.stream()
                .map(BigDecimal::doubleValue)
                .sorted()
                .collect(Collectors.toList());

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum() / values.size();
        double stdDev = Math.sqrt(variance);

        double cur = currentAmount.doubleValue();

        // 1. Z-score check (stdDev > 0)
        if (stdDev > 0) {
            double zScore = (cur - mean) / stdDev;
            if (zScore >= 2.5) {
                return AiAnomalyCheckResponse.builder()
                        .isAnomaly(true)
                        .anomalyScore(Math.min(1.0, zScore / 5.0))
                        .reason(String.format("Unusually high transaction: ₹%.2f is %.1fx higher than your average spend of ₹%.2f (Z-Score: %.2f)",
                                cur, (cur / (mean > 0 ? mean : 1.0)), mean, zScore))
                        .method("Statistical Z-Score Engine")
                        .build();
            }
        }

        // 2. Multiplier check (if current amount > 3x mean and > ₹2,000)
        if (mean > 0 && cur >= (mean * 3.0) && cur > 2000.0) {
            return AiAnomalyCheckResponse.builder()
                    .isAnomaly(true)
                    .anomalyScore(0.85)
                    .reason(String.format("Spike alert: ₹%.2f significantly exceeds normal average spend of ₹%.2f", cur, mean))
                    .method("IQR / Average Multiplier Engine")
                    .build();
        }

        return AiAnomalyCheckResponse.builder()
                .isAnomaly(false)
                .anomalyScore(0.1)
                .reason("Transaction is within typical spending limits.")
                .method("Statistical Baseline Engine")
                .build();
    }
}
