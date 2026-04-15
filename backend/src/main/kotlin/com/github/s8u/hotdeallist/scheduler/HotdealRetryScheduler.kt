package com.github.s8u.hotdeallist.scheduler

import com.github.s8u.hotdeallist.repository.HotdealProcessRepository
import com.github.s8u.hotdeallist.repository.HotdealRawRepository
import com.github.s8u.hotdeallist.service.HotdealProcessService
import com.github.s8u.hotdeallist.service.HotdealService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "app", name = ["mode"], havingValue = "CRAWLING_API")
class HotdealRetryScheduler(
    private val hotdealRawRepository: HotdealRawRepository,
    private val hotdealProcessRepository: HotdealProcessRepository,
    private val hotdealProcessService: HotdealProcessService,
    private val hotdealService: HotdealService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 * * * * *")
    fun retryUnprocessedRaws() {
        val raws = hotdealRawRepository.findRawsWithoutHotdeal()
        if (raws.isEmpty()) return

        logger.info("미처리 핫딜 재시도 시작: {}건", raws.size)

        var success = 0
        var failed = 0

        for (raw in raws) {
            try {
                // AI 가공이 안 된 경우 먼저 가공
                val process = hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(raw.id!!)
                if (process == null) {
                    hotdealProcessService.processHotdealFromRaw(raw.id!!)
                }

                // 핫딜 생성
                hotdealService.createHotdealFromRawAndProcess(raw.id!!)
                success++
            } catch (e: Exception) {
                failed++
                logger.warn("핫딜 재시도 실패: rawId={}, error={}", raw.id, e.message)
            }
        }

        logger.info("미처리 핫딜 재시도 완료: 성공={}, 실패={}", success, failed)
    }
}
