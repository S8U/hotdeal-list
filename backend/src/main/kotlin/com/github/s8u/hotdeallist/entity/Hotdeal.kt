package com.github.s8u.hotdeallist.entity

import com.github.s8u.hotdeallist.entity.base.BaseEntity
import com.github.s8u.hotdeallist.enums.PlatformType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "hotdeals",
    indexes = [
        Index(name = "idx_raw_id", columnList = "raw_id"),
        Index(name = "idx_platform_type", columnList = "platform_type"),
        Index(name = "idx_wrote_at", columnList = "wrote_at"),
        Index(name = "idx_created_at", columnList = "created_at"),
        Index(name = "idx_price", columnList = "price")
    ],
    comment = "핫딜"
)
class Hotdeal(
    @Column(nullable = false, comment = "원본 핫딜 데이터 ID")
    val rawId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, comment = "플랫폼 타입")
    val platformType: PlatformType,

    @Column(nullable = false, comment = "게시글 URL")
    val url: String,

    @Column(nullable = false, comment = "게시글 제목")
    val title: String,

    @Column(comment = "게시글 제목 (영문)")
    val titleEn: String? = null,

    @Column(comment = "상품명")
    val productName: String? = null,

    @Column(comment = "상품명 (영문)")
    val productNameEn: String? = null,

    @Column(comment = "가격")
    val price: Int? = null,

    @Column(length = 3, comment = "통화 단위")
    val currencyUnit: String = "KRW",

    @Column(comment = "출처 URL")
    val sourceUrl: String? = null,

    @Column(comment = "좋아요 수")
    val likeCount: Int = 0,

    @Column(comment = "조회수")
    val viewCount: Int = 0,

    @Column(comment = "댓글 수")
    val commentCount: Int = 0,

    @Column(comment = "게시글 작성 시간")
    val wroteAt: LocalDateTime,
) : BaseEntity()


