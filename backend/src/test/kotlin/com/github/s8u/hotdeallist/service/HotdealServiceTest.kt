package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.entity.*
import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.exception.BusinessException
import com.github.s8u.hotdeallist.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class HotdealServiceTest {

    @Mock
    lateinit var hotdealRepository: HotdealRepository

    @Mock
    lateinit var hotdealCategoryRepository: HotdealCategoryRepository

    @Mock
    lateinit var hotdealRawRepository: HotdealRawRepository

    @Mock
    lateinit var hotdealProcessRepository: HotdealProcessRepository

    @Mock
    lateinit var categoryRepository: CategoryRepository

    private lateinit var service: HotdealService

    private val now = LocalDateTime.now()

    @BeforeEach
    fun setUp() {
        service = HotdealService(
            hotdealRepository,
            hotdealCategoryRepository,
            hotdealRawRepository,
            hotdealProcessRepository,
            categoryRepository
        )
    }

    private fun createHotdealRaw(id: Long): HotdealRaw {
        val raw = HotdealRaw(
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "100",
            url = "https://coolenjoy.net/bbs/jirum/100",
            title = "테스트 핫딜",
            price = BigDecimal("15900"),
            currencyUnit = "KRW",
            viewCount = 100,
            commentCount = 10,
            likeCount = 5,
            isEnded = false,
            sourceUrl = "https://shopping.com/product",
            wroteAt = now
        )
        setEntityId(raw, id)
        return raw
    }

    private fun createHotdealProcess(rawId: Long): HotdealProcess {
        return HotdealProcess(
            hotdealRawId = rawId,
            aiModel = "openai/gpt-oss-120b:free",
            aiPrompt = "test prompt",
            aiResponse = "test response",
            title = "테스트 핫딜",
            titleEn = "Test Hotdeal",
            productName = "테스트 상품",
            productNameEn = "Test Product",
            categoryCode = "smartphone",
            categoryConfidence = BigDecimal("0.97"),
            shoppingPlatform = "쿠팡",
            price = BigDecimal("15900"),
            currencyUnit = "KRW"
        )
    }

    private fun createCategory(id: Long, code: String, parentId: Long? = null): Category {
        val category = Category(
            parentId = parentId,
            code = code,
            name = code,
            depth = if (parentId == null) 0 else 1
        )
        setEntityId(category, id)
        return category
    }

    private fun setEntityId(entity: Any, id: Long) {
        entity.javaClass.superclass.getDeclaredField("id").apply {
            isAccessible = true
            set(entity, id)
        }
    }

    // --- createHotdealFromRawAndProcess 테스트 ---

    @Test
    fun `존재하지 않는 rawId로 생성 시 BusinessException을 발생시킨다`() {
        whenever(hotdealRawRepository.findById(999L)).thenReturn(Optional.empty())

        val exception = assertThrows(BusinessException::class.java) {
            service.createHotdealFromRawAndProcess(999L)
        }
        assertEquals("핫딜 원본 데이터를 찾을 수 없습니다.", exception.message)
    }

    @Test
    fun `가공 데이터 없이도 핫딜을 생성한다`() {
        val raw = createHotdealRaw(1L)

        whenever(hotdealRawRepository.findById(1L)).thenReturn(Optional.of(raw))
        whenever(hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L)).thenReturn(null)
        whenever(hotdealRepository.save(any<Hotdeal>())).thenAnswer { invocation ->
            val hotdeal = invocation.getArgument<Hotdeal>(0)
            setEntityId(hotdeal, 10L)
            hotdeal
        }

        service.createHotdealFromRawAndProcess(1L)

        verify(hotdealRepository).save(argThat<Hotdeal> { hotdeal ->
            hotdeal.hotdealRawId == 1L &&
            hotdeal.title == "테스트 핫딜" &&
            hotdeal.titleEn == null &&
            hotdeal.productName == null &&
            hotdeal.price == BigDecimal("15900")
        })
        verify(hotdealCategoryRepository, never()).saveAll(any<List<HotdealCategory>>())
    }

    @Test
    fun `가공 데이터와 함께 핫딜 및 카테고리 계층을 생성한다`() {
        val raw = createHotdealRaw(1L)
        val process = createHotdealProcess(1L)
        val smartphoneCategory = createCategory(10L, "smartphone", parentId = 5L)
        val mobileCategory = createCategory(5L, "mobile", parentId = 1L)
        val electronicsCategory = createCategory(1L, "electronics")

        whenever(hotdealRawRepository.findById(1L)).thenReturn(Optional.of(raw))
        whenever(hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L)).thenReturn(process)
        whenever(hotdealRepository.save(any<Hotdeal>())).thenAnswer { invocation ->
            val hotdeal = invocation.getArgument<Hotdeal>(0)
            setEntityId(hotdeal, 10L)
            hotdeal
        }
        whenever(categoryRepository.findByCode("smartphone")).thenReturn(smartphoneCategory)
        whenever(categoryRepository.findById(5L)).thenReturn(Optional.of(mobileCategory))
        whenever(categoryRepository.findById(1L)).thenReturn(Optional.of(electronicsCategory))

        service.createHotdealFromRawAndProcess(1L)

        verify(hotdealRepository).save(argThat<Hotdeal> { hotdeal ->
            hotdeal.titleEn == "Test Hotdeal" &&
            hotdeal.productName == "테스트 상품" &&
            hotdeal.productNameEn == "Test Product"
        })
        verify(hotdealCategoryRepository).saveAll(argThat<List<HotdealCategory>> { categories ->
            categories.size == 3 &&
            categories.any { it.categoryId == 10L } &&
            categories.any { it.categoryId == 5L } &&
            categories.any { it.categoryId == 1L }
        })
    }

    @Test
    fun `존재하지 않는 카테고리 코드면 BusinessException을 발생시킨다`() {
        val raw = createHotdealRaw(1L)
        val process = createHotdealProcess(1L)

        whenever(hotdealRawRepository.findById(1L)).thenReturn(Optional.of(raw))
        whenever(hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L)).thenReturn(process)
        whenever(hotdealRepository.save(any<Hotdeal>())).thenAnswer { invocation ->
            val hotdeal = invocation.getArgument<Hotdeal>(0)
            setEntityId(hotdeal, 10L)
            hotdeal
        }
        whenever(categoryRepository.findByCode("smartphone")).thenReturn(null)

        val exception = assertThrows(BusinessException::class.java) {
            service.createHotdealFromRawAndProcess(1L)
        }
        assertEquals("카테고리를 찾을 수 없습니다.", exception.message)
    }

    @Test
    fun `raw에 가격이 없으면 process 가격을 사용한다`() {
        val raw = HotdealRaw(
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "100",
            url = "https://coolenjoy.net/bbs/jirum/100",
            title = "테스트 핫딜",
            price = null,
            wroteAt = now
        )
        setEntityId(raw, 1L)

        val process = createHotdealProcess(1L)
        val category = createCategory(10L, "smartphone")

        whenever(hotdealRawRepository.findById(1L)).thenReturn(Optional.of(raw))
        whenever(hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L)).thenReturn(process)
        whenever(hotdealRepository.save(any<Hotdeal>())).thenAnswer { invocation ->
            val hotdeal = invocation.getArgument<Hotdeal>(0)
            setEntityId(hotdeal, 10L)
            hotdeal
        }
        whenever(categoryRepository.findByCode("smartphone")).thenReturn(category)

        service.createHotdealFromRawAndProcess(1L)

        verify(hotdealRepository).save(argThat<Hotdeal> { hotdeal ->
            hotdeal.price == BigDecimal("15900")
        })
    }

    @Test
    fun `raw와 process 모두 가격이 없으면 ZERO를 사용한다`() {
        val raw = HotdealRaw(
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "100",
            url = "https://coolenjoy.net/bbs/jirum/100",
            title = "테스트 핫딜",
            price = null,
            wroteAt = now
        )
        setEntityId(raw, 1L)

        whenever(hotdealRawRepository.findById(1L)).thenReturn(Optional.of(raw))
        whenever(hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L)).thenReturn(null)
        whenever(hotdealRepository.save(any<Hotdeal>())).thenAnswer { invocation ->
            val hotdeal = invocation.getArgument<Hotdeal>(0)
            setEntityId(hotdeal, 10L)
            hotdeal
        }

        service.createHotdealFromRawAndProcess(1L)

        verify(hotdealRepository).save(argThat<Hotdeal> { hotdeal ->
            hotdeal.price == BigDecimal.ZERO
        })
    }

    // --- updateHotdealFromRaw 테스트 ---

    @Test
    fun `업데이트 시 존재하지 않는 rawId면 BusinessException을 발생시킨다`() {
        whenever(hotdealRawRepository.findById(999L)).thenReturn(Optional.empty())

        val exception = assertThrows(BusinessException::class.java) {
            service.updateHotdealFromRaw(999L)
        }
        assertEquals("핫딜 원본 데이터를 찾을 수 없습니다.", exception.message)
    }

    @Test
    fun `업데이트 시 핫딜이 존재하지 않으면 BusinessException을 발생시킨다`() {
        val raw = createHotdealRaw(1L)

        whenever(hotdealRawRepository.findById(1L)).thenReturn(Optional.of(raw))
        whenever(hotdealRepository.findByHotdealRawId(1L)).thenReturn(null)

        val exception = assertThrows(BusinessException::class.java) {
            service.updateHotdealFromRaw(1L)
        }
        assertEquals("핫딜을 찾을 수 없습니다.", exception.message)
    }

    @Test
    fun `업데이트 시 조회수 댓글수 좋아요수 종료여부를 갱신한다`() {
        val raw = createHotdealRaw(1L)
        raw.viewCount = 500
        raw.commentCount = 30
        raw.likeCount = 20
        raw.isEnded = true

        val hotdeal = Hotdeal(
            hotdealRawId = 1L,
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "100",
            url = "https://coolenjoy.net/bbs/jirum/100",
            title = "테스트 핫딜",
            viewCount = 100,
            commentCount = 10,
            likeCount = 5,
            isEnded = false,
            wroteAt = now
        )
        setEntityId(hotdeal, 10L)

        whenever(hotdealRawRepository.findById(1L)).thenReturn(Optional.of(raw))
        whenever(hotdealRepository.findByHotdealRawId(1L)).thenReturn(hotdeal)
        whenever(hotdealRepository.save(any<Hotdeal>())).thenReturn(hotdeal)

        service.updateHotdealFromRaw(1L)

        assertEquals(500, hotdeal.viewCount)
        assertEquals(30, hotdeal.commentCount)
        assertEquals(20, hotdeal.likeCount)
        assertTrue(hotdeal.isEnded)
        verify(hotdealRepository).save(hotdeal)
    }

    @Test
    fun `업데이트 시 raw의 viewCount가 null이면 0으로 설정한다`() {
        val raw = HotdealRaw(
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "100",
            url = "https://coolenjoy.net/bbs/jirum/100",
            title = "테스트 핫딜",
            wroteAt = now
        )
        setEntityId(raw, 1L)

        val hotdeal = Hotdeal(
            hotdealRawId = 1L,
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "100",
            url = "https://coolenjoy.net/bbs/jirum/100",
            title = "테스트 핫딜",
            viewCount = 100,
            wroteAt = now
        )
        setEntityId(hotdeal, 10L)

        whenever(hotdealRawRepository.findById(1L)).thenReturn(Optional.of(raw))
        whenever(hotdealRepository.findByHotdealRawId(1L)).thenReturn(hotdeal)
        whenever(hotdealRepository.save(any<Hotdeal>())).thenReturn(hotdeal)

        service.updateHotdealFromRaw(1L)

        assertEquals(0, hotdeal.viewCount)
    }
}
