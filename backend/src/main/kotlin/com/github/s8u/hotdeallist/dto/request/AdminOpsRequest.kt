package com.github.s8u.hotdeallist.dto.request

import com.github.s8u.hotdeallist.enums.AdminOpsStage
import com.github.s8u.hotdeallist.enums.PlatformType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "어드민 오퍼레이션 요청")
data class AdminOpsRequest(
    @field:Schema(description = "실행할 단계 목록 (RAW, THUMBNAIL, PROCESS, HOTDEAL, ES)")
    val stages: Set<AdminOpsStage>,

    @field:Schema(description = "대상 지정")
    val target: Target,

    @field:Schema(description = "RAW 단계 옵션")
    val raw: RawOptions = RawOptions(),

    @field:Schema(description = "THUMBNAIL 단계 옵션")
    val thumbnail: ThumbnailOptions = ThumbnailOptions(),

    @field:Schema(description = "PROCESS 단계 옵션")
    val process: ProcessOptions = ProcessOptions(),

    @field:Schema(description = "드라이런 여부 (실제 쓰기 없이 대상 resolve만)", defaultValue = "false")
    val dryRun: Boolean = false
) {

    @Schema(description = "어드민 오퍼레이션 대상 지정")
    data class Target(
        @field:Schema(description = "단건 raw ID (최우선)")
        val rawId: Long? = null,

        @field:Schema(description = "단건 hotdeal ID")
        val hotdealId: Long? = null,

        @field:Schema(description = "플랫폼 타입")
        val platform: PlatformType? = null,

        @field:Schema(description = "시작 페이지 (1-based, RAW 단계 PAGE 모드)")
        val minPage: Int? = null,

        @field:Schema(description = "종료 페이지 (1-based, RAW 단계 PAGE 모드)")
        val maxPage: Int? = null,

        @field:Schema(description = "작성 시각 범위 시작")
        val wroteAtFrom: LocalDateTime? = null,

        @field:Schema(description = "작성 시각 범위 종료")
        val wroteAtTo: LocalDateTime? = null,

        @field:Schema(description = "최대 처리 건수 (최대 10000)")
        val limit: Int? = null,

        @field:Schema(description = "페이지 순회 안전상한 (기본 50, 최대 1000)", defaultValue = "50")
        val maxPagesToScan: Int = 50
    )

    @Schema(description = "RAW 단계 옵션")
    data class RawOptions(
        @field:Schema(description = "페이지 간 대기 시간 (ms)", defaultValue = "1000")
        val delayMs: Long = 1000
    )

    @Schema(description = "THUMBNAIL 단계 옵션")
    data class ThumbnailOptions(
        @field:Schema(description = "기존 썸네일 있어도 강제 재다운로드 여부", defaultValue = "false")
        val force: Boolean = false
    )

    @Schema(description = "PROCESS 단계 옵션")
    data class ProcessOptions(
        @field:Schema(description = "기존 process 있어도 AI 재호출 여부", defaultValue = "false")
        val force: Boolean = false,

        @field:Schema(description = "프롬프트 버전 (Phase B용, 현재 무시됨)")
        val promptVersion: String? = null
    )
}
