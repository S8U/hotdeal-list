package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.crawler.HotdealCrawlerResolver
import com.github.s8u.hotdeallist.crawler.HotdealCrawlingException
import com.github.s8u.hotdeallist.entity.HotdealRaw
import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.exception.BusinessException
import com.github.s8u.hotdeallist.repository.HotdealRawRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HotdealRawService(
    private val hotdealCrawlerResolver: HotdealCrawlerResolver,
    private val hotdealRawRepository: HotdealRawRepository
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
                var hotdealRaw = hotdealRawRepository.findByPlatformTypeAndPlatformPostId(platformType, item.platformPostId)

                // 생성
                if (hotdealRaw == null) {
                    // 게시글 상세 크롤링
                    logger.info("Starting detail crawling: platformType={}, platformPostId={}, url={}", platformType, item.platformPostId, item.url)
                    val crawlDetailDto = crawler.crawlDetail(item.url)
                    Thread.sleep(delay)
                    logger.info("Finished detail crawling: platformType={}, platformPostId={}", platformType, item.platformPostId)

                    hotdealRaw = HotdealRaw(
                        platformType = platformType,
                        platformPostId = crawlDetailDto.platformPostId,
                        url = crawlDetailDto.url,
                        title = crawlDetailDto.title,
                        category = item.category ?: crawlDetailDto.category,
                        contentHtml = crawlDetailDto.contentHtml,
                        price = crawlDetailDto.price,
                        currencyUnit = crawlDetailDto.currencyUnit,
                        viewCount = crawlDetailDto.viewCount,
                        commentCount = crawlDetailDto.commentCount,
                        likeCount = crawlDetailDto.likeCount,
                        isEnded = crawlDetailDto.isEnded,
                        sourceUrl = crawlDetailDto.sourceUrl,
                        thumbnailImageUrl = crawlDetailDto.thumbnailImageUrl,
                        firstImageUrl = crawlDetailDto.firstImageUrl,
                        wroteAt = crawlDetailDto.wroteAt
                    )

                    hotdealRaw = hotdealRawRepository.save(hotdealRaw)
                    ids[hotdealRaw.id!!] = true
                    logger.info("Hotdeal raw created: id={}, platformType={}, platformPostId={}", hotdealRaw.id, platformType, item.platformPostId)
                }
                // 업데이트
                else {
                    item.viewCount?.let { hotdealRaw.viewCount = it }
                    item.commentCount?.let { hotdealRaw.commentCount = it }
                    item.likeCount?.let { hotdealRaw.likeCount = it }
                    hotdealRaw.isEnded = item.isEnded

                    hotdealRaw = hotdealRawRepository.save(hotdealRaw)
                    ids[hotdealRaw.id!!] = false
                    logger.info("Hotdeal raw updated: id={}, platformType={}, platformPostId={}", hotdealRaw.id, platformType, item.platformPostId)
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