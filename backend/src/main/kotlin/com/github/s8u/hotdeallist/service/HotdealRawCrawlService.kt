package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.crawler.HotdealCrawlerResolver
import com.github.s8u.hotdeallist.crawler.HotdealCrawlingException
import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.exception.BusinessException
import com.github.s8u.hotdeallist.repository.HotdealRawRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HotdealRawCrawlService(
    private val hotdealCrawlerResolver: HotdealCrawlerResolver,
    private val hotdealRawRepository: HotdealRawRepository,
    private val hotdealRawService: HotdealRawService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun crawlHotdealRaw(platformType: PlatformType, page: Int, delay: Long = 1000L): Map<Long, Boolean> {
        logger.info("Crawling hotdeal raw platformType={}, page={}", platformType, page)

        val crawler = hotdealCrawlerResolver.getByPlatformType(platformType)
            ?: throw BusinessException("지원하지 않는 플랫폼입니다.")

        // 게시글 목록 크롤링
        logger.info("Starting list crawling: platformType={}, page={}", platformType, page)
        val crawlListDto = try {
            crawler.crawlList(page)
        } catch (e: Exception) {
            logger.error(e.message, e)
            throw BusinessException("게시글 목록 크롤링에 실패했습니다.")
        }
        logger.info("Finished list crawling: platformType={}, page={}, count={}", platformType, page, crawlListDto.count)

        // 게시글 생성/업데이트
        val ids = hashMapOf<Long, Boolean>()

        crawlListDto.items.forEach { item ->
            try {
                val existing = hotdealRawRepository.findByPlatformTypeAndPlatformPostId(platformType, item.platformPostId)

                // 생성
                if (existing == null) {
                    // 게시글 상세 크롤링
                    logger.info("Starting detail crawling: platformType={}, platformPostId={}, url={}", platformType, item.platformPostId, item.url)
                    val crawlDetailDto = crawler.crawlDetail(item.url)
                    Thread.sleep(delay)
                    logger.info("Finished detail crawling: platformType={}, platformPostId={}", platformType, item.platformPostId)

                    val savedId = hotdealRawService.createHotdealRawWithThumbnail(platformType, item, crawlDetailDto)
                    ids[savedId] = true
                    logger.info("Hotdeal raw created: id={}, platformType={}, platformPostId={}", savedId, platformType, item.platformPostId)
                }
                // 업데이트
                else {
                    item.viewCount?.let { existing.viewCount = it }
                    item.commentCount?.let { existing.commentCount = it }
                    item.likeCount?.let { existing.likeCount = it }
                    existing.isEnded = item.isEnded

                    val saved = hotdealRawRepository.save(existing)
                    ids[saved.id!!] = false
                    logger.info("Hotdeal raw updated: id={}, platformType={}, platformPostId={}", saved.id, platformType, item.platformPostId)
                }
            } catch (e: HotdealCrawlingException) {
                logger.error("message={}, platformType={}, url={}", e.message, e.platformType, e.url, e)
                return@forEach
            } catch (e: Exception) {
                logger.error(e.message, e)
                return@forEach
            }
        }

        logger.info("Crawled hotdeal raw platformType={}, page={}, count={}", platformType, page, ids.size)

        return ids
    }

}