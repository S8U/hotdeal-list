package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.document.HotdealDocument
import com.github.s8u.hotdeallist.dto.request.HotdealSearchRequest
import com.github.s8u.hotdeallist.entity.Category
import com.github.s8u.hotdeallist.entity.Hotdeal
import com.github.s8u.hotdeallist.entity.HotdealCategory
import com.github.s8u.hotdeallist.entity.HotdealProcess
import com.github.s8u.hotdeallist.enums.HotdealSortType
import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.exception.BusinessException
import com.github.s8u.hotdeallist.repository.CategoryRepository
import com.github.s8u.hotdeallist.repository.HotdealCategoryRepository
import com.github.s8u.hotdeallist.repository.HotdealElasticsearchRepository
import com.github.s8u.hotdeallist.repository.HotdealProcessRepository
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.data.elasticsearch.core.SearchHits
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExtendWith(MockKExtension::class)
class HotdealSearchServiceTest {

    @MockK
    private lateinit var hotdealElasticsearchRepository: HotdealElasticsearchRepository

    @MockK
    private lateinit var hotdealCategoryRepository: HotdealCategoryRepository

    @MockK
    private lateinit var hotdealProcessRepository: HotdealProcessRepository

    @MockK
    private lateinit var categoryRepository: CategoryRepository

    @MockK
    private lateinit var thumbnailService: HotdealThumbnailService

    @MockK
    private lateinit var elasticsearchOperations: ElasticsearchOperations

    @MockK
    private lateinit var objectMapper: ObjectMapper

    @InjectMockKs
    private lateinit var hotdealSearchService: HotdealSearchService

    private val now = LocalDateTime.of(2025, 1, 1, 12, 0)

    private fun createHotdeal(id: Long = 1L, rawId: Long = 1L): Hotdeal {
        val hotdeal = Hotdeal(
            hotdealRawId = rawId,
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "12345",
            url = "https://coolenjoy.net/bbs/jirum/12345",
            title = "테스트 핫딜",
            titleEn = "Test Hotdeal",
            productName = "테스트 상품",
            productNameEn = "Test Product",
            price = BigDecimal(10000),
            currencyUnit = "KRW",
            viewCount = 100,
            commentCount = 10,
            likeCount = 5,
            isEnded = false,
            sourceUrl = "https://shop.com/product",
            thumbnailPath = "COOLENJOY_JIRUM/12345.webp",
            wroteAt = now
        )
        setEntityId(hotdeal, id)
        setEntityTimestamps(hotdeal)
        return hotdeal
    }

    private fun createHotdealDocument(id: Long = 1L) = HotdealDocument(
        id = id,
        hotdealRawId = 1L,
        platformType = PlatformType.COOLENJOY_JIRUM,
        platformPostId = "12345",
        url = "https://coolenjoy.net/bbs/jirum/12345",
        title = "테스트 핫딜",
        titleEn = "Test Hotdeal",
        productName = "테스트 상품",
        productNameEn = "Test Product",
        price = BigDecimal(10000),
        currencyUnit = "KRW",
        viewCount = 100,
        commentCount = 10,
        likeCount = 5,
        isEnded = false,
        sourceUrl = "https://shop.com/product",
        thumbnailUrl = "https://cdn.example.com/COOLENJOY_JIRUM/12345.webp",
        wroteAt = now,
        createdAt = now,
        updatedAt = now,
        categoryCodes = listOf("electronics", "mobile", "smartphone"),
        shoppingPlatform = "쿠팡"
    )

    private fun setEntityId(entity: Any, id: Long) {
        val idField = entity.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }

    private fun setEntityTimestamps(entity: Any) {
        val createdAtField = entity.javaClass.superclass.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(entity, now)
        val updatedAtField = entity.javaClass.superclass.getDeclaredField("updatedAt")
        updatedAtField.isAccessible = true
        updatedAtField.set(entity, now)
    }

