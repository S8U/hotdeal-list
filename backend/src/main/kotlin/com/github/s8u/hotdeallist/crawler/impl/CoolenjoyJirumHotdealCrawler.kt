package com.github.s8u.hotdeallist.crawler.impl

import com.github.s8u.hotdeallist.crawler.HotdealCrawlDetailDto
import com.github.s8u.hotdeallist.crawler.HotdealCrawlListDto
import com.github.s8u.hotdeallist.crawler.HotdealCrawler
import com.github.s8u.hotdeallist.enums.PlatformType
import org.jsoup.Jsoup
import java.time.OffsetDateTime

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

        val urlElements = document.select("#bo_list li:not(.bg-light) .na-subject")
        val urls = urlElements.map { it.attr("href").substringBefore("?") }

        return HotdealCrawlListDto(
            isSuccess = urls.isNotEmpty(),
            count = urls.size,
            maxCount = 25,
            urls = urls
        )
    }

    override fun crawlDetail(url: String): HotdealCrawlDetailDto {
        val postId = getPlatformPostId(url) ?: throw Exception("Invalid postId")

        val document = Jsoup.connect(url).get()

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
    }

}