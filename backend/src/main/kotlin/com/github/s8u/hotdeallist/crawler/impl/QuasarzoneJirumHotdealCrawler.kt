package com.github.s8u.hotdeallist.crawler.impl

import com.github.s8u.hotdeallist.crawler.HotdealCrawler
import com.github.s8u.hotdeallist.crawler.HotdealCrawlingException
import com.github.s8u.hotdeallist.crawler.dto.HotdealCrawlDetailDto
import com.github.s8u.hotdeallist.crawler.dto.HotdealCrawlListDto
import com.github.s8u.hotdeallist.crawler.dto.HotdealCrawlListItemDto
import com.github.s8u.hotdeallist.enums.PlatformType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class QuasarzoneJirumHotdealCrawler : HotdealCrawler {

    override fun getPlatformType(): PlatformType {
        return PlatformType.QUASARZONE_JIRUM
    }

    /**
     * @param url https://quasarzone.com//bbs/qb_saleinfo/views/{postId}
     */
    override fun getPlatformPostId(url: String): String? {
        if (!url.startsWith("https://quasarzone.com//bbs/qb_saleinfo/views/")) {
            return null
        }

        return url.substringAfterLast("/").substringBefore("?")
    }

    override fun crawlList(page: Int): HotdealCrawlListDto {
        val document = Jsoup.connect("https://quasarzone.com/bbs/qb_saleinfo?page=${page}").get()

        val rowElements = document.select(".market-type-list tbody tr")
        val items = rowElements.map { rowElement ->
            val urlElement = rowElement.select(".subject-link")
            val url = "https://quasarzone.com/" + urlElement.attr("href").substringBefore("?")

            val platformPostId = getPlatformPostId(url) ?: return@map null

            val category = rowElement.selectFirst(".category")?.text()

            val viewCountString = rowElement.selectFirst(".count")?.text()?.trim()
            val viewCount = if (viewCountString?.contains("k") == true) {
                val viewCountK = viewCountString.replace("k", "").toDoubleOrNull() ?: 0.0
                (viewCountK * 1000).toInt()
            } else {
                viewCountString?.toIntOrNull()
            }
            val commentCount = rowElement.selectFirst(".ctn-count")?.text()?.trim()?.toIntOrNull()
            val likeCount = rowElement.selectFirst(".num")?.text()?.toIntOrNull()

            val isEnded = rowElement.selectFirst(".done") != null

            HotdealCrawlListItemDto(
                url = url,
                platformPostId = platformPostId,
                category = category,
                viewCount = viewCount,
                commentCount = commentCount,
                likeCount = likeCount,
                isEnded = isEnded
            )
        }.filterNotNull()

        return HotdealCrawlListDto(
            isSuccess = items.isNotEmpty(),
            count = items.size,
            maxCount = 30,
            items = items
        )
    }

    override fun crawlDetail(url: String): HotdealCrawlDetailDto {
        val postId = getPlatformPostId(url) ?: throw HotdealCrawlingException(
            message = "Invalid postId",
            platformType = getPlatformType(),
            url = url
        )

        lateinit var document: Document

        try {
            document = Jsoup.connect(url).get()

            val title = document.select("#content h1.title").text().trim().substringAfter(" ")
            val category = document.select("#content .util-area .ca_name").text().trim()
            val contentHtml = document.select("#org_contents").text()
            val priceString = document.select("#content .market-info-view-table .text-orange").text().trim()
            val price = priceString.substringAfter(" ").substringBefore(" (").replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO
            val currencyUnit = priceString.substringAfter("(").substringBefore(")")
            val viewCount = document.select("#content .util-area .count .view").text().trim().substringBefore(" ").toIntOrNull() ?: 0
            val commentCount = document.select("#content .util-area .count .reply").text().trim().substringBefore(" ").toIntOrNull() ?: 0
            val likeCount = document.select("#boardGoodCount").text().trim().toIntOrNull() ?: 0
            val isEnded = document.select("#content .title .done").firstOrNull() != null
            val sourceUrl = document.select("#content .market-info-view-table a").text()
            val thumbnailImageUrl = document.selectFirst("meta[property=og:image]")?.attr("content")
            val firstImageUrl = Jsoup.parse(contentHtml).select("img").first()?.attr("src")
            val wroteAtString = document.select("#content .util-area .date").text().trim()
            val wroteAt = LocalDateTime.parse(wroteAtString, DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")).atZone(ZoneId.systemDefault()).toLocalDateTime()

            return HotdealCrawlDetailDto(
                platformType = getPlatformType(),
                platformPostId = postId,
                title = title,
                category = category,
                contentHtml = contentHtml,
                price = price,
                currencyUnit = currencyUnit,
                viewCount = viewCount,
                commentCount = commentCount,
                likeCount = likeCount,
                isEnded = isEnded,
                sourceUrl = sourceUrl,
                thumbnailImageUrl = thumbnailImageUrl,
                firstImageUrl = firstImageUrl,
                wroteAt = wroteAt,
                url = url
            )
        } catch (e: Exception) {
            throw HotdealCrawlingException(
                message = e.message,
                platformType = getPlatformType(),
                url = url,
                html = document.html(),
                cause = e
            )
        }
    }

}