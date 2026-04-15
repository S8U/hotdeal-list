package com.github.s8u.hotdeallist.scheduler

import com.github.s8u.hotdeallist.repository.HotdealRawRepository
import com.github.s8u.hotdeallist.repository.HotdealRepository
import com.github.s8u.hotdeallist.service.HotdealSearchService
import com.github.s8u.hotdeallist.service.HotdealThumbnailService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "app", name = ["mode"], havingValue = "CRAWLING_API")
class HotdealThumbnailScheduler(
    private val hotdealRawRepository: HotdealRawRepository,
    private val hotdealRepository: HotdealRepository,
    private val hotdealSearchService: HotdealSearchService,
    private val thumbnailService: HotdealThumbnailService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 * * * * *")
    fun downloadMissingThumbnails() {
        val raws = hotdealRawRepository.findTop100ByIsThumbnailDownloadedFalseAndThumbnailImageUrlIsNotNull()
        if (raws.isEmpty()) return

        logger.info("썸네일 미다운로드 건 처리 시작: {}건", raws.size)

        var success = 0
        var failed = 0

        for (raw in raws) {
            val path = try {
                thumbnailService.downloadAndStore(
                    platformType = raw.platformType,
                    platformPostId = raw.platformPostId,
                    thumbnailUrl = raw.thumbnailImageUrl,
                    fallbackUrl = raw.firstImageUrl
                )
            } catch (e: Exception) {
                logger.warn("썸네일 다운로드 실패: rawId={}, error={}", raw.id, e.message)
                null
            }

            raw.isThumbnailDownloaded = true
            raw.thumbnailPath = path
            hotdealRawRepository.save(raw)

            if (path != null) {
                success++
                // hotdeals 테이블도 동기화
                val hotdeal = hotdealRepository.findByHotdealRawId(raw.id!!)
                if (hotdeal != null && hotdeal.thumbnailPath == null) {
                    hotdeal.thumbnailPath = path
                    hotdealRepository.save(hotdeal)
                    hotdealSearchService.updateHotdeal(hotdeal)
                }
            } else {
                failed++
            }

            Thread.sleep(1000)
        }

        logger.info("썸네일 다운로드 완료: 성공={}, 실패={}", success, failed)
    }
}
