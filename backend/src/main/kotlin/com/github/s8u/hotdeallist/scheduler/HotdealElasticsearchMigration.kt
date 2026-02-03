package com.github.s8u.hotdeallist.scheduler

import com.github.s8u.hotdeallist.repository.HotdealRepository
import com.github.s8u.hotdeallist.service.HotdealSearchService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("es-migration")
class HotdealElasticsearchMigration(
    private val hotdealRepository: HotdealRepository,
    private val hotdealSearchService: HotdealSearchService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun migrateOnStartup() {
        migrateAllHotdeals()
    }

    fun migrateAllHotdeals() {
        logger.info("Starting hotdeal migration to Elasticsearch...")

        val pageSize = 100
        var page = 0
        var totalMigrated = 0

        do {
            val pageRequest = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "id"))
            val hotdealsPage = hotdealRepository.findAll(pageRequest)
            val hotdeals = hotdealsPage.content

            if (hotdeals.isNotEmpty()) {
                hotdealSearchService.indexAll(hotdeals)
                totalMigrated += hotdeals.size
                logger.info("Migrated {} hotdeals (total: {})", hotdeals.size, totalMigrated)
            }

            page++
        } while (hotdeals.isNotEmpty())

        logger.info("Completed hotdeal migration to Elasticsearch. Total migrated: {}", totalMigrated)
    }
}
