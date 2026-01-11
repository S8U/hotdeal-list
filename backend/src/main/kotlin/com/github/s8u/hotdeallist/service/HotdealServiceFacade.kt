package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.repository.HotdealProcessRepository
import com.github.s8u.hotdeallist.repository.HotdealRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HotdealServiceFacade(
    private val hotdealRepository: HotdealRepository,
    private val hotdealProcessRepository: HotdealProcessRepository,
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

        // 핫딜 크롤링
        val rawIds = hotdealRawService.crawlHotdealRaw(platformType, page, delay)

        // 핫딜 저장
        rawIds.forEach { rawId, _ ->
            val hotdealProcess = hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(rawId)
            val hotdeal = hotdealRepository.findByHotdealRawId(rawId)

            // 핫딜 데이터 가공
            if (hotdealProcess == null) {
                hotdealProcessService.processHotdealFromRaw(rawId)
            }

            // 핫딜 생성
            if (hotdeal == null) {
                hotdealService.createHotdealFromRawAndProcess(rawId)
            }
            // 핫딜 업데이트
            else {
                hotdealService.updateHotdealFromRaw(rawId)
            }
        }

        logger.info("Created hotdeal platformType={} page={}", platformType, page)
    }

}