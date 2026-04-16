package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.crawler.dto.HotdealCrawlDetailDto
import com.github.s8u.hotdeallist.crawler.dto.HotdealCrawlListItemDto
import com.github.s8u.hotdeallist.entity.HotdealRaw
import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.repository.HotdealRawRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HotdealRawService(
    private val hotdealRawRepository: HotdealRawRepository,
    private val hotdealThumbnailService: HotdealThumbnailService
) {

    @Transactional
    fun createHotdealRawWithThumbnail(
        platformType: PlatformType,
        item: HotdealCrawlListItemDto,
        crawlDetailDto: HotdealCrawlDetailDto
    ): Long {
        val thumbnailPath = hotdealThumbnailService.downloadAndStore(
            platformType = platformType,
            platformPostId = crawlDetailDto.platformPostId,
            thumbnailUrl = crawlDetailDto.thumbnailImageUrl,
            fallbackUrl = crawlDetailDto.firstImageUrl
        )

        val hotdealRaw = HotdealRaw(
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
            thumbnailPath = thumbnailPath,
            isThumbnailDownloaded = true,
            wroteAt = crawlDetailDto.wroteAt
        )

        return hotdealRawRepository.save(hotdealRaw).id!!
    }
}
