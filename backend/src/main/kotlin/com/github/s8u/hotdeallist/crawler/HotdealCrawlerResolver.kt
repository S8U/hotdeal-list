package com.github.s8u.hotdeallist.crawler

import com.github.s8u.hotdeallist.enums.PlatformType
import org.springframework.stereotype.Service

@Service
class HotdealCrawlerResolver(
    private val crawlers: List<HotdealCrawler>
) {

    fun getByPlatformType(platformType: PlatformType): HotdealCrawler? {
        return crawlers.find { it.getPlatformType() == platformType }
    }

}