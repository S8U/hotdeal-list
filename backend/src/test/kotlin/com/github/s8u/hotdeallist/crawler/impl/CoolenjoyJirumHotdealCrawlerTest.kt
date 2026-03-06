package com.github.s8u.hotdeallist.crawler.impl

import com.github.s8u.hotdeallist.enums.PlatformType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CoolenjoyJirumHotdealCrawlerTest {

    private val crawler = CoolenjoyJirumHotdealCrawler()

    @Test
    fun `getPlatformType은 COOLENJOY_JIRUM을 반환한다`() {
        assertEquals(PlatformType.COOLENJOY_JIRUM, crawler.getPlatformType())
    }

    @Test
    fun `정상 URL에서 게시글 ID를 추출한다`() {
        val url = "https://coolenjoy.net/bbs/jirum/12345"
        assertEquals("12345", crawler.getPlatformPostId(url))
    }

    @Test
    fun `쿼리 파라미터가 있는 URL에서 게시글 ID를 추출한다`() {
        val url = "https://coolenjoy.net/bbs/jirum/12345?page=1"
        assertEquals("12345", crawler.getPlatformPostId(url))
    }

    @Test
    fun `다른 도메인 URL이면 null을 반환한다`() {
        val url = "https://other-site.com/bbs/jirum/12345"
        assertNull(crawler.getPlatformPostId(url))
    }

    @Test
    fun `다른 게시판 URL이면 null을 반환한다`() {
        val url = "https://coolenjoy.net/bbs/other/12345"
        assertNull(crawler.getPlatformPostId(url))
    }

    @Test
    fun `빈 문자열이면 null을 반환한다`() {
        assertNull(crawler.getPlatformPostId(""))
    }
}
