package com.github.s8u.hotdeallist.exception

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ErrorResponseTest {

    @Test
    fun `ErrorResponse 생성 테스트`() {
        val response = ErrorResponse("에러 메시지", 400)

        assertEquals("에러 메시지", response.message)
        assertEquals(400, response.status)
    }

    @Test
    fun `data class equality 테스트`() {
        val response1 = ErrorResponse("에러", 500)
        val response2 = ErrorResponse("에러", 500)

        assertEquals(response1, response2)
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun `data class copy 테스트`() {
        val response = ErrorResponse("원본 에러", 400)
        val copied = response.copy(status = 500)

        assertEquals("원본 에러", copied.message)
        assertEquals(500, copied.status)
    }
}
