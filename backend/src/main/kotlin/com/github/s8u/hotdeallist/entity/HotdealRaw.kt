package com.github.s8u.hotdeallist.entity

import com.github.s8u.hotdeallist.entity.base.BaseEntity
import com.github.s8u.hotdeallist.enums.PlatformType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "hotdeal_raws",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_platform_post", columnNames = ["platform_type", "platform_post_id"])
    ],
    indexes = [
        Index(name = "idx_platform_type", columnList = "platform_type"),
        Index(name = "idx_wrote_at", columnList = "wrote_at"),
        Index(name = "idx_is_ended", columnList = "is_ended"),
        Index(name = "idx_created_at", columnList = "created_at")
    ],
    comment = "핫딜 원본 데이터"
)
class HotdealRaw(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, comment = "플랫폼 타입")
    val platformType: PlatformType,

    @Column(nullable = false, comment = "플랫폼 게시글 ID")
    val platformPostId: String,

    @Column(nullable = false, length = 2048, comment = "게시글 URL")
    val url: String,

    @Column(nullable = false, comment = "게시글 제목")
    val title: String,

    @Column(comment = "게시글 카테고리")
    val category: String? = null,

    @Column(columnDefinition = "TEXT", comment = "게시글 본문 HTML")
    val contentHtml: String? = null,

    @Column(nullable = true, scale = 2, comment = "가격")
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

    @Column(length = 2048, comment = "썸네일 이미지 URL")
    val thumbnailImageUrl: String? = null,

    @Column(length = 2048, comment = "첫 번째 이미지 URL")
    val firstImageUrl: String? = null,

    @Column(nullable = false, comment = "썸네일 다운로드 여부")
    var isThumbnailDownloaded: Boolean = false,

    @Column(length = 512, comment = "썸네일 경로")
    var thumbnailPath: String? = null,

    @Column(nullable = false, comment = "게시글 작성 시간")
    val wroteAt: LocalDateTime
) : BaseEntity()


