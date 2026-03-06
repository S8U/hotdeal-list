package com.github.s8u.hotdeallist.crawler.impl

import com.github.s8u.hotdeallist.enums.PlatformType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ClienAltteulHotdealCrawlerTest {

    private val crawler = ClienAltteulHotdealCrawler()

    @Test
    fun `getPlatformType은 CLIEN_ALTTEUL을 반환한다`() {
        assertEquals(PlatformType.CLIEN_ALTTEUL, crawler.getPlatformType())
    }

    @Test
    fun `정상 URL에서 게시글 ID를 추출한다`() {
        val url = "https://www.clien.net/service/board/jirum/18888888"
        assertEquals("18888888", crawler.getPlatformPostId(url))
    }

    @Test
    fun `쿼리 파라미터가 있는 URL에서 게시글 ID를 추출한다`() {
        val url = "https://www.clien.net/service/board/jirum/18888888?type=recommend"
        assertEquals("18888888", crawler.getPlatformPostId(url))
    }

    @Test
    fun `다른 게시판 URL이면 null을 반환한다`() {
        val url = "https://www.clien.net/service/board/park/18888888"
        assertNull(crawler.getPlatformPostId(url))
    }

    @Test
    fun `다른 도메인 URL이면 null을 반환한다`() {
        val url = "https://other.net/service/board/jirum/18888888"
        assertNull(crawler.getPlatformPostId(url))
    }

    @Test
    fun `빈 문자열이면 null을 반환한다`() {
        assertNull(crawler.getPlatformPostId(""))
    }
}
