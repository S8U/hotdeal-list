package com.github.s8u.hotdeallist.crawler.impl

import com.github.s8u.hotdeallist.enums.PlatformType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RuliwebHotdealHotdealCrawlerTest {

    private val crawler = RuliwebHotdealHotdealCrawler()

    @Test
    fun `getPlatformType은 RULIWEB_HOTDEAL을 반환한다`() {
        assertEquals(PlatformType.RULIWEB_HOTDEAL, crawler.getPlatformType())
    }

    @Test
    fun `정상 URL에서 게시글 ID를 추출한다`() {
        val url = "https://bbs.ruliweb.com/market/board/1020/read/55555"
        assertEquals("55555", crawler.getPlatformPostId(url))
    }

    @Test
    fun `쿼리 파라미터가 있는 URL에서 게시글 ID를 추출한다`() {
        val url = "https://bbs.ruliweb.com/market/board/1020/read/55555?page=1"
        assertEquals("55555", crawler.getPlatformPostId(url))
    }

    @Test
    fun `다른 게시판 URL이면 null을 반환한다`() {
        val url = "https://bbs.ruliweb.com/market/board/9999/read/55555"
        assertNull(crawler.getPlatformPostId(url))
    }

    @Test
    fun `다른 도메인 URL이면 null을 반환한다`() {
        val url = "https://other.com/market/board/1020/read/55555"
        assertNull(crawler.getPlatformPostId(url))
    }

    @Test
    fun `빈 문자열이면 null을 반환한다`() {
        assertNull(crawler.getPlatformPostId(""))
    }
}
