package com.expensetracker.service;

import com.expensetracker.dto.AiAnomalyCheckResponse;
import com.expensetracker.dto.AiCategorizeRequest;
import com.expensetracker.dto.AiCategorizeResponse;
import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.dto.ExpenseResponse;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.User;
import com.expensetracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final AuthService authService;
    private final CategoryService categoryService;
    private final AiCategorizationService aiCategorizationService;
    private final AnomalyDetectionService anomalyDetectionService;

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        User user = authService.getCurrentAuthenticatedUser();

        Category category;
        boolean isAiCategorized = false;
        String merchant = request.getMerchant();

        // Check if category provided by ID or Name, or if autoCategorize is requested
        if (Boolean.TRUE.equals(request.getAutoCategorize()) || (request.getCategoryId() == null && (request.getCategoryName() == null || request.getCategoryName().isBlank()))) {
            AiCategorizeResponse aiResult = aiCategorizationService.categorize(
                    AiCategorizeRequest.builder()
                            .text(request.getDescription())
                            .amount(request.getAmount())
                            .merchant(request.getMerchant())
                            .build()
            );
            category = categoryService.getCategoryEntityById(aiResult.getCategoryId());
            isAiCategorized = true;
            if (merchant == null || merchant.isBlank()) {
                merchant = aiResult.getExtractedMerchant();
            }
        } else if (request.getCategoryId() != null) {
            category = categoryService.getCategoryEntityById(request.getCategoryId());
        } else if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            category = categoryService.getCategoryEntityByName(request.getCategoryName());
        } else {
            category = categoryService.getCategoryEntityByName("Other Expense");
        }

        // Anomaly Detection Check
        AiAnomalyCheckResponse anomalyCheck = anomalyDetectionService.detectAnomaly(
                request.getAmount(),
                category.getName(),
                user
        );

        Expense expense = Expense.builder()
                .user(user)
                .amount(request.getAmount())
                .description(request.getDescription())
                .category(category)
                .date(request.getDate() != null ? request.getDate() : LocalDate.now())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.UPI)
                .merchant(merchant)
                .isAnomaly(Boolean.TRUE.equals(anomalyCheck.getIsAnomaly()))
                .anomalyReason(anomalyCheck.getReason())
                .aiCategorized(isAiCategorized)
                .build();

        Expense saved = expenseRepository.save(expense);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAllExpensesForCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        return expenseRepository.findByUserOrderByDateDescCreatedAtDesc(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpensesForCurrentUserPaginated(Pageable pageable) {
        User user = authService.getCurrentAuthenticatedUser();
        return expenseRepository.findByUserOrderByDateDescCreatedAtDesc(user, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByDateRange(LocalDate startDate, LocalDate endDate) {
        User user = authService.getCurrentAuthenticatedUser();
        return expenseRepository.findByUserAndDateBetweenOrderByDateDesc(user, startDate, endDate).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAnomaliesForCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        return expenseRepository.findByUserAndIsAnomalyTrueOrderByDateDesc(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));

        if (!expense.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not have permission to access this expense");
        }

        return mapToResponse(expense);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));

        if (!expense.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not have permission to update this expense");
        }

        if (request.getAmount() != null) {
            expense.setAmount(request.getAmount());
        }
        if (request.getDescription() != null) {
            expense.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryService.getCategoryEntityById(request.getCategoryId());
            expense.setCategory(category);
        } else if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            Category category = categoryService.getCategoryEntityByName(request.getCategoryName());
            expense.setCategory(category);
        }
        if (request.getDate() != null) {
            expense.setDate(request.getDate());
        }
        if (request.getPaymentMethod() != null) {
            expense.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getMerchant() != null) {
            expense.setMerchant(request.getMerchant());
        }

        // Re-evaluate anomaly on update
        AiAnomalyCheckResponse anomalyCheck = anomalyDetectionService.detectAnomaly(
                expense.getAmount(),
                expense.getCategory().getName(),
                user
        );
        expense.setIsAnomaly(Boolean.TRUE.equals(anomalyCheck.getIsAnomaly()));
        expense.setAnomalyReason(anomalyCheck.getReason());

        Expense updated = expenseRepository.save(expense);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteExpense(Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));

        if (!expense.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not have permission to delete this expense");
        }

        expenseRepository.delete(expense);
    }

    public ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .description(expense.getDescription())
                .categoryId(expense.getCategory() != null ? expense.getCategory().getId() : null)
                .categoryName(expense.getCategory() != null ? expense.getCategory().getName() : "Uncategorized")
                .categoryColor(expense.getCategory() != null ? expense.getCategory().getColor() : "#6C757D")
                .categoryIcon(expense.getCategory() != null ? expense.getCategory().getIcon() : "fa-tag")
                .date(expense.getDate())
                .paymentMethod(expense.getPaymentMethod())
                .merchant(expense.getMerchant())
                .isAnomaly(expense.getIsAnomaly())
                .anomalyReason(expense.getAnomalyReason())
                .aiCategorized(expense.getAiCategorized())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
