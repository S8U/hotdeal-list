package com.github.s8u.hotdeallist.crawler.impl

import com.github.s8u.hotdeallist.crawler.HotdealCrawlDetailDto
import com.github.s8u.hotdeallist.crawler.HotdealCrawlListDto
import com.github.s8u.hotdeallist.crawler.HotdealCrawler
import com.github.s8u.hotdeallist.enums.PlatformType
import org.jsoup.Jsoup
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PpomppuPpomppuCrawler : HotdealCrawler {

    override fun getPlatformType(): PlatformType {
        return PlatformType.PPOMPPU_PPOMPPU
    }

    /**
     * @param url https://www.ppomppu.co.kr/zboard/view.php?id=ppomppu&no={postId}
     */
    override fun getPlatformPostId(url: String): String? {
        if (!url.startsWith("https://www.ppomppu.co.kr/zboard/view.php?id=ppomppu")) {
            return null
        }

        return url.substringAfter("&no=")
    }

    override fun crawlList(page: Int): HotdealCrawlListDto {
        val document = Jsoup.connect("https://www.ppomppu.co.kr/zboard/zboard.php?id=ppomppu&page=${page}").get()

        val urlElements = document.select("#revolution_main_table .baseList:not(.hotpop_bg_color)")
            .filter { it.select(".baseList-numb").text().toIntOrNull() != null }
            .map { it.select(".baseList-title") }
        val urls = urlElements.map { "https://www.ppomppu.co.kr/zboard/" + it.attr("href") }

        return HotdealCrawlListDto(
            isSuccess = urls.isNotEmpty(),
            count = urls.size,
            maxCount = 20,
            urls = urls
        )
    }

    override fun crawlDetail(url: String): HotdealCrawlDetailDto {
        val postId = getPlatformPostId(url) ?: throw Exception("Invalid postId")

        val document = Jsoup.connect(url).get()

        val title = document.select(".container #topTitle h1").clone().apply { select("span").remove() }.text()
        val category = null
        val contentHtml = document.select(".container table:nth-of-type(3) table").html()
        val price = null
        val currencyUnit = "KRW"
        val viewCount = document.select(".container #topTitle .topTitle-mainbox li:nth-of-type(3)").text().substringAfter("조회수 ").toIntOrNull() ?: 0
        val commentCount = document.select("#comment").text().toIntOrNull() ?: 0
        val likeCount = document.select("#vote_list_btn_txt").text().toIntOrNull() ?: 0
        val isEnded = document.selectFirst(".container table:nth-of-type(3) #header_box") != null
        val sourceUrl = document.select(".container #topTitle .topTitle-mainbox .topTitle-link a").text()
        val thumbnailImageUrl = document.selectFirst("meta[property=og:image]")?.attr("content")
        val firstImageUrl = "https://" + document.select(".container table:nth-of-type(3) table img").first()?.attr("src")
        val wroteAtString = document.select(".container #topTitle .topTitle-mainbox li:nth-of-type(2)").text().substringAfter("등록일 ")
        val wroteAt = LocalDateTime.parse(wroteAtString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).atZone(ZoneId.systemDefault()).toLocalDateTime()

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