package com.github.s8u.hotdeallist.exception

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class BusinessExceptionTest {

    @Test
    fun `기본 상태코드는 BAD_REQUEST이다`() {
        val exception = BusinessException("에러 메시지")

        assertEquals("에러 메시지", exception.message)
        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
    }

    @Test
    fun `커스텀 상태코드를 지정할 수 있다`() {
        val exception = BusinessException("찾을 수 없음", HttpStatus.NOT_FOUND)

        assertEquals("찾을 수 없음", exception.message)
        assertEquals(HttpStatus.NOT_FOUND, exception.status)
    }

    @Test
    fun `RuntimeException을 상속한다`() {
        val exception = BusinessException("에러")

        assertTrue(exception is RuntimeException)
    }
}
