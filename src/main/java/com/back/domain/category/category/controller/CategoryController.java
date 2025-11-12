package com.back.domain.category.category.controller;

import com.back.domain.category.category.dto.CategoryResBody;
import com.back.domain.category.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
@Tag(name = "Category API", description = "카테고리 조회 API, 인증없이 접근 가능")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "카테고리 목록 조회 API", description = "카테고리들과 함께 연관된 하위 카테고리들 목록 조회")
    @GetMapping
    public ResponseEntity<List<CategoryResBody>> readCategories() {
        List<CategoryResBody> categoryResBodyList = categoryService.getCategories();
        return ResponseEntity.ok(categoryResBodyList);
    }
}
