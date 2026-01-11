package com.github.s8u.hotdeallist.crawler.dto

data class HotdealCrawlListItemDto(
    val url: String,
    val platformPostId: String,
    val category: String? = null,
    val viewCount: Int? = null,
    val commentCount: Int? = null,
    val likeCount: Int? = null,
    val isEnded: Boolean = false
)