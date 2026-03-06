package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.entity.Hotdeal
import com.github.s8u.hotdeallist.entity.HotdealProcess
import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.repository.HotdealProcessRepository
import com.github.s8u.hotdeallist.repository.HotdealRepository
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class HotdealServiceFacadeTest {

    @MockK
    private lateinit var hotdealRepository: HotdealRepository

    @MockK
    private lateinit var hotdealProcessRepository: HotdealProcessRepository

    @MockK
    private lateinit var hotdealRawService: HotdealRawService

    @MockK
    private lateinit var hotdealService: HotdealService

    @MockK
    private lateinit var hotdealProcessService: HotdealProcessService

    @InjectMockKs
    private lateinit var hotdealServiceFacade: HotdealServiceFacade

    private val now = LocalDateTime.of(2025, 1, 1, 12, 0)

    @Nested
    @DisplayName("createHotdealAllPlatforms")
    inner class CreateHotdealAllPlatforms {

        @Test
        @DisplayName("모든 플랫폼에 대해 createHotdeal을 호출한다")
        fun `should call createHotdeal for all platform types`() {
            PlatformType.values().forEach { platformType ->
                every { hotdealRawService.crawlHotdealRaw(platformType, 1, 1000L) } returns emptyMap()
            }

            hotdealServiceFacade.createHotdealAllPlatforms(1)

            PlatformType.values().forEach { platformType ->
                verify { hotdealRawService.crawlHotdealRaw(platformType, 1, 1000L) }
            }
        }
    }

    @Nested
    @DisplayName("createHotdeal")
    inner class CreateHotdeal {

        @Test
        @DisplayName("새 핫딜이면 가공 후 생성한다")
        fun `should process and create new hotdeal`() {
            val rawIds = mapOf(1L to true)

            every { hotdealRawService.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 1000L) } returns rawIds
            every { hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L) } returns null
            every { hotdealRepository.findByHotdealRawId(1L) } returns null
            every { hotdealProcessService.processHotdealFromRaw(1L) } just runs
            every { hotdealService.createHotdealFromRawAndProcess(1L) } just runs

            hotdealServiceFacade.createHotdeal(PlatformType.COOLENJOY_JIRUM, 1)

            verify { hotdealProcessService.processHotdealFromRaw(1L) }
            verify { hotdealService.createHotdealFromRawAndProcess(1L) }
        }

        @Test
        @DisplayName("기존 핫딜이면 업데이트한다")
        fun `should update existing hotdeal`() {
            val rawIds = mapOf(1L to false)
            val existingHotdeal = Hotdeal(
                hotdealRawId = 1L,
                platformType = PlatformType.COOLENJOY_JIRUM,
                platformPostId = "12345",
                url = "https://example.com",
                title = "기존 핫딜",
                wroteAt = now
            )

            every { hotdealRawService.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 1000L) } returns rawIds
            every { hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L) } returns null
            every { hotdealRepository.findByHotdealRawId(1L) } returns existingHotdeal
            every { hotdealProcessService.processHotdealFromRaw(1L) } just runs
            every { hotdealService.updateHotdealFromRaw(1L) } just runs

            hotdealServiceFacade.createHotdeal(PlatformType.COOLENJOY_JIRUM, 1)

            verify { hotdealService.updateHotdealFromRaw(1L) }
            verify(exactly = 0) { hotdealService.createHotdealFromRawAndProcess(1L) }
        }

        @Test
        @DisplayName("이미 가공 데이터가 있으면 다시 가공하지 않는다")
        fun `should skip processing when process data already exists`() {
            val rawIds = mapOf(1L to true)
            val process = HotdealProcess(
                hotdealRawId = 1L, aiModel = "m", aiPrompt = "p", aiResponse = "r",
                title = "t", titleEn = "te", productName = "pn", productNameEn = "pne",
                categoryCode = "etc", categoryConfidence = BigDecimal("0.9")
            )

            every { hotdealRawService.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 1000L) } returns rawIds
            every { hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L) } returns process
            every { hotdealRepository.findByHotdealRawId(1L) } returns null
            every { hotdealService.createHotdealFromRawAndProcess(1L) } just runs

            hotdealServiceFacade.createHotdeal(PlatformType.COOLENJOY_JIRUM, 1)

            verify(exactly = 0) { hotdealProcessService.processHotdealFromRaw(any()) }
            verify { hotdealService.createHotdealFromRawAndProcess(1L) }
        }

        @Test
        @DisplayName("크롤링 결과가 비어있으면 아무 작업도 하지 않는다")
        fun `should do nothing when no crawl results`() {
            every { hotdealRawService.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 1000L) } returns emptyMap()

            hotdealServiceFacade.createHotdeal(PlatformType.COOLENJOY_JIRUM, 1)

            verify(exactly = 0) { hotdealProcessService.processHotdealFromRaw(any()) }
            verify(exactly = 0) { hotdealService.createHotdealFromRawAndProcess(any()) }
            verify(exactly = 0) { hotdealService.updateHotdealFromRaw(any()) }
        }

        @Test
        @DisplayName("여러 rawId를 순차적으로 처리한다")
        fun `should process multiple raw ids sequentially`() {
            val rawIds = mapOf(1L to true, 2L to false, 3L to true)

            every { hotdealRawService.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 1000L) } returns rawIds

            // rawId 1: 새 핫딜
            every { hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L) } returns null
            every { hotdealRepository.findByHotdealRawId(1L) } returns null
            every { hotdealProcessService.processHotdealFromRaw(1L) } just runs
            every { hotdealService.createHotdealFromRawAndProcess(1L) } just runs

            // rawId 2: 기존 핫딜
            every { hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(2L) } returns null
            val existingHotdeal = Hotdeal(
                hotdealRawId = 2L, platformType = PlatformType.COOLENJOY_JIRUM,
                platformPostId = "222", url = "https://example.com/2", title = "기존", wroteAt = now
            )
            every { hotdealRepository.findByHotdealRawId(2L) } returns existingHotdeal
            every { hotdealProcessService.processHotdealFromRaw(2L) } just runs
            every { hotdealService.updateHotdealFromRaw(2L) } just runs

            // rawId 3: 새 핫딜
            every { hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(3L) } returns null
            every { hotdealRepository.findByHotdealRawId(3L) } returns null
            every { hotdealProcessService.processHotdealFromRaw(3L) } just runs
            every { hotdealService.createHotdealFromRawAndProcess(3L) } just runs

            hotdealServiceFacade.createHotdeal(PlatformType.COOLENJOY_JIRUM, 1)

            verify { hotdealService.createHotdealFromRawAndProcess(1L) }
            verify { hotdealService.updateHotdealFromRaw(2L) }
            verify { hotdealService.createHotdealFromRawAndProcess(3L) }
        }
    }
}
