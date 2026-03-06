package com.github.s8u.hotdeallist.exception

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `BusinessException 처리 시 해당 상태코드와 메시지를 반환한다`() {
        val exception = BusinessException("잘못된 요청입니다.", HttpStatus.BAD_REQUEST)

        val response = handler.handleBusinessException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("잘못된 요청입니다.", response.body?.message)
        assertEquals(400, response.body?.status)
    }

    @Test
    fun `BusinessException의 기본 상태코드는 BAD_REQUEST이다`() {
        val exception = BusinessException("에러")

        val response = handler.handleBusinessException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `BusinessException에 NOT_FOUND 상태코드를 지정할 수 있다`() {
        val exception = BusinessException("찾을 수 없습니다.", HttpStatus.NOT_FOUND)

        val response = handler.handleBusinessException(exception)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(404, response.body?.status)
    }

    @Test
    fun `일반 Exception 처리 시 500 상태코드와 일반 메시지를 반환한다`() {
        val exception = RuntimeException("unexpected error")

        val response = handler.handleException(exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("오류가 발생했습니다. 잠시 후에 다시 시도하세요.", response.body?.message)
        assertEquals(500, response.body?.status)
    }

    @Test
    fun `일반 Exception은 원본 메시지를 노출하지 않는다`() {
        val exception = RuntimeException("DB connection failed with password=secret")

        val response = handler.handleException(exception)

        assertFalse(response.body?.message?.contains("secret") ?: true)
    }
}
