package com.github.s8u.hotdeallist.controller

import com.github.s8u.hotdeallist.dto.response.PlatformResponse
import com.github.s8u.hotdeallist.enums.PlatformType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/platforms")
@Tag(name = "Platform", description = "플랫폼 API")
class PlatformController {

    @GetMapping
    @Operation(summary = "플랫폼 목록 조회", description = "지원하는 플랫폼(커뮤니티) 목록을 반환")
    fun listPlatforms(): ResponseEntity<List<PlatformResponse>> {
        val platforms = PlatformType.entries.map { PlatformResponse(it.name, it.communityName, it.displayName) }
        return ResponseEntity.ok(platforms)
    }
}
