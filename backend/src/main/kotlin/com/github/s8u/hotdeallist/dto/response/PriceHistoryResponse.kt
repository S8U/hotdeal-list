package com.github.s8u.hotdeallist.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "가격 히스토리 응답")
data class PriceHistoryResponse(
    @field:Schema(description = "기준 핫딜 ID")
    val hotdealId: Long,

    @field:Schema(description = "기준 핫딜 상품명")
    val productName: String?,

    @field:Schema(description = "유사 상품 총 개수")
    val totalSimilarCount: Long,

    @field:Schema(description = "날짜별 가격 통계 목록")
    val priceHistory: List<DailyPriceStats>
) {
    @Schema(description = "일별 가격 통계")
    data class DailyPriceStats(
        @field:Schema(description = "날짜 (yyyy-MM-dd)")
        val date: LocalDate,

        @field:Schema(description = "해당 날짜의 핫딜 수")
        val count: Int,

        @field:Schema(description = "최저 가격")
        val minPrice: BigDecimal?,

        @field:Schema(description = "최고 가격")
        val maxPrice: BigDecimal?,

        @field:Schema(description = "평균 가격")
        val avgPrice: BigDecimal?,

        @field:Schema(description = "해당 날짜의 핫딜 목록")
        val hotdeals: List<HotdealSummary>
    )

    @Schema(description = "핫딜 요약 정보")
    data class HotdealSummary(
        @field:Schema(description = "핫딜 ID")
        val id: Long,

        @field:Schema(description = "게시글 제목")
        val title: String,

        @field:Schema(description = "상품명")
        val productName: String?,

        @field:Schema(description = "가격")
        val price: BigDecimal?,

        @field:Schema(description = "원본 게시글 URL")
        val url: String,

        @field:Schema(description = "썸네일 URL")
        val thumbnailUrl: String?
    )
}
