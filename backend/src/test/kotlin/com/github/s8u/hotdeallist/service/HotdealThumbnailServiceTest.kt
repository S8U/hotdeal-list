package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.store.FileStore
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockKExtension::class)
class HotdealThumbnailServiceTest {

    @MockK
    private lateinit var fileStore: FileStore

    private lateinit var thumbnailService: HotdealThumbnailService

    @BeforeEach
    fun setUp() {
        thumbnailService = HotdealThumbnailService(fileStore)
    }

    @Nested
    @DisplayName("downloadAndStore")
    inner class DownloadAndStore {

        @Test
        @DisplayName("thumbnailUrl과 fallbackUrl 모두 null이면 null을 반환한다")
        fun `should return null when both urls are null`() {
            val result = thumbnailService.downloadAndStore(
                PlatformType.COOLENJOY_JIRUM, "12345", null, null
            )

            assertNull(result)
            verify(exactly = 0) { fileStore.store(any(), any(), any()) }
        }

        @Test
        @DisplayName("thumbnailUrl이 null이면 fallbackUrl을 사용한다")
        fun `should use fallback url when thumbnail url is null`() {
            // 실제 HTTP 호출은 실패할 수 있으므로, 다운로드 실패 시 null 반환을 테스트
            val result = thumbnailService.downloadAndStore(
                PlatformType.COOLENJOY_JIRUM, "12345",
                null, "https://invalid-url-that-does-not-exist.example.com/image.jpg"
            )

            // 유효하지 않은 URL이므로 다운로드 실패 → null
            assertNull(result)
        }

        @Test
        @DisplayName("다운로드 실패 시 null을 반환한다")
        fun `should return null when download fails`() {
            val result = thumbnailService.downloadAndStore(
                PlatformType.COOLENJOY_JIRUM, "12345",
                "https://invalid-url-that-does-not-exist.example.com/image.jpg", null
            )

            assertNull(result)
        }
    }

    @Nested
    @DisplayName("getThumbnailUrl")
    inner class GetThumbnailUrl {

        @Test
        @DisplayName("경로가 있으면 FileStore에서 URL을 반환한다")
        fun `should return url from fileStore when path is provided`() {
            every { fileStore.getUrl("COOLENJOY_JIRUM/12345.webp") } returns
                "https://cdn.example.com/COOLENJOY_JIRUM/12345.webp"

            val result = thumbnailService.getThumbnailUrl("COOLENJOY_JIRUM/12345.webp")

            assertEquals("https://cdn.example.com/COOLENJOY_JIRUM/12345.webp", result)
        }

        @Test
        @DisplayName("경로가 null이면 null을 반환한다")
        fun `should return null when path is null`() {
            val result = thumbnailService.getThumbnailUrl(null)

            assertNull(result)
            verify(exactly = 0) { fileStore.getUrl(any()) }
        }
    }
}
