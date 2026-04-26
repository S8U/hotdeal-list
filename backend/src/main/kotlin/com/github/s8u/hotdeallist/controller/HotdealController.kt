package com.github.s8u.hotdeallist.controller

import com.github.s8u.hotdeallist.dto.request.HotdealSearchRequest
import com.github.s8u.hotdeallist.dto.response.HotdealListResponse
import com.github.s8u.hotdeallist.dto.response.HotdealResponse
import com.github.s8u.hotdeallist.dto.response.PriceHistoryResponse
import com.github.s8u.hotdeallist.dto.response.SuggestResponse
import com.github.s8u.hotdeallist.exception.BusinessException
import com.github.s8u.hotdeallist.service.HotdealSearchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/hotdeals")
@Tag(name = "Hotdeal", description = "핫딜 조회 API")
@Validated
class HotdealController(
    private val hotdealSearchService: HotdealSearchService
) {

    @GetMapping
    @Operation(summary = "핫딜 목록 조회", description = "필터, 검색, 정렬을 지원하는 핫딜 목록 API")
    fun listHotdeals(
        @ParameterObject @Valid request: HotdealSearchRequest
    ): ResponseEntity<HotdealListResponse> {
        val response = hotdealSearchService.search(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/suggest")
    @Operation(summary = "검색어 자동완성", description = "입력된 키워드로 상품명 자동완성 추천")
    fun suggest(
        @Parameter(description = "검색 키워드") @RequestParam q: String,
        @Parameter(description = "추천 개수 (기본 7)") @RequestParam(defaultValue = "7") size: Int
    ): ResponseEntity<SuggestResponse> {
        if (q.isBlank()) return ResponseEntity.ok(SuggestResponse(emptyList()))
        val response = hotdealSearchService.suggest(q.trim(), size.coerceIn(1, 20))
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "핫딜 상세 조회")
    fun getHotdeal(
        @Parameter(description = "핫딜 ID") @PathVariable id: Long
    ): ResponseEntity<HotdealResponse> {
        val document = hotdealSearchService.findById(id)
            ?: throw BusinessException("핫딜을 찾을 수 없습니다.")
        
        val response = HotdealResponse(
            id = document.id,
            platformType = document.platformType,
            url = document.url,
            title = document.title,
            productName = document.productName,
            price = document.price,
            currencyUnit = document.currencyUnit,
            viewCount = document.viewCount,
            commentCount = document.commentCount,
            likeCount = document.likeCount,
            isEnded = document.isEnded,
            thumbnailUrl = document.thumbnailUrl,
            shoppingPlatform = document.shoppingPlatform,
            categoryCodes = document.categoryCodes,
            wroteAt = document.wroteAt,
            createdAt = document.createdAt
        )
        
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/price-history")
    @Operation(summary = "가격 히스토리 조회", description = "유사 상품명 기반 날짜별 가격 추이 조회")
    fun getPriceHistory(
        @Parameter(description = "핫딜 ID") @PathVariable id: Long
    ): ResponseEntity<PriceHistoryResponse> {
        val response = hotdealSearchService.getPriceHistory(id)
        return ResponseEntity.ok(response)
    }
}
