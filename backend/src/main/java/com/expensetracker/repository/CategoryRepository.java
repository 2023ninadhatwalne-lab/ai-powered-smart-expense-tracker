package com.expensetracker.repository;

import com.expensetracker.model.Category;
import com.expensetracker.model.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);
    List<Category> findByType(CategoryType type);
    Boolean existsByNameIgnoreCase(String name);
}
