package com.github.s8u.hotdeallist.scheduler

import com.github.s8u.hotdeallist.service.HotdealServiceFacade
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class HotdealCrawlScheduler(
    private val hotdealServiceFacade: HotdealServiceFacade
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "30 */3 * * * *")
    fun crawlHotdeal() {
        logger.info("Starting scheduled hotdeal crawl")
        hotdealServiceFacade.createHotdealAllPlatforms(page = 1)
        logger.info("Completed scheduled hotdeal crawl")
    }
}