package com.github.s8u.hotdeallist.crawler

import com.github.s8u.hotdeallist.crawler.dto.HotdealCrawlDetailDto
import com.github.s8u.hotdeallist.crawler.dto.HotdealCrawlListDto
import com.github.s8u.hotdeallist.enums.PlatformType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class HotdealCrawlerResolverTest {

    private fun createFakeCrawler(type: PlatformType): HotdealCrawler {
        return object : HotdealCrawler {
            override fun getPlatformType() = type
            override fun getPlatformPostId(url: String): String? = null
            override fun crawlList(page: Int) = HotdealCrawlListDto(true, 0, 0, emptyList())
            override fun crawlDetail(url: String) = HotdealCrawlDetailDto(
                platformType = type, platformPostId = "", url = "", title = "", wroteAt = LocalDateTime.now()
            )
        }
    }

    @Test
    fun `등록된 플랫폼 타입으로 크롤러를 조회하면 해당 크롤러를 반환한다`() {
        val coolenjoy = createFakeCrawler(PlatformType.COOLENJOY_JIRUM)
        val quasarzone = createFakeCrawler(PlatformType.QUASARZONE_JIRUM)
        val resolver = HotdealCrawlerResolver(listOf(coolenjoy, quasarzone))

        val result = resolver.getByPlatformType(PlatformType.COOLENJOY_JIRUM)

        assertNotNull(result)
        assertEquals(PlatformType.COOLENJOY_JIRUM, result!!.getPlatformType())
    }

    @Test
    fun `등록되지 않은 플랫폼 타입으로 조회하면 null을 반환한다`() {
        val coolenjoy = createFakeCrawler(PlatformType.COOLENJOY_JIRUM)
        val resolver = HotdealCrawlerResolver(listOf(coolenjoy))

        val result = resolver.getByPlatformType(PlatformType.RULIWEB_HOTDEAL)

        assertNull(result)
    }

    @Test
    fun `빈 크롤러 목록에서 조회하면 null을 반환한다`() {
        val resolver = HotdealCrawlerResolver(emptyList())

        val result = resolver.getByPlatformType(PlatformType.COOLENJOY_JIRUM)

        assertNull(result)
    }

    @Test
    fun `모든 플랫폼 타입에 대해 각각 올바른 크롤러를 반환한다`() {
        val crawlers = PlatformType.values().map { createFakeCrawler(it) }
        val resolver = HotdealCrawlerResolver(crawlers)

        PlatformType.values().forEach { type ->
            val result = resolver.getByPlatformType(type)
            assertNotNull(result)
            assertEquals(type, result!!.getPlatformType())
        }
    }
}
