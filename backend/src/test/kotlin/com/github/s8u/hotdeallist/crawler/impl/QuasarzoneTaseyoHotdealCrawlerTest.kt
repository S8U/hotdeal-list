package com.github.s8u.hotdeallist.crawler.impl

import com.github.s8u.hotdeallist.enums.PlatformType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class QuasarzoneTaseyoHotdealCrawlerTest {

    private val crawler = QuasarzoneTaseyoHotdealCrawler()

    @Test
    fun `getPlatformType은 QUASARZONE_TASEYO을 반환한다`() {
        assertEquals(PlatformType.QUASARZONE_TASEYO, crawler.getPlatformType())
    }

    @Test
    fun `정상 URL에서 게시글 ID를 추출한다`() {
        val url = "https://quasarzone.com//bbs/qb_tsy/views/11111"
        assertEquals("11111", crawler.getPlatformPostId(url))
    }

    @Test
    fun `쿼리 파라미터가 있는 URL에서 게시글 ID를 추출한다`() {
        val url = "https://quasarzone.com//bbs/qb_tsy/views/11111?page=3"
        assertEquals("11111", crawler.getPlatformPostId(url))
    }

    @Test
    fun `다른 게시판 URL이면 null을 반환한다`() {
        val url = "https://quasarzone.com//bbs/qb_saleinfo/views/11111"
        assertNull(crawler.getPlatformPostId(url))
    }

    @Test
    fun `빈 문자열이면 null을 반환한다`() {
        assertNull(crawler.getPlatformPostId(""))
    }
}
