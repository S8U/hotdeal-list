package com.github.s8u.hotdeallist.controller

import com.github.s8u.hotdeallist.document.HotdealDocument
import com.github.s8u.hotdeallist.dto.response.HotdealListResponse
import com.github.s8u.hotdeallist.dto.response.HotdealResponse
import com.github.s8u.hotdeallist.dto.response.PriceHistoryResponse
import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.exception.BusinessException
import com.github.s8u.hotdeallist.exception.GlobalExceptionHandler
import com.github.s8u.hotdeallist.service.HotdealSearchService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class HotdealControllerTest {

    @MockK
    private lateinit var hotdealSearchService: HotdealSearchService

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(HotdealController(hotdealSearchService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    private fun createHotdealResponse(id: Long = 1L) = HotdealResponse(
        id = id,
        platformType = PlatformType.COOLENJOY_JIRUM,
        url = "https://coolenjoy.net/bbs/jirum/1",
        title = "테스트 핫딜",
        productName = "테스트 상품",
        price = BigDecimal(10000),
        currencyUnit = "KRW",
        viewCount = 100,
        commentCount = 10,
        likeCount = 5,
        isEnded = false,
        thumbnailUrl = "https://example.com/thumb.webp",
        shoppingPlatform = "쿠팡",
        categoryCodes = listOf("electronics", "mobile", "smartphone"),
        wroteAt = LocalDateTime.of(2025, 1, 1, 12, 0),
        createdAt = LocalDateTime.of(2025, 1, 1, 12, 0)
    )

    private fun createHotdealDocument(id: Long = 1L) = HotdealDocument(
        id = id,
        hotdealRawId = 1L,
        platformType = PlatformType.COOLENJOY_JIRUM,
        platformPostId = "12345",
        url = "https://coolenjoy.net/bbs/jirum/1",
        title = "테스트 핫딜",
        productName = "테스트 상품",
        price = BigDecimal(10000),
        currencyUnit = "KRW",
        viewCount = 100,
        commentCount = 10,
        likeCount = 5,
        isEnded = false,
        thumbnailUrl = "https://example.com/thumb.webp",
        shoppingPlatform = "쿠팡",
        categoryCodes = listOf("electronics", "mobile", "smartphone"),
        wroteAt = LocalDateTime.of(2025, 1, 1, 12, 0),
        createdAt = LocalDateTime.of(2025, 1, 1, 12, 0),
        updatedAt = LocalDateTime.of(2025, 1, 1, 12, 0)
    )

    @Nested
    @DisplayName("GET /api/v1/hotdeals")
    inner class ListHotdeals {

        @Test
        @DisplayName("기본 요청 시 핫딜 목록을 반환한다")
        fun `should return hotdeal list with default parameters`() {
            val response = HotdealListResponse(
                items = listOf(createHotdealResponse()),
                nextCursor = null,
                hasMore = false
            )
            every { hotdealSearchService.search(any()) } returns response

            mockMvc.get("/api/v1/hotdeals")
                .andExpect {
                    status { isOk() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                    jsonPath("$.items.length()") { value(1) }
                    jsonPath("$.items[0].id") { value(1) }
                    jsonPath("$.items[0].title") { value("테스트 핫딜") }
                    jsonPath("$.hasMore") { value(false) }
                    jsonPath("$.nextCursor") { doesNotExist() }
                }
        }

        @Test
        @DisplayName("페이지네이션 커서와 함께 요청 시 다음 페이지 정보를 반환한다")
        fun `should return list with cursor pagination`() {
            val response = HotdealListResponse(
                items = listOf(createHotdealResponse()),
                nextCursor = "encodedCursor123",
                hasMore = true
            )
            every { hotdealSearchService.search(any()) } returns response

            mockMvc.get("/api/v1/hotdeals") {
                param("cursor", "prevCursor")
                param("size", "10")
            }.andExpect {
                status { isOk() }
                jsonPath("$.hasMore") { value(true) }
                jsonPath("$.nextCursor") { value("encodedCursor123") }
            }
        }

        @Test
        @DisplayName("카테고리 필터로 검색한다")
        fun `should filter by categories`() {
            val response = HotdealListResponse(items = emptyList(), nextCursor = null, hasMore = false)
            every { hotdealSearchService.search(any()) } returns response

            mockMvc.get("/api/v1/hotdeals") {
                param("categories", "electronics", "mobile")
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        @DisplayName("플랫폼 필터로 검색한다")
        fun `should filter by platforms`() {
            val response = HotdealListResponse(items = emptyList(), nextCursor = null, hasMore = false)
            every { hotdealSearchService.search(any()) } returns response

            mockMvc.get("/api/v1/hotdeals") {
                param("platforms", "COOLENJOY_JIRUM", "PPOMPPU_PPOMPPU")
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        @DisplayName("가격 범위 필터로 검색한다")
        fun `should filter by price range`() {
            val response = HotdealListResponse(items = emptyList(), nextCursor = null, hasMore = false)
            every { hotdealSearchService.search(any()) } returns response

            mockMvc.get("/api/v1/hotdeals") {
                param("minPrice", "10000")
                param("maxPrice", "50000")
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        @DisplayName("키워드로 검색한다")
        fun `should search by keyword`() {
            val response = HotdealListResponse(items = emptyList(), nextCursor = null, hasMore = false)
            every { hotdealSearchService.search(any()) } returns response

            mockMvc.get("/api/v1/hotdeals") {
                param("keyword", "아이폰")
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        @DisplayName("정렬 기준을 지정하여 검색한다")
        fun `should sort by specified sort type`() {
            val response = HotdealListResponse(items = emptyList(), nextCursor = null, hasMore = false)
            every { hotdealSearchService.search(any()) } returns response

            mockMvc.get("/api/v1/hotdeals") {
                param("sort", "POPULAR")
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        @DisplayName("종료된 핫딜도 포함하여 검색한다")
        fun `should include ended hotdeals when specified`() {
            val response = HotdealListResponse(items = emptyList(), nextCursor = null, hasMore = false)
            every { hotdealSearchService.search(any()) } returns response

            mockMvc.get("/api/v1/hotdeals") {
                param("includeEnded", "true")
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        @DisplayName("빈 결과를 정상적으로 반환한다")
        fun `should return empty list when no results`() {
            val response = HotdealListResponse(items = emptyList(), nextCursor = null, hasMore = false)
            every { hotdealSearchService.search(any()) } returns response

            mockMvc.get("/api/v1/hotdeals")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.items.length()") { value(0) }
                    jsonPath("$.hasMore") { value(false) }
                }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/hotdeals/{id}")
    inner class GetHotdeal {

        @Test
        @DisplayName("핫딜 ID로 상세 정보를 조회한다")
        fun `should return hotdeal detail by id`() {
            every { hotdealSearchService.findById(1L) } returns createHotdealDocument()

            mockMvc.get("/api/v1/hotdeals/1")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.id") { value(1) }
                    jsonPath("$.title") { value("테스트 핫딜") }
                    jsonPath("$.productName") { value("테스트 상품") }
                    jsonPath("$.price") { value(10000) }
                    jsonPath("$.platformType") { value("COOLENJOY_JIRUM") }
                    jsonPath("$.categoryCodes.length()") { value(3) }
                }
        }

        @Test
        @DisplayName("존재하지 않는 핫딜을 조회하면 400 에러를 반환한다")
        fun `should return 400 when hotdeal not found`() {
            every { hotdealSearchService.findById(999L) } returns null

            mockMvc.get("/api/v1/hotdeals/999")
                .andExpect {
                    status { isBadRequest() }
                    jsonPath("$.message") { value("핫딜을 찾을 수 없습니다.") }
                }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/hotdeals/{id}/price-history")
    inner class GetPriceHistory {

        @Test
        @DisplayName("핫딜의 가격 히스토리를 조회한다")
        fun `should return price history for hotdeal`() {
            val priceHistory = PriceHistoryResponse(
                hotdealId = 1L,
                productName = "테스트 상품",
                totalSimilarCount = 5L,
                priceHistory = listOf(
                    PriceHistoryResponse.DailyPriceStats(
                        date = LocalDate.of(2025, 1, 1),
                        count = 2,
                        minPrice = BigDecimal(9000),
                        maxPrice = BigDecimal(11000),
                        avgPrice = BigDecimal(10000),
                        hotdeals = listOf(
                            PriceHistoryResponse.HotdealSummary(
                                id = 1L,
                                title = "테스트 핫딜",
                                productName = "테스트 상품",
                                price = BigDecimal(10000),
                                url = "https://example.com/1",
                                thumbnailUrl = null
                            )
                        )
                    )
                )
            )
            every { hotdealSearchService.getPriceHistory(1L) } returns priceHistory

            mockMvc.get("/api/v1/hotdeals/1/price-history")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.hotdealId") { value(1) }
                    jsonPath("$.productName") { value("테스트 상품") }
                    jsonPath("$.totalSimilarCount") { value(5) }
                    jsonPath("$.priceHistory.length()") { value(1) }
                    jsonPath("$.priceHistory[0].count") { value(2) }
                    jsonPath("$.priceHistory[0].minPrice") { value(9000) }
                }
        }

        @Test
        @DisplayName("존재하지 않는 핫딜의 가격 히스토리 조회 시 400 에러를 반환한다")
        fun `should return 400 when hotdeal not found for price history`() {
            every { hotdealSearchService.getPriceHistory(999L) } throws
                BusinessException("핫딜을 찾을 수 없습니다: 999")

            mockMvc.get("/api/v1/hotdeals/999/price-history")
                .andExpect {
                    status { isBadRequest() }
                    jsonPath("$.message") { value("핫딜을 찾을 수 없습니다: 999") }
                }
        }
    }
}
