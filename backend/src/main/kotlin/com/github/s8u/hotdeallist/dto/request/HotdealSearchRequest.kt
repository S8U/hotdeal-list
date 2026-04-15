package com.github.s8u.hotdeallist.dto.request

import com.github.s8u.hotdeallist.enums.HotdealSortType
import com.github.s8u.hotdeallist.enums.PlatformType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import java.math.BigDecimal

@Schema(description = "핫딜 검색 요청")
data class HotdealSearchRequest(
    @field:Schema(description = "페이지네이션 커서")
    val cursor: String? = null,

    @field:Schema(description = "페이지 크기 (최대 100)", defaultValue = "20")
    @field:Max(100)
    val size: Int = 20,

    @field:Schema(description = "정렬 기준", defaultValue = "LATEST")
    val sort: HotdealSortType = HotdealSortType.LATEST,

    @field:Schema(description = "검색 키워드")
    val keyword: String? = null,

    @field:Schema(description = "카테고리 코드 목록")
    val categories: List<String>? = null,

    @field:Schema(description = "플랫폼 목록")
    val platforms: List<PlatformType>? = null,

    @field:Schema(description = "최소 가격")
    val minPrice: BigDecimal? = null,

    @field:Schema(description = "최대 가격")
    val maxPrice: BigDecimal? = null,

    @field:Schema(description = "종료된 핫딜 포함 여부", defaultValue = "true")
    val includeEnded: Boolean = true
)
