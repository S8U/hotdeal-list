package com.github.s8u.hotdeallist.crawler

data class HotdealCrawlListDto(
    val isSuccess: Boolean,
    val count: Int,
    val maxCount: Int,
    val urls: List<String>
)