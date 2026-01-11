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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
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

        val rowElements = document.select(".list_content .contents_jirum .list_item")
        val items = rowElements.map { rowElement ->
            val urlElement = rowElement.select(".list_subject a:first-child")
            val url = "https://www.clien.net" + urlElement.attr("href")

            val platformPostId = getPlatformPostId(url) ?: return@map null

            val category = rowElement.selectFirst(".icon_keyword")?.text()

            val viewCount = rowElement.selectFirst(".hit")?.text()?.toIntOrNull()
            val commentCount = rowElement.selectFirst(".rSymph05")?.text()?.toIntOrNull()
            val likeCount = rowElement.selectFirst(".list_votes")?.apply {
                select("i").remove()
            }?.text()?.toIntOrNull()

            val title = rowElement.selectFirst(".list_subject a:first-child")?.text()
            val isEnded = title?.startsWith("종료") == true

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