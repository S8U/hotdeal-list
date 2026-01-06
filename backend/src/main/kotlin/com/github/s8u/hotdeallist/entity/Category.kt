package com.github.s8u.hotdeallist.entity

import com.github.s8u.hotdeallist.entity.base.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "categories",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_code", columnNames = ["code"])
    ],
    indexes = [
        Index(name = "idx_parent_id", columnList = "parent_id"),
        Index(name = "idx_depth", columnList = "depth"),
        Index(name = "idx_sort_order", columnList = "sort_order")
    ],
    comment = "카테고리 정보"
)
class Category(
    @Column(comment = "부모 카테고리 ID")
    var parentId: Long? = null,

    @Column(nullable = false, unique = true, comment = "카테고리 코드")
    val code: String,

    @Column(nullable = false, comment = "카테고리 이름")
    var name: String,

    @Column(comment = "카테고리 이름 (영문)")
    var nameEn: String? = null,

    @Column(nullable = false, comment = "카테고리 깊이")
    var depth: Int = 0,

    @Column(comment = "정렬 순서")
    var sortOrder: Int = 0
) : BaseEntity()
