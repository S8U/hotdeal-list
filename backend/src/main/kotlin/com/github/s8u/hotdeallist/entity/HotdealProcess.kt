package com.github.s8u.hotdeallist.entity

import com.github.s8u.hotdeallist.entity.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(
    name = "hotdeal_processes",
    indexes = [
        Index(name = "idx_hotdeal_raw_id", columnList = "hotdeal_raw_id"),
        Index(name = "idx_category_confidence", columnList = "category_confidence"),
        Index(name = "idx_created_at", columnList = "created_at")
    ],
    comment = "핫딜 가공 데이터"
)
class HotdealProcess(
    @Column(nullable = false, comment = "핫딜 원본 ID")
    val hotdealRawId: Long,

    @Column(nullable = false, comment = "AI 모델명")
    val aiModel: String,

    @Column(columnDefinition = "TEXT", nullable = false, comment = "AI 요청 프롬프트")
    val aiPrompt: String,

    @Column(columnDefinition = "TEXT", nullable = false, comment = "AI 응답 원본")
    val aiResponse: String,

    @Column(nullable = false, comment = "게시글 제목")
    val title: String,

    @Column(nullable = false, comment = "게시글 제목 (영문)")
    val titleEn: String,

    @Column(nullable = false, comment = "상품명 (한글)")
    val productName: String,

    @Column(nullable = false, comment = "상품명 (영어)")
    val productNameEn: String,

    @Column(nullable = false, comment = "카테고리 코드")
    val categoryCode: String,

    @Column(nullable = false, precision = 3, scale = 2, comment = "카테고리 분류 신뢰도 (0.00 ~ 1.00)")
    val categoryConfidence: BigDecimal,

    @Column(comment = "쇼핑 플랫폼")
    val shoppingPlatform: String? = null,

    @Column(scale = 2, comment = "가격")
    val price: BigDecimal? = null,

    @Column(length = 10, comment = "통화 단위")
    val currencyUnit: String? = null
) : BaseEntity()
