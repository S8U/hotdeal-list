package com.github.s8u.hotdeallist.crawler.impl

import com.github.s8u.hotdeallist.crawler.HotdealCrawlDetailDto
import com.github.s8u.hotdeallist.crawler.HotdealCrawlListDto
import com.github.s8u.hotdeallist.crawler.HotdealCrawler
import com.github.s8u.hotdeallist.enums.PlatformType
import org.jsoup.Jsoup
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ClienAltteulHotdealCrawler : HotdealCrawler {

    override fun getPlatformType(): PlatformType {
        return PlatformType.CLIEN_ALTTEUL
    }

    /**
     * @param url https://www.clien.net/service/board/jirum/{postId}
     */
    override fun getPlatformPostId(url: String): String? {
        if (!url.startsWith("https://www.clien.net/service/board/jirum/")) {
            return null
        }

        return url.substringAfterLast("/").substringBefore("?")
    }

    override fun crawlList(page: Int): HotdealCrawlListDto {
        val document = Jsoup.connect("https://www.clien.net/service/board/jirum?&od=T31&category=0&po=${page - 1}").get()

        val urlElements = document.select(".list_content .contents_jirum .list_subject a:first-child")
        val urls = urlElements.map { "https://www.clien.net" + it.attr("href") }

        return HotdealCrawlListDto(
            isSuccess = urls.isNotEmpty(),
            count = urls.size,
            maxCount = 30,
            urls = urls
        )
    }

    override fun crawlDetail(url: String): HotdealCrawlDetailDto {
        val postId = getPlatformPostId(url) ?: throw Exception("Invalid postId")

        val document = Jsoup.connect(url).get()

        var title = document.select(".content_view .post_title .post_subject").clone().apply {
            select(".post_soldout").remove()
            select(".post_category").remove()
            select("a").remove()
        }.text()
        if (title.startsWith("품절 ")) {
            title = title.substringAfter("품절 ")
        }
        val category = document.select(".content_view .post_title .post_subject .post_category").text()
        val contentHtml = document.select(".content_view .post_content .post_article").html()
        val price = null
        val currencyUnit = "KRW"
        val viewCount = document.select(".content_view .post_author .view_count strong").text().replace(",", "").toIntOrNull() ?: 0
        val commentCount = document.select(".content_view .post_comment .comment_head strong").text().replace(",", "").toIntOrNull() ?: 0
        val likeCount = document.select(".content_view .post_title .post_symph").text().trim().replace(",", "").toIntOrNull() ?: 0
        val isEnded = document.selectFirst(".content_view .post_title .post_subject .post_soldout") != null
        val sourceUrl = document.select(".content_view .post_view .attached_link a").text()
        val thumbnailImageUrl = document.selectFirst("meta[property=og:image]")?.attr("content") + "?scale=width:480"
        val firstImageUrl = document.select(".content_view .post_content .post_article img").first()?.attr("src")
        val wroteAtString = document.select(".content_view .post_author .view_count.date").clone().apply { select("span").remove() }.text().trim()
        val wroteAt = LocalDateTime.parse(wroteAtString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(ZoneId.systemDefault()).toLocalDateTime()

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