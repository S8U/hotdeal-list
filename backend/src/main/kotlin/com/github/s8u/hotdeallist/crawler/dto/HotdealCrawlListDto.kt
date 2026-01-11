package com.github.s8u.hotdeallist.crawler.dto

data class HotdealCrawlListDto(
    val isSuccess: Boolean,
    val count: Int,
    val maxCount: Int,
    val items: List<HotdealCrawlListItemDto>
)