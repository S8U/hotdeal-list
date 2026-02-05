package com.github.s8u.hotdeallist.scheduler

import com.github.s8u.hotdeallist.repository.HotdealRepository
import com.github.s8u.hotdeallist.service.HotdealSearchService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.IndexOperations
import com.github.s8u.hotdeallist.document.HotdealDocument
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("es-migration")
class HotdealElasticsearchMigration(
    private val hotdealRepository: HotdealRepository,
    private val hotdealSearchService: HotdealSearchService,
    private val elasticsearchOperations: ElasticsearchOperations
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun migrateOnStartup() {
        deleteIndex()
        createIndex()
        migrateAllHotdeals()
    }

    private fun deleteIndex() {
        val indexOps = elasticsearchOperations.indexOps(HotdealDocument::class.java)
        if (indexOps.exists()) {
            indexOps.delete()
            logger.info("Deleted existing hotdeals index")
        }
    }

    private fun createIndex() {
        val indexOps = elasticsearchOperations.indexOps(HotdealDocument::class.java)
        indexOps.createWithMapping()
        logger.info("Created hotdeals index with new mapping")
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