    @Nested
    @DisplayName("indexHotdeal")
    inner class IndexHotdeal {

        @Test
        @DisplayName("핫딜을 ES에 인덱싱한다")
        fun `should index hotdeal to elasticsearch`() {
            val hotdeal = createHotdeal()
            val category = HotdealCategory(hotdealId = 1L, categoryId = 10L)
            setEntityId(category, 1L)
            val categoryEntity = Category(parentId = null, code = "electronics", name = "전자·가전", depth = 0, sortOrder = 1)
            setEntityId(categoryEntity, 10L)
            val process = HotdealProcess(
                hotdealRawId = 1L, aiModel = "gpt", aiPrompt = "p", aiResponse = "r",
                title = "t", titleEn = "te", productName = "pn", productNameEn = "pne",
                categoryCode = "electronics", categoryConfidence = BigDecimal("0.95"),
                shoppingPlatform = "쿠팡"
            )

            every { hotdealCategoryRepository.findByHotdealId(1L) } returns listOf(category)
            every { categoryRepository.findAllById(listOf(10L)) } returns listOf(categoryEntity)
            every { hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L) } returns process
            every { thumbnailService.getThumbnailUrl("COOLENJOY_JIRUM/12345.webp") } returns "https://cdn.example.com/COOLENJOY_JIRUM/12345.webp"
            every { hotdealElasticsearchRepository.save(any<HotdealDocument>()) } answers { firstArg() }

            hotdealSearchService.indexHotdeal(hotdeal)

            verify { hotdealElasticsearchRepository.save(any<HotdealDocument>()) }
        }
    }

    @Nested
    @DisplayName("updateHotdeal")
    inner class UpdateHotdeal {

        @Test
        @DisplayName("기존 ES 문서가 있으면 업데이트한다")
        fun `should update existing document in elasticsearch`() {
            val hotdeal = createHotdeal()
            hotdeal.viewCount = 200
            hotdeal.likeCount = 10
            val existingDoc = createHotdealDocument()

            every { hotdealElasticsearchRepository.findById(1L) } returns Optional.of(existingDoc)
            every { thumbnailService.getThumbnailUrl("COOLENJOY_JIRUM/12345.webp") } returns "https://cdn.example.com/COOLENJOY_JIRUM/12345.webp"
            every { hotdealElasticsearchRepository.save(any<HotdealDocument>()) } answers { firstArg() }

            hotdealSearchService.updateHotdeal(hotdeal)

            verify { hotdealElasticsearchRepository.save(match<HotdealDocument> { it.viewCount == 200 && it.likeCount == 10 }) }
        }

        @Test
        @DisplayName("ES 문서가 없으면 새로 인덱싱한다")
        fun `should index hotdeal if not found in elasticsearch`() {
            val hotdeal = createHotdeal()
            val category = HotdealCategory(hotdealId = 1L, categoryId = 10L)
            setEntityId(category, 1L)
            val categoryEntity = Category(parentId = null, code = "electronics", name = "전자·가전", depth = 0, sortOrder = 1)
            setEntityId(categoryEntity, 10L)

            every { hotdealElasticsearchRepository.findById(1L) } returns Optional.empty()
            every { hotdealCategoryRepository.findByHotdealId(1L) } returns listOf(category)
            every { categoryRepository.findAllById(listOf(10L)) } returns listOf(categoryEntity)
            every { hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(1L) } returns null
            every { thumbnailService.getThumbnailUrl("COOLENJOY_JIRUM/12345.webp") } returns "https://cdn.example.com/COOLENJOY_JIRUM/12345.webp"
            every { hotdealElasticsearchRepository.save(any<HotdealDocument>()) } answers { firstArg() }

            hotdealSearchService.updateHotdeal(hotdeal)

            verify { hotdealElasticsearchRepository.save(any<HotdealDocument>()) }
        }
    }

    @Nested
    @DisplayName("deleteHotdeal")
    inner class DeleteHotdeal {

        @Test
        @DisplayName("ES에서 핫딜을 삭제한다")
        fun `should delete hotdeal from elasticsearch`() {
            every { hotdealElasticsearchRepository.deleteById(1L) } just runs

            hotdealSearchService.deleteHotdeal(1L)

            verify { hotdealElasticsearchRepository.deleteById(1L) }
        }
    }

    @Nested
    @DisplayName("indexAll")
    inner class IndexAll {

        @Test
        @DisplayName("여러 핫딜을 벌크 인덱싱한다")
        fun `should bulk index hotdeals to elasticsearch`() {
            val hotdeals = (1L..3L).map { id ->
                val hotdeal = createHotdeal(id = id, rawId = id)
                val category = HotdealCategory(hotdealId = id, categoryId = 10L)
                setEntityId(category, id)
                every { hotdealCategoryRepository.findByHotdealId(id) } returns listOf(category)
                every { hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(id) } returns null
                hotdeal
            }

            val categoryEntity = Category(parentId = null, code = "electronics", name = "전자·가전", depth = 0, sortOrder = 1)
            setEntityId(categoryEntity, 10L)
            every { categoryRepository.findAllById(listOf(10L)) } returns listOf(categoryEntity)
            every { thumbnailService.getThumbnailUrl("COOLENJOY_JIRUM/12345.webp") } returns "https://cdn.example.com/thumb.webp"
            every { hotdealElasticsearchRepository.saveAll(any<List<HotdealDocument>>()) } answers { firstArg() }

            hotdealSearchService.indexAll(hotdeals)

            verify { hotdealElasticsearchRepository.saveAll(match<List<HotdealDocument>> { it.size == 3 }) }
        }
    }

    @Nested
    @DisplayName("findById")
    inner class FindById {

        @Test
        @DisplayName("ID로 ES 문서를 찾으면 반환한다")
        fun `should return document when found`() {
            val doc = createHotdealDocument()
            every { hotdealElasticsearchRepository.findById(1L) } returns Optional.of(doc)

            val result = hotdealSearchService.findById(1L)

            assertNotNull(result)
            assertEquals(1L, result.id)
            assertEquals("테스트 핫딜", result.title)
        }

        @Test
        @DisplayName("ID로 ES 문서를 찾지 못하면 null을 반환한다")
        fun `should return null when not found`() {
            every { hotdealElasticsearchRepository.findById(999L) } returns Optional.empty()

            val result = hotdealSearchService.findById(999L)

            assertNull(result)
        }
    }

    @Nested
    @DisplayName("getPriceHistory")
    inner class GetPriceHistory {

        @Test
        @DisplayName("핫딜이 존재하지 않으면 BusinessException을 던진다")
        fun `should throw BusinessException when hotdeal not found`() {
            every { hotdealElasticsearchRepository.findById(999L) } returns Optional.empty()

            val exception = assertThrows<BusinessException> {
                hotdealSearchService.getPriceHistory(999L)
            }
            assertEquals("핫딜을 찾을 수 없습니다: 999", exception.message)
        }

        @Test
        @DisplayName("유사 상품이 없으면 빈 가격 히스토리를 반환한다")
        fun `should return empty price history when no similar products`() {
            val doc = createHotdealDocument()
            val searchHits = mockk<SearchHits<HotdealDocument>>()

            every { hotdealElasticsearchRepository.findById(1L) } returns Optional.of(doc)
            every { elasticsearchOperations.search(any(), HotdealDocument::class.java) } returns searchHits
            every { searchHits.searchHits } returns emptyList()
            every { searchHits.totalHits } returns 0L

            val result = hotdealSearchService.getPriceHistory(1L)

            assertEquals(1L, result.hotdealId)
            assertEquals("테스트 상품", result.productName)
            assertEquals(0L, result.totalSimilarCount)
            assertEquals(0, result.priceHistory.size)
        }

        @Test
        @DisplayName("productName이 null이면 title로 검색한다")
        fun `should use title when productName is null`() {
            val doc = HotdealDocument(
                id = 1L, hotdealRawId = 1L,
                platformType = PlatformType.COOLENJOY_JIRUM,
                platformPostId = "12345",
                url = "https://example.com",
                title = "제목으로 검색",
                productName = null,
                wroteAt = now, createdAt = now, updatedAt = now
            )
            val searchHits = mockk<SearchHits<HotdealDocument>>()

            every { hotdealElasticsearchRepository.findById(1L) } returns Optional.of(doc)
            every { elasticsearchOperations.search(any(), HotdealDocument::class.java) } returns searchHits
            every { searchHits.searchHits } returns emptyList()
            every { searchHits.totalHits } returns 0L

            val result = hotdealSearchService.getPriceHistory(1L)

            assertNull(result.productName)
        }

        @Test
        @DisplayName("유사 상품들의 가격을 날짜별로 그룹핑한다")
        fun `should group similar products by date with price stats`() {
            val baseDoc = createHotdealDocument()
            val similarDoc1 = HotdealDocument(
                id = 2L, hotdealRawId = 2L,
                platformType = PlatformType.PPOMPPU_PPOMPPU,
                platformPostId = "222",
                url = "https://ppomppu.co.kr/222",
                title = "유사 핫딜 1",
                productName = "테스트 상품",
                price = BigDecimal(9000),
                wroteAt = LocalDateTime.of(2025, 1, 1, 10, 0),
                createdAt = now, updatedAt = now
            )
            val similarDoc2 = HotdealDocument(
                id = 3L, hotdealRawId = 3L,
                platformType = PlatformType.QUASARZONE_JIRUM,
                platformPostId = "333",
                url = "https://quasarzone.com/333",
                title = "유사 핫딜 2",
                productName = "테스트 상품",
                price = BigDecimal(11000),
                wroteAt = LocalDateTime.of(2025, 1, 1, 14, 0),
                createdAt = now, updatedAt = now
            )

            val searchHit1 = mockk<SearchHit<HotdealDocument>>()
            val searchHit2 = mockk<SearchHit<HotdealDocument>>()
            val searchHits = mockk<SearchHits<HotdealDocument>>()

            every { hotdealElasticsearchRepository.findById(1L) } returns Optional.of(baseDoc)
            every { searchHit1.content } returns similarDoc1
            every { searchHit2.content } returns similarDoc2
            every { searchHits.searchHits } returns listOf(searchHit1, searchHit2)
            every { searchHits.totalHits } returns 2L
            every { elasticsearchOperations.search(any(), HotdealDocument::class.java) } returns searchHits

            val result = hotdealSearchService.getPriceHistory(1L)

            assertEquals(1L, result.hotdealId)
            assertEquals(2L, result.totalSimilarCount)
            assertEquals(1, result.priceHistory.size) // 같은 날짜 → 1개 그룹
            assertEquals(2, result.priceHistory[0].count)
            assertEquals(BigDecimal(9000), result.priceHistory[0].minPrice)
            assertEquals(BigDecimal(11000), result.priceHistory[0].maxPrice)
        }
    }

    @Nested
    @DisplayName("search")
    inner class Search {

        @Test
        @DisplayName("검색 결과가 없으면 빈 리스트와 hasMore=false를 반환한다")
        fun `should return empty list when no results`() {
            val searchHits = mockk<SearchHits<HotdealDocument>>()
            every { searchHits.searchHits } returns emptyList()
            every { searchHits.hasSearchHits() } returns false
            every { elasticsearchOperations.search(any(), HotdealDocument::class.java) } returns searchHits

            val request = HotdealSearchRequest()
            val result = hotdealSearchService.search(request)

            assertEquals(0, result.items.size)
            assertNull(result.nextCursor)
            assertEquals(false, result.hasMore)
        }
    }
}
