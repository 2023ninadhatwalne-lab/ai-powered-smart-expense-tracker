package com.expensetracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCategorizeRequest {
    @NotBlank(message = "Text is required")
    private String text;
    private BigDecimal amount;
    private String merchant;
}
