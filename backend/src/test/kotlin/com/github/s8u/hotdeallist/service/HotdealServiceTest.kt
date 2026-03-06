package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.entity.Category
import com.github.s8u.hotdeallist.entity.Hotdeal
import com.github.s8u.hotdeallist.entity.HotdealProcess
import com.github.s8u.hotdeallist.entity.HotdealRaw
import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.exception.BusinessException
import com.github.s8u.hotdeallist.repository.*
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class HotdealServiceTest {

    @MockK
    private lateinit var hotdealRepository: HotdealRepository

    @MockK
    private lateinit var hotdealCategoryRepository: HotdealCategoryRepository

    @MockK
    private lateinit var hotdealRawRepository: HotdealRawRepository

    @MockK
    private lateinit var hotdealProcessRepository: HotdealProcessRepository

    @MockK
    private lateinit var categoryRepository: CategoryRepository

    @MockK
    private lateinit var hotdealSearchService: HotdealSearchService

    @MockK
    private lateinit var thumbnailService: HotdealThumbnailService

    @InjectMockKs
    private lateinit var hotdealService: HotdealService

    private val now = LocalDateTime.of(2025, 1, 1, 12, 0)

    private fun createHotdealRaw(id: Long = 1L): HotdealRaw {
        val raw = HotdealRaw(
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "12345",
            url = "https://coolenjoy.net/bbs/jirum/12345",
            title = "[쿠팡] 삼성 갤럭시 S25 (100,000원)",
            category = "디지털",
            contentHtml = "<p>본문 내용</p>",
            price = BigDecimal(100000),
            currencyUnit = "KRW",
            viewCount = 100,
            commentCount = 10,
            likeCount = 5,
            isEnded = false,
            sourceUrl = "https://shop.com/product",
            thumbnailImageUrl = "https://cdn.example.com/thumb.jpg",
            firstImageUrl = "https://cdn.example.com/first.jpg",
            wroteAt = now
        )
        setEntityId(raw, id)
        return raw
    }

    private fun createHotdealProcess(rawId: Long = 1L): HotdealProcess {
        val process = HotdealProcess(
            hotdealRawId = rawId,
            aiModel = "openai/gpt-oss-120b:free",
            aiPrompt = "prompt",
            aiResponse = "response",
            title = "삼성 갤럭시 S25",
            titleEn = "Samsung Galaxy S25",
            productName = "갤럭시 S25",
            productNameEn = "Galaxy S25",
            categoryCode = "smartphone",
            categoryConfidence = BigDecimal("0.95"),
            shoppingPlatform = "쿠팡",
            price = BigDecimal(100000),
            currencyUnit = "KRW"
        )
        setEntityId(process, 1L)
        return process
    }

    private fun createCategory(id: Long, parentId: Long?, code: String): Category {
        val category = Category(
            parentId = parentId,
            code = code,
            name = "카테고리",
            depth = if (parentId == null) 0 else 1,
            sortOrder = 1
        )
        setEntityId(category, id)
        return category
    }

    private fun setEntityId(entity: Any, id: Long) {
        val idField = entity.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }

    @Nested
    @DisplayName("createHotdealFromRawAndProcess")
    inner class CreateHotdealFromRawAndProcess {

        @Test
        @DisplayName("Raw와 Process 데이터로 핫딜을 생성한다")
        fun `should create hotdeal from raw and process data`() {
            val raw = createHotdealRaw()
            val process = createHotdealProcess()
            val category = createCategory(10L, 5L, "smartphone")
            val parentCategory = createCategory(5L, null, "mobile")

            every { hotdealRawRepository.findById(1L) } returns Optional.of(raw)
            every { hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L) } returns process
            every { thumbnailService.downloadAndStore(
                PlatformType.COOLENJOY_JIRUM, "12345",
                "https://cdn.example.com/thumb.jpg", "https://cdn.example.com/first.jpg"
            ) } returns "COOLENJOY_JIRUM/12345.webp"
            every { hotdealRepository.save(any()) } answers {
                val hotdeal = firstArg<Hotdeal>()
                setEntityId(hotdeal, 1L)
                hotdeal
            }
            every { categoryRepository.findByCode("smartphone") } returns category
            every { categoryRepository.findById(5L) } returns Optional.of(parentCategory)
            every { categoryRepository.findById(any()) } returns Optional.empty()
            every { hotdealCategoryRepository.saveAll(any<List<Any>>()) } answers { firstArg() }
            every { hotdealSearchService.indexHotdeal(any()) } just runs

            hotdealService.createHotdealFromRawAndProcess(1L)

            verify { hotdealRepository.save(any()) }
            verify { hotdealCategoryRepository.saveAll(match<List<Any>> { it.size == 2 }) }
            verify { hotdealSearchService.indexHotdeal(any()) }
        }

        @Test
        @DisplayName("가공 데이터가 없어도 핫딜을 생성한다")
        fun `should create hotdeal even without process data`() {
            val raw = createHotdealRaw()

            every { hotdealRawRepository.findById(1L) } returns Optional.of(raw)
            every { hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L) } returns null
            every { thumbnailService.downloadAndStore(any(), any(), any(), any()) } returns null
            every { hotdealRepository.save(any()) } answers {
                val hotdeal = firstArg<Hotdeal>()
                setEntityId(hotdeal, 1L)
                hotdeal
            }
            every { hotdealSearchService.indexHotdeal(any()) } just runs

            hotdealService.createHotdealFromRawAndProcess(1L)

            verify { hotdealRepository.save(any()) }
            verify(exactly = 0) { hotdealCategoryRepository.saveAll(any<List<Any>>()) }
            verify { hotdealSearchService.indexHotdeal(any()) }
        }

        @Test
        @DisplayName("Raw 데이터가 없으면 BusinessException을 던진다")
        fun `should throw BusinessException when raw data not found`() {
            every { hotdealRawRepository.findById(999L) } returns Optional.empty()

            assertThrows<BusinessException> {
                hotdealService.createHotdealFromRawAndProcess(999L)
            }
        }

        @Test
        @DisplayName("카테고리 코드에 해당하는 카테고리가 없으면 BusinessException을 던진다")
        fun `should throw BusinessException when category not found`() {
            val raw = createHotdealRaw()
            val process = createHotdealProcess()

            every { hotdealRawRepository.findById(1L) } returns Optional.of(raw)
            every { hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L) } returns process
            every { thumbnailService.downloadAndStore(any(), any(), any(), any()) } returns null
            every { hotdealRepository.save(any()) } answers {
                val hotdeal = firstArg<Hotdeal>()
                setEntityId(hotdeal, 1L)
                hotdeal
            }
            every { categoryRepository.findByCode("smartphone") } returns null

            assertThrows<BusinessException> {
                hotdealService.createHotdealFromRawAndProcess(1L)
            }
        }

        @Test
        @DisplayName("Raw에 가격이 없으면 Process의 가격을 사용한다")
        fun `should use process price when raw price is null`() {
            val raw = HotdealRaw(
                platformType = PlatformType.COOLENJOY_JIRUM,
                platformPostId = "12345",
                url = "https://coolenjoy.net/bbs/jirum/12345",
                title = "테스트",
                price = null,
                wroteAt = now
            )
            setEntityId(raw, 1L)
            val process = createHotdealProcess()

            every { hotdealRawRepository.findById(1L) } returns Optional.of(raw)
            every { hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L) } returns process
            every { thumbnailService.downloadAndStore(any(), any(), any(), any()) } returns null
            every { hotdealRepository.save(any()) } answers {
                val hotdeal = firstArg<Hotdeal>()
                setEntityId(hotdeal, 1L)
                assertEquals(BigDecimal(100000), hotdeal.price)
                hotdeal
            }
            every { categoryRepository.findByCode("smartphone") } returns createCategory(10L, null, "smartphone")
            every { hotdealCategoryRepository.saveAll(any<List<Any>>()) } answers { firstArg() }
            every { hotdealSearchService.indexHotdeal(any()) } just runs

            hotdealService.createHotdealFromRawAndProcess(1L)

            verify { hotdealRepository.save(match { it.price == BigDecimal(100000) }) }
        }

        @Test
        @DisplayName("Raw와 Process 모두 가격이 없으면 0을 사용한다")
        fun `should use zero price when both raw and process price are null`() {
            val raw = HotdealRaw(
                platformType = PlatformType.COOLENJOY_JIRUM,
                platformPostId = "12345",
                url = "https://coolenjoy.net/bbs/jirum/12345",
                title = "테스트",
                price = null,
                wroteAt = now
            )
            setEntityId(raw, 1L)
            val process = HotdealProcess(
                hotdealRawId = 1L, aiModel = "m", aiPrompt = "p", aiResponse = "r",
                title = "t", titleEn = "te", productName = "pn", productNameEn = "pne",
                categoryCode = "etc", categoryConfidence = BigDecimal("0.5"),
                price = null
            )

            every { hotdealRawRepository.findById(1L) } returns Optional.of(raw)
            every { hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L) } returns process
            every { thumbnailService.downloadAndStore(any(), any(), any(), any()) } returns null
            every { hotdealRepository.save(any()) } answers {
                val hotdeal = firstArg<Hotdeal>()
                setEntityId(hotdeal, 1L)
                hotdeal
            }
            every { categoryRepository.findByCode("etc") } returns createCategory(99L, null, "etc")
            every { hotdealCategoryRepository.saveAll(any<List<Any>>()) } answers { firstArg() }
            every { hotdealSearchService.indexHotdeal(any()) } just runs

            hotdealService.createHotdealFromRawAndProcess(1L)

            verify { hotdealRepository.save(match { it.price == BigDecimal.ZERO }) }
        }
    }

    @Nested
    @DisplayName("updateHotdealFromRaw")
    inner class UpdateHotdealFromRaw {

        @Test
        @DisplayName("Raw 데이터로 핫딜의 조회수/댓글수/좋아요수/종료여부를 업데이트한다")
        fun `should update hotdeal counts from raw data`() {
            val raw = createHotdealRaw()
            raw.viewCount = 500
            raw.commentCount = 50
            raw.likeCount = 30
            raw.isEnded = true

            val hotdeal = Hotdeal(
                hotdealRawId = 1L,
                platformType = PlatformType.COOLENJOY_JIRUM,
                platformPostId = "12345",
                url = "https://coolenjoy.net/bbs/jirum/12345",
                title = "테스트",
                viewCount = 100,
                commentCount = 10,
                likeCount = 5,
                isEnded = false,
                wroteAt = now
            )
            setEntityId(hotdeal, 1L)

            every { hotdealRawRepository.findById(1L) } returns Optional.of(raw)
            every { hotdealRepository.findByHotdealRawId(1L) } returns hotdeal
            every { hotdealRepository.save(any()) } answers { firstArg() }
            every { hotdealSearchService.updateHotdeal(any()) } just runs

            hotdealService.updateHotdealFromRaw(1L)

            assertEquals(500, hotdeal.viewCount)
            assertEquals(50, hotdeal.commentCount)
            assertEquals(30, hotdeal.likeCount)
            assertEquals(true, hotdeal.isEnded)
            verify { hotdealRepository.save(hotdeal) }
            verify { hotdealSearchService.updateHotdeal(hotdeal) }
        }

        @Test
        @DisplayName("Raw 데이터가 없으면 BusinessException을 던진다")
        fun `should throw BusinessException when raw data not found`() {
            every { hotdealRawRepository.findById(999L) } returns Optional.empty()

            assertThrows<BusinessException> {
                hotdealService.updateHotdealFromRaw(999L)
            }
        }

        @Test
        @DisplayName("핫딜이 없으면 BusinessException을 던진다")
        fun `should throw BusinessException when hotdeal not found`() {
            val raw = createHotdealRaw()
            every { hotdealRawRepository.findById(1L) } returns Optional.of(raw)
            every { hotdealRepository.findByHotdealRawId(1L) } returns null

            assertThrows<BusinessException> {
                hotdealService.updateHotdealFromRaw(1L)
            }
        }

        @Test
        @DisplayName("Raw의 viewCount가 null이면 0으로 업데이트한다")
        fun `should set zero when raw counts are null`() {
            val raw = HotdealRaw(
                platformType = PlatformType.COOLENJOY_JIRUM,
                platformPostId = "12345",
                url = "https://coolenjoy.net/bbs/jirum/12345",
                title = "테스트",
                viewCount = 0,
                commentCount = 0,
                likeCount = 0,
                isEnded = false,
                wroteAt = now
            )
            setEntityId(raw, 1L)

            val hotdeal = Hotdeal(
                hotdealRawId = 1L,
                platformType = PlatformType.COOLENJOY_JIRUM,
                platformPostId = "12345",
                url = "https://coolenjoy.net/bbs/jirum/12345",
                title = "테스트",
                viewCount = 100,
                wroteAt = now
            )
            setEntityId(hotdeal, 1L)

            every { hotdealRawRepository.findById(1L) } returns Optional.of(raw)
            every { hotdealRepository.findByHotdealRawId(1L) } returns hotdeal
            every { hotdealRepository.save(any()) } answers { firstArg() }
            every { hotdealSearchService.updateHotdeal(any()) } just runs

            hotdealService.updateHotdealFromRaw(1L)

            assertEquals(0, hotdeal.viewCount)
        }
    }
}
