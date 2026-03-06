package com.github.s8u.hotdeallist.enums

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlatformTypeTest {

    @Test
    fun `모든 플랫폼 타입이 6개 존재한다`() {
        assertEquals(6, PlatformType.values().size)
    }

    @Test
    fun `각 플랫폼 타입은 displayName을 가진다`() {
        assertEquals("쿨엔조이 지름", PlatformType.COOLENJOY_JIRUM.displayName)
        assertEquals("퀘이사존 지름", PlatformType.QUASARZONE_JIRUM.displayName)
        assertEquals("퀘이사존 타세요", PlatformType.QUASARZONE_TASEYO.displayName)
        assertEquals("루리웹 핫딜", PlatformType.RULIWEB_HOTDEAL.displayName)
        assertEquals("뽐뿌 뽐뿌게시판", PlatformType.PPOMPPU_PPOMPPU.displayName)
        assertEquals("클리앙 알뜰구매", PlatformType.CLIEN_ALTTEUL.displayName)
    }

    @Test
    fun `valueOf로 플랫폼 타입을 조회할 수 있다`() {
        assertEquals(PlatformType.COOLENJOY_JIRUM, PlatformType.valueOf("COOLENJOY_JIRUM"))
        assertEquals(PlatformType.QUASARZONE_JIRUM, PlatformType.valueOf("QUASARZONE_JIRUM"))
    }

    @Test
    fun `잘못된 이름으로 valueOf 호출 시 IllegalArgumentException이 발생한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            PlatformType.valueOf("INVALID_PLATFORM")
        }
    }
}
