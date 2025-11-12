package com.back.domain.category.category.controller;

import com.back.domain.category.category.dto.CategoryCreateReqBody;
import com.back.domain.category.category.dto.CategoryResBody;
import com.back.domain.category.category.dto.CategoryUpdateReqBody;
import com.back.domain.category.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/adm/categories")
@Tag(name = "Category Admin API", description = "카테고리 관리자 API, 관리자 인증 필요")
public class CategoryAdmController {

    private final CategoryService categoryService;

    @Operation(summary = "카테고리 등록 API", description = "parentId = null : 상위 카테고리 등록, parentId가 있다면 해당 Id의 하위 카테고리로 등록")
    @PostMapping
    public ResponseEntity<CategoryResBody> createCategory(@Valid @RequestBody CategoryCreateReqBody categoryCreateReqBody) {
        CategoryResBody categoryResBody = categoryService.createCategory(categoryCreateReqBody);
        return ResponseEntity.ok(categoryResBody);
    }

    @Operation(summary = "카테고리 수정 API", description = "카테고리 이름 수정, 수정된 카테고리와 함께 연관된 하위 카테고리들 응답")
    @PatchMapping("/{id}")
    public ResponseEntity<CategoryResBody> updateCategory(
            @PathVariable("id") Long categoryId,
            @Valid @RequestBody CategoryUpdateReqBody categoryUpdateReqBody) {
        CategoryResBody categoryResBody = categoryService.updateCategory(categoryId, categoryUpdateReqBody);
        return ResponseEntity.ok(categoryResBody);
    }

    @Operation(summary = "카테고리 삭제 API", description = "해당 카테고리와 함께 연관된 하위 카테고리들도 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable("id") Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok().build();
    }
}
