package com.expensetracker;

import com.expensetracker.dto.AiCategorizeRequest;
import com.expensetracker.dto.AiCategorizeResponse;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategoryType;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.service.AiCategorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AiCategorizationServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private AiCategorizationService aiCategorizationService;

    @BeforeEach
    void setUp() {
        when(categoryRepository.findByNameIgnoreCase(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            return Optional.of(Category.builder().id(1L).name(name).type(CategoryType.EXPENSE).build());
        });
    }

    @Test
    void testFoodAndDiningClassification() {
        AiCategorizeRequest request = AiCategorizeRequest.builder()
                .text("Swiggy dinner 450")
                .amount(new BigDecimal("450"))
                .build();

        AiCategorizeResponse response = aiCategorizationService.categorize(request);
        assertEquals("Food & Dining", response.getCategory());
        assertTrue(response.getConfidence() > 0.4);
    }

    @Test
    void testTransportationClassification() {
        AiCategorizeRequest request = AiCategorizeRequest.builder()
                .text("Uber cab ride to office")
                .amount(new BigDecimal("320"))
                .build();

        AiCategorizeResponse response = aiCategorizationService.categorize(request);
        assertEquals("Transportation", response.getCategory());
    }

    @Test
    void testEntertainmentClassification() {
        AiCategorizeRequest request = AiCategorizeRequest.builder()
                .text("Netflix monthly 649")
                .amount(new BigDecimal("649"))
                .build();

        AiCategorizeResponse response = aiCategorizationService.categorize(request);
        assertEquals("Entertainment", response.getCategory());
    }

    @Test
    void testHealthcareClassification() {
        AiCategorizeRequest request = AiCategorizeRequest.builder()
                .text("Apollo pharmacy tablets")
                .amount(new BigDecimal("210"))
                .build();

        AiCategorizeResponse response = aiCategorizationService.categorize(request);
        assertEquals("Healthcare", response.getCategory());
    }
}
