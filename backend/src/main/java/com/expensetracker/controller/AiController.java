package com.expensetracker.controller;

import com.expensetracker.dto.AiAnomalyCheckResponse;
import com.expensetracker.dto.AiCategorizeRequest;
import com.expensetracker.dto.AiCategorizeResponse;
import com.expensetracker.dto.ApiResponse;
import com.expensetracker.model.User;
import com.expensetracker.service.AiCategorizationService;
import com.expensetracker.service.AnomalyDetectionService;
import com.expensetracker.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiCategorizationService aiCategorizationService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final AuthService authService;

    @PostMapping("/categorize")
    public ResponseEntity<ApiResponse<AiCategorizeResponse>> categorizeTransaction(
            @Valid @RequestBody AiCategorizeRequest request) {
        AiCategorizeResponse response = aiCategorizationService.categorize(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction categorized by AI"));
    }

    @PostMapping("/check-anomaly")
    public ResponseEntity<ApiResponse<AiAnomalyCheckResponse>> checkAnomaly(
            @RequestBody Map<String, Object> payload) {
        User user = authService.getCurrentAuthenticatedUser();
        BigDecimal amount = BigDecimal.valueOf(Double.parseDouble(payload.get("amount").toString()));
        String category = payload.get("category") != null ? payload.get("category").toString() : "General";

        AiAnomalyCheckResponse response = anomalyDetectionService.detectAnomaly(amount, category, user);
        return ResponseEntity.ok(ApiResponse.success(response, "Anomaly check completed"));
    }
}
