package com.github.s8u.hotdeallist.crawler.impl

import com.github.s8u.hotdeallist.crawler.HotdealCrawlDetailDto
import com.github.s8u.hotdeallist.crawler.HotdealCrawlListDto
import com.github.s8u.hotdeallist.crawler.HotdealCrawler
import com.github.s8u.hotdeallist.enums.PlatformType
import org.jsoup.Jsoup
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RuliwebHotdealHotdealCrawler : HotdealCrawler {

    override fun getPlatformType(): PlatformType {
        return PlatformType.RULIWEB_HOTDEAL
    }

    /**
     * @param url https://bbs.ruliweb.com/market/board/1020/read/{postId}
     */
    override fun getPlatformPostId(url: String): String? {
        if (!url.startsWith("https://bbs.ruliweb.com/market/board/1020/read/")) {
            return null
        }

        return url.substringAfterLast("/").substringBefore("?")
    }

    override fun crawlList(page: Int): HotdealCrawlListDto {
        val document = Jsoup.connect("https://bbs.ruliweb.com/market/board/1020?page=${page}").get()

        val urlElements = document.select(".board_main .table_body:not(.notice):not(.best) .subject_link")
        val urls = urlElements.map { it.attr("href").substringBefore("?") }

        return HotdealCrawlListDto(
            isSuccess = urls.isNotEmpty(),
            count = urls.size,
            maxCount = 28,
            urls = urls
        )
    }

    override fun crawlDetail(url: String): HotdealCrawlDetailDto {
        val postId = getPlatformPostId(url) ?: throw Exception("Invalid postId")

        val document = Jsoup.connect(url).get()

        val title = document.select(".board_main .subject_text .subject_inner_text").text()
        val category = document.select(".board_main .subject_text .category_text").text()
        val contentHtml = document.select(".board_main .board_main_view .view_content").html()
        val price = null
        val currencyUnit = "KRW"
        val viewCount = document.select(".board_main .user_info p:nth-of-type(5)").text().substringAfter("조회 ").substringBefore(" ").toIntOrNull() ?: 0
        val commentCount = document.select(".comment_container .reply_count").text().toIntOrNull() ?: 0
        val likeCount = document.select(".board_main .user_info p:nth-of-type(5) .like").text().trim().toIntOrNull() ?: 0
        val isEnded = document.select(".board_main > div:nth-of-type(3)").text().contains("이 핫딜은 종료되었습니다")
        val sourceUrl = document.select(".board_main .source_url a").text()
        val thumbnailImageUrl = document.selectFirst("meta[property=og:image]")?.attr("content")
        val firstImageUrl = Jsoup.parse(contentHtml).select("img").first()?.attr("src")
        val wroteAtString = document.select(".board_main .user_info .regdate").text().trim()
        val wroteAt = LocalDateTime.parse(wroteAtString, DateTimeFormatter.ofPattern("yyyy.MM.dd (HH:mm:ss)")).atZone(ZoneId.systemDefault()).toLocalDateTime()

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