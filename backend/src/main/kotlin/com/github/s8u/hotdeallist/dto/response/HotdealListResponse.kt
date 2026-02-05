package com.github.s8u.hotdeallist.dto.response

data class HotdealListResponse(
    val items: List<HotdealResponse>,
    val nextCursor: String?,
    val hasMore: Boolean
)
