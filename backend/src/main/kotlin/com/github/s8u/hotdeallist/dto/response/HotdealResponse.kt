package com.github.s8u.hotdeallist.dto.response

import com.github.s8u.hotdeallist.enums.PlatformType
import java.math.BigDecimal
import java.time.LocalDateTime

data class HotdealResponse(
    val id: Long,
    val platformType: PlatformType,
    val url: String,
    val title: String,
    val productName: String?,
    val price: BigDecimal?,
    val currencyUnit: String,
    val viewCount: Int,
    val commentCount: Int,
    val likeCount: Int,
    val isEnded: Boolean,
    val thumbnailUrl: String?,
    val shoppingPlatform: String?,
    val categoryCodes: List<String>,
    val wroteAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val highlightedTitle: String? = null,
    val highlightedProductName: String? = null,
)
