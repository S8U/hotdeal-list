package com.github.s8u.hotdeallist.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "플랫폼 정보")
data class PlatformResponse(
    @field:Schema(description = "플랫폼 코드", example = "COOLENJOY_JIRUM")
    val code: String,

    @field:Schema(description = "커뮤니티(사이트)명", example = "쿨엔조이")
    val communityName: String,

    @field:Schema(description = "플랫폼 표시명", example = "쿨엔조이 지름")
    val displayName: String,
)
