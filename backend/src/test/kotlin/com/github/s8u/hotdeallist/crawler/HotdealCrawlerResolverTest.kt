package com.github.s8u.hotdeallist.crawler

import com.github.s8u.hotdeallist.enums.PlatformType
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockKExtension::class)
class HotdealCrawlerResolverTest {

    @MockK
    private lateinit var coolEnjoyCrawler: HotdealCrawler

    @MockK
    private lateinit var ppomppuCrawler: HotdealCrawler

    @Test
    @DisplayName("플랫폼 타입에 맞는 크롤러를 반환한다")
    fun `should return matching crawler for platform type`() {
        every { coolEnjoyCrawler.getPlatformType() } returns PlatformType.COOLENJOY_JIRUM
        every { ppomppuCrawler.getPlatformType() } returns PlatformType.PPOMPPU_PPOMPPU

        val resolver = HotdealCrawlerResolver(listOf(coolEnjoyCrawler, ppomppuCrawler))

        val result = resolver.getByPlatformType(PlatformType.COOLENJOY_JIRUM)

        assertEquals(coolEnjoyCrawler, result)
    }

    @Test
    @DisplayName("매칭되는 크롤러가 없으면 null을 반환한다")
    fun `should return null when no matching crawler`() {
        every { coolEnjoyCrawler.getPlatformType() } returns PlatformType.COOLENJOY_JIRUM

        val resolver = HotdealCrawlerResolver(listOf(coolEnjoyCrawler))

        val result = resolver.getByPlatformType(PlatformType.QUASARZONE_JIRUM)

        assertNull(result)
    }

    @Test
    @DisplayName("크롤러 목록이 비어있으면 null을 반환한다")
    fun `should return null when crawler list is empty`() {
        val resolver = HotdealCrawlerResolver(emptyList())

        val result = resolver.getByPlatformType(PlatformType.COOLENJOY_JIRUM)

        assertNull(result)
    }

    @Test
    @DisplayName("모든 플랫폼 타입에 대해 크롤러를 찾을 수 있다")
    fun `should find crawler for each registered platform type`() {
        val crawlers = PlatformType.values().map { platformType ->
            val crawler = io.mockk.mockk<HotdealCrawler>()
            every { crawler.getPlatformType() } returns platformType
            crawler
        }

        val resolver = HotdealCrawlerResolver(crawlers)

        PlatformType.values().forEach { platformType ->
            val result = resolver.getByPlatformType(platformType)
            assertEquals(platformType, result?.getPlatformType())
        }
    }
}
