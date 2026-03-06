package com.github.s8u.hotdeallist.crawler.impl

import com.github.s8u.hotdeallist.enums.PlatformType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PpomppuPpomppuCrawlerTest {

    private val crawler = PpomppuPpomppuCrawler()

    @Test
    fun `getPlatformType은 PPOMPPU_PPOMPPU을 반환한다`() {
        assertEquals(PlatformType.PPOMPPU_PPOMPPU, crawler.getPlatformType())
    }

    @Test
    fun `정상 URL에서 게시글 ID를 추출한다`() {
        val url = "https://www.ppomppu.co.kr/zboard/view.php?id=ppomppu&no=12345"
        assertEquals("12345", crawler.getPlatformPostId(url))
    }

    @Test
    fun `추가 파라미터가 있는 URL에서도 게시글 ID를 추출한다`() {
        val url = "https://www.ppomppu.co.kr/zboard/view.php?id=ppomppu&no=12345&page=2"
        assertEquals("12345&page=2", crawler.getPlatformPostId(url))
    }

    @Test
    fun `다른 게시판 id면 null을 반환한다`() {
        val url = "https://www.ppomppu.co.kr/zboard/view.php?id=other&no=12345"
        assertNull(crawler.getPlatformPostId(url))
    }

    @Test
    fun `다른 도메인 URL이면 null을 반환한다`() {
        val url = "https://other.com/zboard/view.php?id=ppomppu&no=12345"
        assertNull(crawler.getPlatformPostId(url))
    }

    @Test
    fun `빈 문자열이면 null을 반환한다`() {
        assertNull(crawler.getPlatformPostId(""))
    }
}
