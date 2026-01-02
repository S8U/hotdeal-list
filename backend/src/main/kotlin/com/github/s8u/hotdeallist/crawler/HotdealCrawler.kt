package com.github.s8u.hotdeallist.crawler

import com.github.s8u.hotdeallist.enums.PlatformType

interface HotdealCrawler {

    fun getPlatformType(): PlatformType

    fun getPlatformPostId(url: String): String?

    fun crawlList(page: Int): HotdealCrawlListDto

    fun crawlDetail(url: String): HotdealCrawlDetailDto

}