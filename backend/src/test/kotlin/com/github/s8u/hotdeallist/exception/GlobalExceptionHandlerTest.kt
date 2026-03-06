package com.github.s8u.hotdeallist.exception

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    @DisplayName("BusinessException 발생 시 해당 상태코드와 메시지를 반환한다")
    fun `should return business exception status and message`() {
        val exception = BusinessException("핫딜을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST)

        val response = handler.handleBusinessException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("핫딜을 찾을 수 없습니다.", response.body?.message)
        assertEquals(400, response.body?.status)
    }

    @Test
    @DisplayName("BusinessException에 NOT_FOUND 상태를 전달하면 404를 반환한다")
    fun `should return 404 when business exception has NOT_FOUND status`() {
        val exception = BusinessException("리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND)

        val response = handler.handleBusinessException(exception)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(404, response.body?.status)
    }

    @Test
    @DisplayName("BusinessException의 기본 상태코드는 BAD_REQUEST이다")
    fun `should default to BAD_REQUEST status`() {
        val exception = BusinessException("잘못된 요청입니다.")

        val response = handler.handleBusinessException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    @DisplayName("일반 Exception 발생 시 500 에러를 반환한다")
    fun `should return 500 for generic exception`() {
        val exception = RuntimeException("Unexpected error")

        val response = handler.handleException(exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("오류가 발생했습니다. 잠시 후에 다시 시도하세요.", response.body?.message)
        assertEquals(500, response.body?.status)
    }

    @Test
    @DisplayName("NullPointerException 등 일반 예외도 500으로 처리한다")
    fun `should handle NullPointerException as 500`() {
        val exception = NullPointerException("null ref")

        val response = handler.handleException(exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals(500, response.body?.status)
    }
}
