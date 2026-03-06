package com.github.s8u.hotdeallist.crawler

import com.github.s8u.hotdeallist.enums.PlatformType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HotdealCrawlingExceptionTest {

    @Test
    fun `기본값으로 생성할 수 있다`() {
        val exception = HotdealCrawlingException()

        assertNull(exception.message)
        assertNull(exception.platformType)
        assertNull(exception.url)
        assertNull(exception.html)
        assertNull(exception.cause)
    }

    @Test
    fun `모든 필드를 설정하여 생성할 수 있다`() {
        val cause = RuntimeException("original error")
        val exception = HotdealCrawlingException(
            message = "파싱 실패",
            platformType = PlatformType.COOLENJOY_JIRUM,
            url = "https://coolenjoy.net/bbs/jirum/100",
            html = "<html>...</html>",
            cause = cause
        )

        assertEquals("파싱 실패", exception.message)
        assertEquals(PlatformType.COOLENJOY_JIRUM, exception.platformType)
        assertEquals("https://coolenjoy.net/bbs/jirum/100", exception.url)
        assertEquals("<html>...</html>", exception.html)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `RuntimeException을 상속한다`() {
        val exception = HotdealCrawlingException("error")

        assertTrue(exception is RuntimeException)
    }
}
