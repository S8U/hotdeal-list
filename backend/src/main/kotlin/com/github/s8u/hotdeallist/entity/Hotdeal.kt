package com.github.s8u.hotdeallist.entity

import com.github.s8u.hotdeallist.entity.base.BaseEntity
import com.github.s8u.hotdeallist.enums.PlatformType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "hotdeals",
    indexes = [
        Index(name = "idx_hotdeal_raw_id", columnList = "hotdeal_raw_id"),
        Index(name = "idx_platform_type", columnList = "platform_type"),
        Index(name = "idx_wrote_at", columnList = "wrote_at"),
        Index(name = "idx_created_at", columnList = "created_at"),
        Index(name = "idx_price", columnList = "price")
    ],
    comment = "핫딜"
)
class Hotdeal(
    @Column(nullable = false, comment = "핫딜 원본 데이터 ID")
    val hotdealRawId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, comment = "플랫폼 타입")
    val platformType: PlatformType,

    @Column(nullable = false, length = 2048, comment = "게시글 URL")
    val url: String,

    @Column(nullable = false, comment = "게시글 제목")
    val title: String,

    @Column(comment = "게시글 제목 (영문)")
    val titleEn: String? = null,

    @Column(comment = "상품명")
    val productName: String? = null,

    @Column(comment = "상품명 (영문)")
    val productNameEn: String? = null,

    @Column(scale = 2, comment = "가격")
    val price: BigDecimal? = null,

    @Column(length = 3, comment = "통화 단위")
    val currencyUnit: String = "KRW",

    @Column(comment = "조회수")
    var viewCount: Int = 0,

    @Column(comment = "댓글 수")
    var commentCount: Int = 0,

    @Column(comment = "좋아요 수")
    var likeCount: Int = 0,

    @Column(comment = "종료 여부")
    var isEnded: Boolean = false,

    @Column(length = 2048, comment = "출처 URL")
    val sourceUrl: String? = null,

    @Column(comment = "게시글 작성 시간")
    val wroteAt: LocalDateTime
) : BaseEntity()


