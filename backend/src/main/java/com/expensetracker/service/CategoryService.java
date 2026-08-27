package com.expensetracker.service;

import com.expensetracker.dto.CategoryDto;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategoryType;
import com.expensetracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategoriesByType(CategoryType type) {
        return categoryRepository.findByType(type).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Category getCategoryEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public Category getCategoryEntityByName(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    // Fallback to "Other Expense" or create
                    return categoryRepository.findByNameIgnoreCase("Other Expense")
                            .orElseGet(() -> categoryRepository.findAll().stream().findFirst()
                                    .orElseThrow(() -> new ResourceNotFoundException("No categories configured")));
                });
    }

    @Transactional
    public CategoryDto createCategory(CategoryDto dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new BadRequestException("Category already exists: " + dto.getName());
        }

        Category category = Category.builder()
                .name(dto.getName())
                .type(dto.getType() != null ? dto.getType() : CategoryType.EXPENSE)
                .icon(dto.getIcon() != null ? dto.getIcon() : "fa-tag")
                .color(dto.getColor() != null ? dto.getColor() : "#6C757D")
                .build();

        Category saved = categoryRepository.save(category);
        return mapToDto(saved);
    }

    private CategoryDto mapToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .icon(category.getIcon())
                .color(category.getColor())
                .build();
    }
}
