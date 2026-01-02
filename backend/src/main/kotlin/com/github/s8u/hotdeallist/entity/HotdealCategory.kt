package com.github.s8u.hotdeallist.entity

import com.github.s8u.hotdeallist.entity.base.BaseEntity
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(
    name = "hotdeal_categories",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_hotdeal_category", columnNames = ["hotdeal_id", "category_id"])
    ],
    indexes = [
        Index(name = "idx_hotdeal_id", columnList = "hotdeal_id"),
        Index(name = "idx_category_id", columnList = "category_id"),
        Index(name = "idx_confidence", columnList = "confidence_score")
    ],
    comment = "핫딜 카테고리 매핑"
)
class HotdealCategory(
    @Column(nullable = false, comment = "핫딜 ID")
    val hotdealId: Long,

    @Column(nullable = false, comment = "카테고리 ID")
    val categoryId: Long,

    @Column(precision = 3, scale = 2, comment = "신뢰도 점수 (0.00 ~ 1.00)")
    val confidenceScore: BigDecimal? = null
) : BaseEntity()