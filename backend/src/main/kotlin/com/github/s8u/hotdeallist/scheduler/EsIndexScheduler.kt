package com.github.s8u.hotdeallist.scheduler

import com.github.s8u.hotdeallist.config.AppModeProperties
import com.github.s8u.hotdeallist.enums.AppMode
import com.github.s8u.hotdeallist.repository.HotdealRepository
import com.github.s8u.hotdeallist.service.EsDictionaryService
import com.github.s8u.hotdeallist.service.EsIndexManagementService
import com.github.s8u.hotdeallist.service.HotdealSearchService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class EsIndexScheduler(
    private val esIndexManagementService: EsIndexManagementService,
    private val esDictionaryService: EsDictionaryService,
    private val hotdealSearchService: HotdealSearchService,
    private val hotdealRepository: HotdealRepository,
    private val appModeProperties: AppModeProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BATCH_SIZE = 10000
        private const val MIN_DOC_RATIO = 0.95
    }

    /**
     * 앱 시작 시: alias가 없으면 인덱스 초기화
     */
    @EventListener(ApplicationReadyEvent::class)
    fun initializeOnStartup() {
        if (esIndexManagementService.aliasExists()) {
            logger.info("ES alias '{}' 존재 — 초기화 건너뜀", EsIndexManagementService.ALIAS_NAME)
            return
        }

        logger.info("ES alias '{}' 없음 — 인덱스 초기화 시작", EsIndexManagementService.ALIAS_NAME)
        val reindexStartTime = LocalDateTime.now()

        try {
            // 사전 자동 추출
            esDictionaryService.extractAndUpdateDictionary()

            // 인덱스 생성 + 재인덱싱
            val targetIndex = EsIndexManagementService.INDEX_BLUE
            reindex(targetIndex)
            deltaSyncAfter(targetIndex, reindexStartTime)
            esIndexManagementService.switchAlias(targetIndex)
            logger.info("ES 인덱스 초기화 완료")
        } catch (e: Exception) {
            logger.error("ES 인덱스 초기화 실패", e)
            esIndexManagementService.deleteIndex(EsIndexManagementService.INDEX_BLUE)
        }
    }

    /**
     * 매일 새벽 4시: 사전 갱신 + blue/green 재인덱싱
     */
    @Scheduled(cron = "0 0 4 * * *")
    fun scheduledReindex() {
        if (appModeProperties.mode != AppMode.CRAWLING_API) {
            return
        }

        logger.info("=== ES 정기 재인덱싱 시작 ===")
        val reindexStartTime = LocalDateTime.now()

        try {
            // Step 0: 사전 자동 추출
            esDictionaryService.extractAndUpdateDictionary()

            // Step 1: 다음 인덱스 결정 (blue ↔ green)
            val currentIndex = esIndexManagementService.getCurrentIndex()
            val nextIndex = esIndexManagementService.getNextIndex()
            logger.info("인덱스 전환: {} → {}", currentIndex, nextIndex)

            // Step 2: 새 인덱스 생성 + 전체 재인덱싱
            reindex(nextIndex)

            // Step 3: delta sync (재인덱싱 중 변경된 문서)
            deltaSyncAfter(nextIndex, reindexStartTime)

            // Step 4: 문서 수 검증
            val newCount = esIndexManagementService.getDocumentCount(nextIndex)
            val oldCount = if (currentIndex != null) esIndexManagementService.getDocumentCount(currentIndex) else 0L
            logger.info("문서 수 검증 — 신규: {}, 기존: {}", newCount, oldCount)

            if (oldCount > 0 && newCount < oldCount * MIN_DOC_RATIO) {
                logger.error("문서 수가 기존 대비 {}% 미만 — 재인덱싱 중단", (MIN_DOC_RATIO * 100).toInt())
                esIndexManagementService.deleteIndex(nextIndex)
                return
            }

            // Step 5: alias atomic 스위칭
            esIndexManagementService.switchAlias(nextIndex)

            // Step 6: 구 인덱스 삭제
            if (currentIndex != null) {
                esIndexManagementService.deleteIndex(currentIndex)
            }

            logger.info("=== ES 정기 재인덱싱 완료 ({}건) ===", newCount)
        } catch (e: Exception) {
            logger.error("ES 정기 재인덱싱 실패", e)
            // 실패 시 새 인덱스만 정리 (기존 alias 유지)
            val nextIndex = esIndexManagementService.getNextIndex()
            esIndexManagementService.deleteIndex(nextIndex)
        }
    }

    /**
     * 인덱스 생성 + 전체 데이터 재인덱싱
     */
    private fun reindex(indexName: String) {
        esIndexManagementService.createIndex(indexName)

        var page = 0
        var totalMigrated = 0

        do {
            val pageRequest = PageRequest.of(page, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "id"))
            val hotdealsPage = hotdealRepository.findAll(pageRequest)
            val hotdeals = hotdealsPage.content

            if (hotdeals.isNotEmpty()) {
                hotdealSearchService.indexAllToIndex(hotdeals, indexName)
                totalMigrated += hotdeals.size
                logger.info("재인덱싱 진행: {}건 (누적: {})", hotdeals.size, totalMigrated)
            }

            page++
        } while (hotdeals.isNotEmpty())

        logger.info("전체 재인덱싱 완료: {}건 → {}", totalMigrated, indexName)
    }

    /**
     * delta sync: 재인덱싱 시작 이후 변경된 문서를 새 인덱스에 추가
     */
    private fun deltaSyncAfter(indexName: String, since: LocalDateTime) {
        val modifiedHotdeals = hotdealRepository.findByUpdatedAtGreaterThanEqual(since)
        if (modifiedHotdeals.isNotEmpty()) {
            hotdealSearchService.indexAllToIndex(modifiedHotdeals, indexName)
            logger.info("delta sync: {}건 추가 인덱싱 → {}", modifiedHotdeals.size, indexName)
        }
    }
}
