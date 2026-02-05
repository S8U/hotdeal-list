package com.github.s8u.hotdeallist.dto.response

data class CategoryResponse(
    val id: Long,
    val code: String,
    val name: String,
    val nameEn: String?,
    val depth: Int,
    val sortOrder: Int,
    val children: List<CategoryResponse> = emptyList()
)
