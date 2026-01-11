package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.enums.PlatformType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HotdealServiceFacade(
    private val hotdealRawService: HotdealRawService,
    private val hotdealService: HotdealService,
    private val hotdealProcessService: HotdealProcessService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun createHotdealAllPlatforms(page: Int, delay: Long = 1000L) {
        logger.info("Creating hotdeal all platforms page={}", page)

        PlatformType.values().forEach { platformType ->
            createHotdeal(platformType, page, delay)
        }
        
        logger.info("Created hotdeal all platforms page={}", page)
    }

    fun createHotdeal(platformType: PlatformType, page: Int, delay: Long = 1000L) {
        logger.info("Creating hotdeal platformType={}, page={}", platformType, page)

        // 크롤링
        val rawIds = hotdealRawService.crawlHotdealRaw(platformType, page, delay)

        // 생성
        rawIds.forEach { rawId, isCreate ->
            // 생성
            if (isCreate) {
                // 핫딜 데이터 가공
                hotdealProcessService.processHotdealFromRaw(rawId)

                // 핫딜 생성
                hotdealService.createHotdealFromRawAndProcess(rawId)
            }
            // 업데이트
            else {
                // 핫딜 데이터 업데이트
                hotdealService.updateHotdealFromRaw(rawId)
            }
        }

        logger.info("Created hotdeal platformType={} page={}", platformType, page)
    }

}