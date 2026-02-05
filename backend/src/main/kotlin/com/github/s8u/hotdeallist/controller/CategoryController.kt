package com.github.s8u.hotdeallist.controller

import com.github.s8u.hotdeallist.dto.response.CategoryResponse
import com.github.s8u.hotdeallist.entity.Category
import com.github.s8u.hotdeallist.repository.CategoryRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Category", description = "카테고리 API")
class CategoryController(
    private val categoryRepository: CategoryRepository
) {

    @GetMapping
    @Operation(summary = "카테고리 목록 조회", description = "트리 구조의 카테고리 목록을 반환")
    fun listCategories(): ResponseEntity<List<CategoryResponse>> {
        val allCategories = categoryRepository.findAll()
        val categoryMap = allCategories.groupBy { it.parentId }
        
        val rootCategories = categoryMap[null] ?: emptyList()
        val tree = buildTree(rootCategories.sortedBy { it.sortOrder }, categoryMap)
        
        return ResponseEntity.ok(tree)
    }

    private fun buildTree(
        categories: List<Category>,
        categoryMap: Map<Long?, List<Category>>
    ): List<CategoryResponse> {
        return categories.map { category ->
            val children = categoryMap[category.id]?.sortedBy { it.sortOrder } ?: emptyList()
            CategoryResponse(
                id = category.id!!,
                code = category.code,
                name = category.name,
                nameEn = category.nameEn,
                depth = category.depth,
                sortOrder = category.sortOrder,
                children = buildTree(children, categoryMap)
            )
        }
    }
}
