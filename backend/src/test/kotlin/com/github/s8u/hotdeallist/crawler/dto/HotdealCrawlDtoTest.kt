package com.github.s8u.hotdeallist.crawler.dto

import com.github.s8u.hotdeallist.enums.PlatformType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class HotdealCrawlDtoTest {

    // --- HotdealCrawlListItemDto 테스트 ---

    @Test
    fun `HotdealCrawlListItemDto 기본값 테스트`() {
        val item = HotdealCrawlListItemDto(
            url = "https://test.com/1",
            platformPostId = "1"
        )

        assertNull(item.category)
        assertNull(item.viewCount)
        assertNull(item.commentCount)
        assertNull(item.likeCount)
        assertFalse(item.isEnded)
    }

    @Test
    fun `HotdealCrawlListItemDto 모든 필드 설정 테스트`() {
        val item = HotdealCrawlListItemDto(
            url = "https://test.com/1",
            platformPostId = "1",
            category = "전자",
            viewCount = 100,
            commentCount = 10,
            likeCount = 5,
            isEnded = true
        )

        assertEquals("https://test.com/1", item.url)
        assertEquals("1", item.platformPostId)
        assertEquals("전자", item.category)
        assertEquals(100, item.viewCount)
        assertEquals(10, item.commentCount)
        assertEquals(5, item.likeCount)
        assertTrue(item.isEnded)
    }

    // --- HotdealCrawlListDto 테스트 ---

    @Test
    fun `HotdealCrawlListDto 빈 목록 테스트`() {
        val dto = HotdealCrawlListDto(
            isSuccess = true,
            count = 0,
            maxCount = 25,
            items = emptyList()
        )

        assertTrue(dto.isSuccess)
        assertEquals(0, dto.count)
        assertEquals(25, dto.maxCount)
        assertTrue(dto.items.isEmpty())
    }

    @Test
    fun `HotdealCrawlListDto 아이템 포함 테스트`() {
        val items = listOf(
            HotdealCrawlListItemDto(url = "https://test.com/1", platformPostId = "1"),
            HotdealCrawlListItemDto(url = "https://test.com/2", platformPostId = "2")
        )
        val dto = HotdealCrawlListDto(
            isSuccess = true,
            count = 2,
            maxCount = 25,
            items = items
        )

        assertEquals(2, dto.items.size)
        assertEquals("1", dto.items[0].platformPostId)
        assertEquals("2", dto.items[1].platformPostId)
    }

    // --- HotdealCrawlDetailDto 테스트 ---

    @Test
    fun `HotdealCrawlDetailDto 기본값 테스트`() {
        val now = LocalDateTime.now()
        val dto = HotdealCrawlDetailDto(
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "100",
            url = "https://coolenjoy.net/bbs/jirum/100",
            title = "테스트",
            wroteAt = now
        )

        assertNull(dto.category)
        assertNull(dto.contentHtml)
        assertNull(dto.price)
        assertEquals("KRW", dto.currencyUnit)
        assertEquals(0, dto.viewCount)
        assertEquals(0, dto.commentCount)
        assertEquals(0, dto.likeCount)
        assertFalse(dto.isEnded)
        assertNull(dto.sourceUrl)
        assertNull(dto.thumbnailImageUrl)
        assertNull(dto.firstImageUrl)
    }

    @Test
    fun `HotdealCrawlDetailDto 모든 필드 설정 테스트`() {
        val now = LocalDateTime.now()
        val dto = HotdealCrawlDetailDto(
            platformType = PlatformType.QUASARZONE_JIRUM,
            platformPostId = "200",
            url = "https://quasarzone.com//bbs/qb_saleinfo/views/200",
            title = "RTX 5090 최저가",
            category = "그래픽카드",
            contentHtml = "<p>내용</p>",
            price = BigDecimal("2500000"),
            currencyUnit = "KRW",
            viewCount = 5000,
            commentCount = 100,
            likeCount = 50,
            isEnded = false,
            sourceUrl = "https://shopping.com/product/123",
            thumbnailImageUrl = "https://img.com/thumb.jpg",
            firstImageUrl = "https://img.com/first.jpg",
            wroteAt = now
        )

        assertEquals(PlatformType.QUASARZONE_JIRUM, dto.platformType)
        assertEquals("200", dto.platformPostId)
        assertEquals("RTX 5090 최저가", dto.title)
        assertEquals("그래픽카드", dto.category)
        assertEquals(BigDecimal("2500000"), dto.price)
        assertEquals(5000, dto.viewCount)
    }

    // --- data class equals/copy 테스트 ---

    @Test
    fun `HotdealCrawlListItemDto data class equality 테스트`() {
        val item1 = HotdealCrawlListItemDto(url = "https://test.com/1", platformPostId = "1")
        val item2 = HotdealCrawlListItemDto(url = "https://test.com/1", platformPostId = "1")

        assertEquals(item1, item2)
        assertEquals(item1.hashCode(), item2.hashCode())
    }

    @Test
    fun `HotdealCrawlListItemDto copy 테스트`() {
        val item = HotdealCrawlListItemDto(url = "https://test.com/1", platformPostId = "1", viewCount = 10)
        val copied = item.copy(viewCount = 100)

        assertEquals(10, item.viewCount)
        assertEquals(100, copied.viewCount)
        assertEquals(item.url, copied.url)
    }
}
