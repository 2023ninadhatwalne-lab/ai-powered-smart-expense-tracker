package com.expensetracker.service;

import com.expensetracker.dto.IncomeRequest;
import com.expensetracker.dto.IncomeResponse;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.model.Category;
import com.expensetracker.model.Income;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.User;
import com.expensetracker.repository.IncomeRepository;
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
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final AuthService authService;
    private final CategoryService categoryService;

    @Transactional
    public IncomeResponse createIncome(IncomeRequest request) {
        User user = authService.getCurrentAuthenticatedUser();

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryService.getCategoryEntityById(request.getCategoryId());
        } else if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            category = categoryService.getCategoryEntityByName(request.getCategoryName());
        } else {
            category = categoryService.getCategoryEntityByName("Other Income");
        }

        Income income = Income.builder()
                .user(user)
                .amount(request.getAmount())
                .source(request.getSource())
                .category(category)
                .date(request.getDate() != null ? request.getDate() : LocalDate.now())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.UPI)
                .build();

        Income saved = incomeRepository.save(income);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IncomeResponse> getAllIncomesForCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        return incomeRepository.findByUserOrderByDateDescCreatedAtDesc(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<IncomeResponse> getIncomesForCurrentUserPaginated(Pageable pageable) {
        User user = authService.getCurrentAuthenticatedUser();
        return incomeRepository.findByUserOrderByDateDescCreatedAtDesc(user, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public IncomeResponse getIncomeById(Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found with ID: " + id));

        if (!income.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not have permission to access this income");
        }

        return mapToResponse(income);
    }

    @Transactional
    public IncomeResponse updateIncome(Long id, IncomeRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found with ID: " + id));

        if (!income.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not have permission to update this income");
        }

        if (request.getAmount() != null) {
            income.setAmount(request.getAmount());
        }
        if (request.getSource() != null) {
            income.setSource(request.getSource());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryService.getCategoryEntityById(request.getCategoryId());
            income.setCategory(category);
        } else if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            Category category = categoryService.getCategoryEntityByName(request.getCategoryName());
            income.setCategory(category);
        }
        if (request.getDate() != null) {
            income.setDate(request.getDate());
        }
        if (request.getPaymentMethod() != null) {
            income.setPaymentMethod(request.getPaymentMethod());
        }

        Income updated = incomeRepository.save(income);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteIncome(Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found with ID: " + id));

        if (!income.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not have permission to delete this income");
        }

        incomeRepository.delete(income);
    }

    public IncomeResponse mapToResponse(Income income) {
        return IncomeResponse.builder()
                .id(income.getId())
                .amount(income.getAmount())
                .source(income.getSource())
                .categoryId(income.getCategory() != null ? income.getCategory().getId() : null)
                .categoryName(income.getCategory() != null ? income.getCategory().getName() : "Uncategorized")
                .categoryColor(income.getCategory() != null ? income.getCategory().getColor() : "#2ECC71")
                .categoryIcon(income.getCategory() != null ? income.getCategory().getIcon() : "fa-money-bill")
                .date(income.getDate())
                .paymentMethod(income.getPaymentMethod())
                .createdAt(income.getCreatedAt())
                .build();
    }
}
