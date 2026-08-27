package com.expensetracker;

import com.expensetracker.dto.AiAnomalyCheckResponse;
import com.expensetracker.model.User;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.service.AnomalyDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnomalyDetectionServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private AnomalyDetectionService anomalyDetectionService;

    @Test
    void testInsufficientDataGracefulHandling() {
        User user = User.builder().id(1L).username("testuser").build();
        when(expenseRepository.findExpenseAmountsByUser(any(User.class)))
                .thenReturn(Collections.singletonList(new BigDecimal("500")));

        AiAnomalyCheckResponse response = anomalyDetectionService.detectAnomaly(
                new BigDecimal("2500"), "Food & Dining", user
        );

        assertFalse(response.getIsAnomaly());
        assertTrue(response.getReason().contains("Insufficient"));
    }

    @Test
    void testNormalSpendingPattern() {
        User user = User.builder().id(1L).username("testuser").build();
        List<BigDecimal> history = Arrays.asList(
                new BigDecimal("450"),
                new BigDecimal("500"),
                new BigDecimal("480"),
                new BigDecimal("520"),
                new BigDecimal("490")
        );
        when(expenseRepository.findExpenseAmountsByUser(any(User.class))).thenReturn(history);

        AiAnomalyCheckResponse response = anomalyDetectionService.detectAnomaly(
                new BigDecimal("510"), "Food & Dining", user
        );

        assertFalse(response.getIsAnomaly());
    }

    @Test
    void testExtremeSpendingAnomalyFlagged() {
        User user = User.builder().id(1L).username("testuser").build();
        List<BigDecimal> history = Arrays.asList(
                new BigDecimal("450"),
                new BigDecimal("500"),
                new BigDecimal("480"),
                new BigDecimal("520"),
                new BigDecimal("490")
        );
        when(expenseRepository.findExpenseAmountsByUser(any(User.class))).thenReturn(history);

        AiAnomalyCheckResponse response = anomalyDetectionService.detectAnomaly(
                new BigDecimal("25000"), "Shopping", user
        );

        assertTrue(response.getIsAnomaly());
        assertTrue(response.getReason().contains("Unusually high transaction") || response.getReason().contains("Spike alert"));
    }
}
