package com.expensetracker.controller;

import com.expensetracker.dto.ApiResponse;
import com.expensetracker.dto.IncomeRequest;
import com.expensetracker.dto.IncomeResponse;
import com.expensetracker.service.IncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
public class IncomeController {

    private final IncomeService incomeService;

    @PostMapping
    public ResponseEntity<ApiResponse<IncomeResponse>> createIncome(@Valid @RequestBody IncomeRequest request) {
        IncomeResponse response = incomeService.createIncome(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Income logged successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<IncomeResponse>>> getAllIncomes() {
        List<IncomeResponse> incomes = incomeService.getAllIncomesForCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(incomes, "Incomes retrieved successfully"));
    }

    @GetMapping("/paginated")
    public ResponseEntity<ApiResponse<Page<IncomeResponse>>> getIncomesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<IncomeResponse> pagedIncomes = incomeService.getIncomesForCurrentUserPaginated(pageable);
        return ResponseEntity.ok(ApiResponse.success(pagedIncomes, "Paginated incomes retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IncomeResponse>> getIncomeById(@PathVariable Long id) {
        IncomeResponse income = incomeService.getIncomeById(id);
        return ResponseEntity.ok(ApiResponse.success(income, "Income retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<IncomeResponse>> updateIncome(
            @PathVariable Long id,
            @Valid @RequestBody IncomeRequest request) {
        IncomeResponse updated = incomeService.updateIncome(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Income updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteIncome(@PathVariable Long id) {
        incomeService.deleteIncome(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Income deleted successfully"));
    }
}
