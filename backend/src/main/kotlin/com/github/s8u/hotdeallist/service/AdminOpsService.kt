package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.crawler.HotdealCrawlerResolver
import com.github.s8u.hotdeallist.dto.request.AdminOpsRequest
import com.github.s8u.hotdeallist.dto.response.AdminOpsResponse
import com.github.s8u.hotdeallist.enums.AdminOpsStage
import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.repository.HotdealProcessRepository
import com.github.s8u.hotdeallist.repository.HotdealRawRepository
import com.github.s8u.hotdeallist.repository.HotdealRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class AdminOpsService(
    private val hotdealRawCrawlService: HotdealRawCrawlService,
    private val hotdealThumbnailService: HotdealThumbnailService,
    private val hotdealProcessService: HotdealProcessService,
    private val hotdealService: HotdealService,
    private val hotdealSearchService: HotdealSearchService,
    private val hotdealRawRepository: HotdealRawRepository,
    private val hotdealProcessRepository: HotdealProcessRepository,
    private val hotdealRepository: HotdealRepository,
    private val hotdealCrawlerResolver: HotdealCrawlerResolver
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(request: AdminOpsRequest): AdminOpsResponse {
        val startMs = System.currentTimeMillis()

        // 검증
        require(request.stages.isNotEmpty()) { "stages는 비어있을 수 없습니다." }
        require(
            request.target.rawId != null ||
            request.target.hotdealId != null ||
            request.target.platform != null ||
            request.target.minPage != null ||
            request.target.wroteAtFrom != null ||
            request.target.limit != null
        ) { "target에 최소 하나의 조건이 필요합니다." }
        if (request.target.wroteAtFrom != null && request.target.wroteAtTo != null) {
            require(!request.target.wroteAtFrom.isAfter(request.target.wroteAtTo)) { "wroteAtFrom은 wroteAtTo보다 이전이어야 합니다." }
        }
        if (request.target.limit != null) {
            require(request.target.limit in 1..10000) { "limit은 1~10000 사이여야 합니다." }
        }
        if (request.target.maxPagesToScan !in 1..1000) {
            require(false) { "maxPagesToScan은 1~1000 사이여야 합니다." }
        }

        // 고정 순서로 정렬
        val orderedStages = AdminOpsStage.values().filter { it in request.stages }

        val byStage = mutableMapOf<String, MutableMap<String, Int>>()
        val errors = mutableListOf<AdminOpsResponse.Error>()

        // dryRun: target resolve만 수행
        if (request.dryRun) {
            val rawIds = resolveRawIds(request.target)
            val hotdealIds = if (AdminOpsStage.ES in request.stages && AdminOpsStage.HOTDEAL !in request.stages && AdminOpsStage.RAW !in request.stages) {
                resolveHotdealIds(request.target)
            } else emptyList()

            val targeted = if (AdminOpsStage.ES == orderedStages.firstOrNull() && AdminOpsStage.RAW !in request.stages && AdminOpsStage.HOTDEAL !in request.stages) {
                hotdealIds.size
            } else {
                rawIds.size
            }

            orderedStages.forEach { stage ->
                val stageMap = mutableMapOf<String, Int>()
                when (stage) {
                    AdminOpsStage.RAW -> stageMap["wouldCrawl"] = targeted
                    AdminOpsStage.THUMBNAIL -> stageMap["wouldProcess"] = targeted
                    AdminOpsStage.PROCESS -> {
                        stageMap["wouldProcess"] = targeted
                        // 토큰 비용 추정 (평균 입력 500토큰, 출력 50토큰)
                        val estimatedTokensIn = targeted * 500
                        val estimatedTokensOut = targeted * 50
                        // gpt-oss-120b 기준 추정 단가 (USD per 1M tokens)
                        val costUsd = (estimatedTokensIn * 0.8 + estimatedTokensOut * 0.8) / 1_000_000.0
                        stageMap["estimatedTokensIn"] = estimatedTokensIn
                        stageMap["estimatedTokensOut"] = estimatedTokensOut
                        stageMap["estimatedCostUsdCents"] = (costUsd * 100).toInt()
                    }
                    AdminOpsStage.HOTDEAL -> stageMap["wouldUpsert"] = targeted
                    AdminOpsStage.ES -> stageMap["wouldIndex"] = if (hotdealIds.isNotEmpty()) hotdealIds.size else targeted
                }
                byStage[stage.name.lowercase()] = stageMap
            }

            return AdminOpsResponse(
                elapsedMs = System.currentTimeMillis() - startMs,
                dryRun = true,
                stages = orderedStages,
                totals = AdminOpsResponse.Totals(targeted = targeted, processed = 0, skipped = 0, failed = 0),
                byStage = byStage,
                errors = errors
            )
        }

        // 실 실행
        var currentRawIds: List<Long> = emptyList()
        var currentHotdealIds: List<Long> = emptyList()
        var totalTargeted = 0
        var totalProcessed = 0
        var totalSkipped = 0
        var totalFailed = 0

        orderedStages.forEach { stage ->
            val stageMap = mutableMapOf<String, Int>()

            when (stage) {
                AdminOpsStage.RAW -> {
                    val result = runRawStage(request.target, request.raw, stageMap, errors)
                    currentRawIds = result
                    totalTargeted += result.size
                    totalProcessed += stageMap["created"] ?: 0
                    totalProcessed += stageMap["updated"] ?: 0
                    totalFailed += stageMap["failed"] ?: 0
                }
                AdminOpsStage.THUMBNAIL -> {
                    // 직전 RAW 단계가 없으면 target에서 resolve
                    if (AdminOpsStage.RAW !in request.stages) {
                        currentRawIds = resolveRawIds(request.target)
                        totalTargeted += currentRawIds.size
                    }
                    runThumbnailStage(currentRawIds, request.thumbnail, stageMap, errors)
                    totalProcessed += stageMap["downloaded"] ?: 0
                    totalSkipped += stageMap["skipped"] ?: 0
                    totalFailed += stageMap["failed"] ?: 0
                }
                AdminOpsStage.PROCESS -> {
                    val prevStages = orderedStages.takeWhile { it != AdminOpsStage.PROCESS }
                    if (prevStages.none { it == AdminOpsStage.RAW || it == AdminOpsStage.THUMBNAIL }) {
                        currentRawIds = resolveRawIds(request.target)
                        totalTargeted += currentRawIds.size
                    }
                    runProcessStage(currentRawIds, request.process, stageMap, errors)
                    totalProcessed += stageMap["called"] ?: 0
                    totalSkipped += stageMap["skipped"] ?: 0
                    totalFailed += stageMap["failed"] ?: 0
                }
                AdminOpsStage.HOTDEAL -> {
                    val prevStages = orderedStages.takeWhile { it != AdminOpsStage.HOTDEAL }
                    if (prevStages.none { it == AdminOpsStage.RAW || it == AdminOpsStage.THUMBNAIL || it == AdminOpsStage.PROCESS }) {
                        currentRawIds = resolveRawIds(request.target)
                        totalTargeted += currentRawIds.size
                    }
                    currentHotdealIds = runHotdealStage(currentRawIds, stageMap, errors)
                    totalProcessed += stageMap["created"] ?: 0
                    totalProcessed += stageMap["updated"] ?: 0
                    totalFailed += stageMap["failed"] ?: 0
                }
                AdminOpsStage.ES -> {
                    val prevStages = orderedStages.takeWhile { it != AdminOpsStage.ES }
                    when {
                        // 직전에 HOTDEAL 단계가 있으면 그 출력 사용
                        AdminOpsStage.HOTDEAL in prevStages -> {}
                        // RAW/THUMBNAIL/PROCESS 단계가 있으면 rawId -> hotdealId 변환
                        prevStages.any { it == AdminOpsStage.RAW || it == AdminOpsStage.THUMBNAIL || it == AdminOpsStage.PROCESS } -> {
                            currentHotdealIds = currentRawIds.mapNotNull { rawId ->
                                hotdealRepository.findByHotdealRawId(rawId)?.id
                            }
                        }
                        // 아무 단계도 없으면 target에서 hotdeal resolve
                        else -> {
                            currentHotdealIds = resolveHotdealIds(request.target)
                            totalTargeted += currentHotdealIds.size
                        }
                    }
                    runEsStage(currentHotdealIds, stageMap, errors)
                    totalProcessed += stageMap["indexed"] ?: 0
                    totalFailed += stageMap["failed"] ?: 0
                }
            }

            byStage[stage.name.lowercase()] = stageMap
        }

        return AdminOpsResponse(
            elapsedMs = System.currentTimeMillis() - startMs,
            dryRun = false,
            stages = orderedStages,
            totals = AdminOpsResponse.Totals(
                targeted = totalTargeted,
                processed = totalProcessed,
                skipped = totalSkipped,
                failed = totalFailed
            ),
            byStage = byStage,
            errors = errors
        )
    }

    private fun runRawStage(
        target: AdminOpsRequest.Target,
        options: AdminOpsRequest.RawOptions,
        stageMap: MutableMap<String, Int>,
        errors: MutableList<AdminOpsResponse.Error>
    ): List<Long> {
        val rawIds = mutableListOf<Long>()
        stageMap["created"] = 0
        stageMap["updated"] = 0
        stageMap["failed"] = 0

        val platforms = if (target.platform != null) {
            listOf(target.platform)
        } else {
            PlatformType.values().filter { hotdealCrawlerResolver.isSupportedPlatformType(it) }
        }

        when {
            // PAGE 모드
            target.minPage != null && target.maxPage != null -> {
                for (page in target.minPage..target.maxPage) {
                    platforms.forEach { platform ->
                        try {
                            val ids = hotdealRawCrawlService.crawlHotdealRaw(platform, page, options.delayMs).keys.toList()
                            rawIds.addAll(ids)
                            stageMap["created"] = (stageMap["created"] ?: 0) + ids.size
                        } catch (e: Exception) {
                            logger.error("RAW stage failed platform={}, page={}", platform, page, e)
                            stageMap["failed"] = (stageMap["failed"] ?: 0) + 1
                            errors.add(AdminOpsResponse.Error(stage = "raw", message = "platform=$platform page=$page: ${e.message}"))
                        }
                    }
                }
            }
            // WROTE_AT 모드
            target.wroteAtFrom != null -> {
                platforms.forEach { platform ->
                    var page = 1
                    var pagesScanned = 0
                    val maxPages = target.maxPagesToScan.coerceIn(1, 1000)
                    var shouldStop = false

                    while (!shouldStop && pagesScanned < maxPages) {
                        try {
                            val ids = hotdealRawCrawlService.crawlHotdealRaw(platform, page, options.delayMs).keys.toList()
                            rawIds.addAll(ids)
                            stageMap["created"] = (stageMap["created"] ?: 0) + ids.size
                            pagesScanned++

                            // 페이지의 모든 raw가 wroteAtFrom 이전이면 종료
                            if (ids.isNotEmpty()) {
                                val oldestInPage = hotdealRawRepository.findAllById(ids)
                                    .minByOrNull { it.wroteAt }
                                if (oldestInPage != null && oldestInPage.wroteAt.isBefore(target.wroteAtFrom)) {
                                    shouldStop = true
                                }
                            } else {
                                shouldStop = true
                            }
                            page++
                        } catch (e: Exception) {
                            logger.error("RAW stage failed platform={}, page={}", platform, page, e)
                            stageMap["failed"] = (stageMap["failed"] ?: 0) + 1
                            errors.add(AdminOpsResponse.Error(stage = "raw", message = "platform=$platform page=$page: ${e.message}"))
                            shouldStop = true
                        }
                    }
                }
            }
            // LIMIT 모드
            target.limit != null -> {
                val limit = target.limit.coerceIn(1, 10000)
                platforms.forEach { platform ->
                    var page = 1
                    var pagesScanned = 0
                    val maxPages = target.maxPagesToScan.coerceIn(1, 1000)

                    while (rawIds.size < limit && pagesScanned < maxPages) {
                        try {
                            val ids = hotdealRawCrawlService.crawlHotdealRaw(platform, page, options.delayMs).keys.toList()
                            rawIds.addAll(ids)
                            stageMap["created"] = (stageMap["created"] ?: 0) + ids.size
                            pagesScanned++
                            if (ids.isEmpty()) break
                            page++
                        } catch (e: Exception) {
                            logger.error("RAW stage failed platform={}, page={}", platform, page, e)
                            stageMap["failed"] = (stageMap["failed"] ?: 0) + 1
                            errors.add(AdminOpsResponse.Error(stage = "raw", message = "platform=$platform page=$page: ${e.message}"))
                            break
                        }
                    }
                }
            }
            else -> {
                logger.warn("RAW stage: no valid mode determined from target")
            }
        }

        // limit 초과분 제거
        val result = if (target.limit != null) rawIds.take(target.limit) else rawIds
        logger.info("RAW stage completed rawIds={}", result.size)
        return result
    }

    private fun runThumbnailStage(
        rawIds: List<Long>,
        options: AdminOpsRequest.ThumbnailOptions,
        stageMap: MutableMap<String, Int>,
        errors: MutableList<AdminOpsResponse.Error>
    ): List<Long> {
        stageMap["downloaded"] = 0
        stageMap["skipped"] = 0
        stageMap["failed"] = 0

        rawIds.forEach { rawId ->
            try {
                val raw = hotdealRawRepository.findById(rawId).orElse(null)
                if (raw == null) {
                    errors.add(AdminOpsResponse.Error(rawId = rawId, stage = "thumbnail", message = "Raw not found"))
                    stageMap["failed"] = (stageMap["failed"] ?: 0) + 1
                    return@forEach
                }

                if (!options.force && raw.thumbnailPath != null) {
                    stageMap["skipped"] = (stageMap["skipped"] ?: 0) + 1
                    return@forEach
                }

                val path = hotdealThumbnailService.downloadAndStore(
                    platformType = raw.platformType,
                    platformPostId = raw.platformPostId,
                    thumbnailUrl = raw.thumbnailImageUrl,
                    fallbackUrl = raw.firstImageUrl
                )

                if (path != null) {
                    raw.thumbnailPath = path
                    raw.isThumbnailDownloaded = true
                    hotdealRawRepository.save(raw)
                    stageMap["downloaded"] = (stageMap["downloaded"] ?: 0) + 1
                } else {
                    stageMap["skipped"] = (stageMap["skipped"] ?: 0) + 1
                }
            } catch (e: Exception) {
                logger.error("THUMBNAIL stage failed rawId={}", rawId, e)
                stageMap["failed"] = (stageMap["failed"] ?: 0) + 1
                errors.add(AdminOpsResponse.Error(rawId = rawId, stage = "thumbnail", message = e.message ?: "Unknown error"))
            }
        }

        logger.info("THUMBNAIL stage completed downloaded={}, skipped={}, failed={}", stageMap["downloaded"], stageMap["skipped"], stageMap["failed"])
        return rawIds
    }

    private fun runProcessStage(
        rawIds: List<Long>,
        options: AdminOpsRequest.ProcessOptions,
        stageMap: MutableMap<String, Int>,
        errors: MutableList<AdminOpsResponse.Error>
    ): List<Long> {
        stageMap["called"] = 0
        stageMap["skipped"] = 0
        stageMap["failed"] = 0

        if (options.promptVersion != null) {
            logger.warn("PROCESS stage: promptVersion={} is not yet supported (Phase B), ignoring", options.promptVersion)
        }

        rawIds.forEach { rawId ->
            try {
                if (!options.force) {
                    val existing = hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(rawId)
                    if (existing != null) {
                        stageMap["skipped"] = (stageMap["skipped"] ?: 0) + 1
                        return@forEach
                    }
                }
                hotdealProcessService.processHotdealFromRaw(rawId)
                stageMap["called"] = (stageMap["called"] ?: 0) + 1
            } catch (e: Exception) {
                logger.error("PROCESS stage failed rawId={}", rawId, e)
                stageMap["failed"] = (stageMap["failed"] ?: 0) + 1
                errors.add(AdminOpsResponse.Error(rawId = rawId, stage = "process", message = e.message ?: "Unknown error"))
            }
        }

        logger.info("PROCESS stage completed called={}, skipped={}, failed={}", stageMap["called"], stageMap["skipped"], stageMap["failed"])
        return rawIds
    }

    private fun runHotdealStage(
        rawIds: List<Long>,
        stageMap: MutableMap<String, Int>,
        errors: MutableList<AdminOpsResponse.Error>
    ): List<Long> {
        val hotdealIds = mutableListOf<Long>()
        stageMap["created"] = 0
        stageMap["updated"] = 0
        stageMap["categoryRebuilt"] = 0
        stageMap["failed"] = 0

        rawIds.forEach { rawId ->
            try {
                val existingBefore = hotdealRepository.findByHotdealRawId(rawId)
                val hotdealId = hotdealService.upsertHotdealFromRawAndProcess(rawId)
                hotdealIds.add(hotdealId)

                if (existingBefore == null) {
                    stageMap["created"] = (stageMap["created"] ?: 0) + 1
                } else {
                    stageMap["updated"] = (stageMap["updated"] ?: 0) + 1
                    // process_id가 변경되었으면 categoryRebuilt 카운트
                    val latestProcess = hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(rawId)
                    if (latestProcess != null && existingBefore.hotdealProcessId != latestProcess.id) {
                        stageMap["categoryRebuilt"] = (stageMap["categoryRebuilt"] ?: 0) + 1
                    }
                }
            } catch (e: Exception) {
                logger.error("HOTDEAL stage failed rawId={}", rawId, e)
                stageMap["failed"] = (stageMap["failed"] ?: 0) + 1
                errors.add(AdminOpsResponse.Error(rawId = rawId, stage = "hotdeal", message = e.message ?: "Unknown error"))
            }
        }

        logger.info("HOTDEAL stage completed created={}, updated={}, categoryRebuilt={}, failed={}", stageMap["created"], stageMap["updated"], stageMap["categoryRebuilt"], stageMap["failed"])
        return hotdealIds
    }

    private fun runEsStage(
        hotdealIds: List<Long>,
        stageMap: MutableMap<String, Int>,
        errors: MutableList<AdminOpsResponse.Error>
    ) {
        stageMap["indexed"] = 0
        stageMap["failed"] = 0

        // 1000건 청크로 bulk index
        hotdealIds.chunked(1000).forEach { chunk ->
            try {
                val hotdeals = hotdealRepository.findAllById(chunk)
                hotdealSearchService.indexAll(hotdeals)
                stageMap["indexed"] = (stageMap["indexed"] ?: 0) + hotdeals.size
            } catch (e: Exception) {
                logger.error("ES stage failed chunk size={}", chunk.size, e)
                stageMap["failed"] = (stageMap["failed"] ?: 0) + chunk.size
                errors.add(AdminOpsResponse.Error(stage = "es", message = "Chunk failed: ${e.message}"))
            }
        }

        logger.info("ES stage completed indexed={}, failed={}", stageMap["indexed"], stageMap["failed"])
    }

    private fun resolveRawIds(target: AdminOpsRequest.Target): List<Long> {
        // 단건 raw 지정
        if (target.rawId != null) {
            return if (hotdealRawRepository.existsById(target.rawId)) {
                listOf(target.rawId)
            } else {
                logger.warn("Raw not found rawId={}", target.rawId)
                emptyList()
            }
        }

        // 조건 검색
        val limit = (target.limit ?: MAX_LIMIT).coerceIn(1, MAX_LIMIT)
        val pageable = PageRequest.of(0, limit)
        val ids = hotdealRawRepository.findIdsByCriteria(
            platform = target.platform,
            wroteAtFrom = target.wroteAtFrom,
            wroteAtTo = target.wroteAtTo,
            pageable = pageable
        )
        logger.info("Resolved raw ids count={}", ids.size)
        return ids
    }

    private fun resolveHotdealIds(target: AdminOpsRequest.Target): List<Long> {
        // 단건 hotdeal 지정
        if (target.hotdealId != null) {
            return if (hotdealRepository.existsById(target.hotdealId)) {
                listOf(target.hotdealId)
            } else {
                logger.warn("Hotdeal not found hotdealId={}", target.hotdealId)
                emptyList()
            }
        }

        // 단건 raw로 hotdeal 조회
        if (target.rawId != null) {
            val hotdeal = hotdealRepository.findByHotdealRawId(target.rawId)
            return if (hotdeal != null) listOf(hotdeal.id!!) else emptyList()
        }

        // 조건 검색
        val limit = (target.limit ?: MAX_LIMIT).coerceIn(1, MAX_LIMIT)
        val pageable = PageRequest.of(0, limit)
        val ids = hotdealRepository.findIdsByCriteria(
            platform = target.platform,
            wroteAtFrom = target.wroteAtFrom,
            wroteAtTo = target.wroteAtTo,
            pageable = pageable
        )
        logger.info("Resolved hotdeal ids count={}", ids.size)
        return ids
    }

    companion object {
        private const val MAX_LIMIT = 10000
    }
}
