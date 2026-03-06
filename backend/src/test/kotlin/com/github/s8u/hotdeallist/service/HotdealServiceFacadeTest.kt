package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.entity.Hotdeal
import com.github.s8u.hotdeallist.entity.HotdealProcess
import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.repository.HotdealProcessRepository
import com.github.s8u.hotdeallist.repository.HotdealRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class HotdealServiceFacadeTest {

    @Mock
    lateinit var hotdealRepository: HotdealRepository

    @Mock
    lateinit var hotdealProcessRepository: HotdealProcessRepository

    @Mock
    lateinit var hotdealRawService: HotdealRawService

    @Mock
    lateinit var hotdealService: HotdealService

    @Mock
    lateinit var hotdealProcessService: HotdealProcessService

    private lateinit var facade: HotdealServiceFacade

    @BeforeEach
    fun setUp() {
        facade = HotdealServiceFacade(
            hotdealRepository,
            hotdealProcessRepository,
            hotdealRawService,
            hotdealService,
            hotdealProcessService
        )
    }

    @Test
    fun `createHotdealAllPlatforms는 모든 플랫폼에 대해 createHotdeal을 호출한다`() {
        PlatformType.values().forEach { type ->
            whenever(hotdealRawService.crawlHotdealRaw(eq(type), eq(1), any())).thenReturn(emptyMap())
        }

        facade.createHotdealAllPlatforms(1, 0L)

        PlatformType.values().forEach { type ->
            verify(hotdealRawService).crawlHotdealRaw(eq(type), eq(1), any())
        }
    }

    @Test
    fun `신규 핫딜은 AI 가공 후 생성한다`() {
        val rawIds = mapOf(1L to true)

        whenever(hotdealRawService.crawlHotdealRaw(any(), any(), any())).thenReturn(rawIds)
        whenever(hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L)).thenReturn(null)
        whenever(hotdealRepository.findByHotdealRawId(1L)).thenReturn(null)

        facade.createHotdeal(PlatformType.COOLENJOY_JIRUM, 1, 0L)

        verify(hotdealProcessService).processHotdealFromRaw(1L)
        verify(hotdealService).createHotdealFromRawAndProcess(1L)
        verify(hotdealService, never()).updateHotdealFromRaw(any())
    }

    @Test
    fun `기존 핫딜은 업데이트한다`() {
        val rawIds = mapOf(5L to false)
        val existingHotdeal = Hotdeal(
            hotdealRawId = 5L,
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "100",
            url = "test",
            title = "test",
            wroteAt = LocalDateTime.now()
        )

        whenever(hotdealRawService.crawlHotdealRaw(any(), any(), any())).thenReturn(rawIds)
        whenever(hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(5L)).thenReturn(null)
        whenever(hotdealRepository.findByHotdealRawId(5L)).thenReturn(existingHotdeal)

        facade.createHotdeal(PlatformType.COOLENJOY_JIRUM, 1, 0L)

        verify(hotdealProcessService).processHotdealFromRaw(5L)
        verify(hotdealService).updateHotdealFromRaw(5L)
        verify(hotdealService, never()).createHotdealFromRawAndProcess(any())
    }

    @Test
    fun `이미 가공 데이터가 있으면 AI 처리를 건너뛴다`() {
        val rawIds = mapOf(1L to true)
        val existingProcess = HotdealProcess(
            hotdealRawId = 1L,
            aiModel = "test",
            aiPrompt = "test",
            aiResponse = "test",
            title = "test",
            titleEn = "test",
            productName = "test",
            productNameEn = "test",
            categoryCode = "etc",
            categoryConfidence = BigDecimal("0.5")
        )

        whenever(hotdealRawService.crawlHotdealRaw(any(), any(), any())).thenReturn(rawIds)
        whenever(hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L)).thenReturn(existingProcess)
        whenever(hotdealRepository.findByHotdealRawId(1L)).thenReturn(null)

        facade.createHotdeal(PlatformType.COOLENJOY_JIRUM, 1, 0L)

        verify(hotdealProcessService, never()).processHotdealFromRaw(any())
        verify(hotdealService).createHotdealFromRawAndProcess(1L)
    }

    @Test
    fun `이미 핫딜이 존재하면 업데이트한다`() {
        val rawIds = mapOf(1L to true)
        val existingProcess = HotdealProcess(
            hotdealRawId = 1L,
            aiModel = "test",
            aiPrompt = "test",
            aiResponse = "test",
            title = "test",
            titleEn = "test",
            productName = "test",
            productNameEn = "test",
            categoryCode = "etc",
            categoryConfidence = BigDecimal("0.5")
        )
        val existingHotdeal = Hotdeal(
            hotdealRawId = 1L,
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "100",
            url = "test",
            title = "test",
            wroteAt = LocalDateTime.now()
        )

        whenever(hotdealRawService.crawlHotdealRaw(any(), any(), any())).thenReturn(rawIds)
        whenever(hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L)).thenReturn(existingProcess)
        whenever(hotdealRepository.findByHotdealRawId(1L)).thenReturn(existingHotdeal)

        facade.createHotdeal(PlatformType.COOLENJOY_JIRUM, 1, 0L)

        verify(hotdealService).updateHotdealFromRaw(1L)
        verify(hotdealService, never()).createHotdealFromRawAndProcess(any())
    }

    @Test
    fun `크롤링 결과가 비어있으면 아무 처리도 하지 않는다`() {
        whenever(hotdealRawService.crawlHotdealRaw(any(), any(), any())).thenReturn(emptyMap())

        facade.createHotdeal(PlatformType.COOLENJOY_JIRUM, 1, 0L)

        verify(hotdealProcessService, never()).processHotdealFromRaw(any())
        verify(hotdealService, never()).createHotdealFromRawAndProcess(any())
        verify(hotdealService, never()).updateHotdealFromRaw(any())
    }
}
