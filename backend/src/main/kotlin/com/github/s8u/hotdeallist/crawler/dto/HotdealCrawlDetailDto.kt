package com.github.s8u.hotdeallist.crawler.dto

import com.github.s8u.hotdeallist.enums.PlatformType
import java.math.BigDecimal
import java.time.LocalDateTime

data class HotdealCrawlDetailDto(
    val platformType: PlatformType,
    val platformPostId: String,
    val url: String,
    val title: String,
    val category: String? = null,
    val contentHtml: String? = null,
    val price: BigDecimal? = null,
    val currencyUnit: String = "KRW",
    val viewCount: Int = 0,
    val commentCount: Int = 0,
    val likeCount: Int = 0,
    val isEnded: Boolean = false,
    val sourceUrl: String? = null,
    val thumbnailImageUrl: String? = null,
    val firstImageUrl: String? = null,
    val wroteAt: LocalDateTime
)