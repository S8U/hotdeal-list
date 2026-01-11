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
import java.time.OffsetDateTime

@Component
class CoolenjoyJirumHotdealCrawler : HotdealCrawler {

    override fun getPlatformType(): PlatformType {
        return PlatformType.COOLENJOY_JIRUM
    }

    /**
     * @param url https://coolenjoy.net/bbs/jirum/{postId}
     */
    override fun getPlatformPostId(url: String): String? {
        if (!url.startsWith("https://coolenjoy.net/bbs/jirum/")) {
            return null
        }

        return url.substringAfterLast("/").substringBefore("?")
    }

    override fun crawlList(page: Int): HotdealCrawlListDto {
        val document = Jsoup.connect("https://coolenjoy.net/bbs/jirum?page=${page}").get()

        val rowElements = document.select("#bo_list li:not(.bg-light)")
        val items = rowElements.map { rowElement ->
            val urlElement = rowElement.select(".na-subject")
            val url = urlElement.attr("href").substringBefore("?")

            val platformPostId = getPlatformPostId(url) ?: return@map null

            var category = rowElement.selectFirst("#abcd")?.text()

            val viewCount = rowElement.selectFirst("div.float-left.float-md-none.d-md-table-cell.nw-4.nw-md-auto.f-sm.font-weight-normal.py-md-2.pr-md-1")?.apply {
                select("i").remove()
                select("span").remove()
            }?.text()?.trim()?.toIntOrNull()
            val commentCount = rowElement.selectFirst(".count-plus a")?.text()?.toIntOrNull()
            val likeCount = rowElement.selectFirst(".rank-icon_vote")?.text()?.toIntOrNull()

            val isEnded = category == "종료됨"
            if (isEnded) {
                category = null // 카테고리에 종료 표시됨
            }

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
            maxCount = 25,
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

            val title = document.select("#bo_v_title").text().trim().substringAfter("| ")
            val category = document.select("#bo_v_title").text().trim().substringBefore(" ")
            val contentHtml = document.select(".view-content").html()
            val price = null
            val currencyUnit = "KRW"
            val viewCount = document.select("#bo_v_info div:nth-of-type(2) .pr-3:nth-of-type(1)").text().trim().substringBefore(" ").replace(",", "").toIntOrNull() ?: 0
            val commentCount = document.select("#bo_v_info div:nth-of-type(2) .pr-3:nth-of-type(2)").text().trim().substringBefore(" ").replace(",", "").toIntOrNull() ?: 0
            val likeCount = document.select("#bo_v_info .wr_good_cnt").text().trim().replace(",", "").toIntOrNull() ?: 0
            val isEnded = document.select("#bo_v_atc > b").text()?.contains("종료") ?: false
            val sourceUrl = document.select(".d-table-row.border-top.border-bottom a").text().substringBefore(" ")
            val thumbnailImageUrl = "https://coolenjoy.net/" + document.select(".view-content img").first()?.attr("src")
            val firstImageUrl = document.select(".view-content img").first()?.attr("src")
            val wroteAtString = document.select("#bo_v_info time").attr("datetime")
            val wroteAt = OffsetDateTime.parse(wroteAtString).toLocalDateTime()

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