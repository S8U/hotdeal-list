package com.github.s8u.hotdeallist.crawler.impl

import com.github.s8u.hotdeallist.enums.PlatformType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class QuasarzoneJirumHotdealCrawlerTest {

    private val crawler = QuasarzoneJirumHotdealCrawler()

    @Test
    fun `getPlatformType은 QUASARZONE_JIRUM을 반환한다`() {
        assertEquals(PlatformType.QUASARZONE_JIRUM, crawler.getPlatformType())
    }

    @Test
    fun `정상 URL에서 게시글 ID를 추출한다`() {
        val url = "https://quasarzone.com//bbs/qb_saleinfo/views/98765"
        assertEquals("98765", crawler.getPlatformPostId(url))
    }

    @Test
    fun `쿼리 파라미터가 있는 URL에서 게시글 ID를 추출한다`() {
        val url = "https://quasarzone.com//bbs/qb_saleinfo/views/98765?page=2"
        assertEquals("98765", crawler.getPlatformPostId(url))
    }

    @Test
    fun `다른 게시판 URL이면 null을 반환한다`() {
        val url = "https://quasarzone.com//bbs/qb_tsy/views/98765"
        assertNull(crawler.getPlatformPostId(url))
    }

    @Test
    fun `다른 도메인 URL이면 null을 반환한다`() {
        val url = "https://other.com//bbs/qb_saleinfo/views/98765"
        assertNull(crawler.getPlatformPostId(url))
    }

    @Test
    fun `빈 문자열이면 null을 반환한다`() {
        assertNull(crawler.getPlatformPostId(""))
    }
}
