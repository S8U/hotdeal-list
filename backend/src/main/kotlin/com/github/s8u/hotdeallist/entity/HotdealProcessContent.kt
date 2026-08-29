package com.github.s8u.hotdeallist.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 핫딜 AI 가공 원문
 */
@Entity
@Table(
    name = "hotdeal_process_contents",
    comment = "핫딜 AI 가공 원문"
)
class HotdealProcessContent(
    @Id
    @Column(name = "hotdeal_process_id", nullable = false, comment = "핫딜 가공 데이터 ID")
    val hotdealProcessId: Long,

    @Column(name = "ai_prompt", columnDefinition = "TEXT", nullable = false, comment = "AI 요청 프롬프트")
    val aiPrompt: String,

    @Column(name = "ai_response", columnDefinition = "TEXT", nullable = false, comment = "AI 응답 원본")
    val aiResponse: String
)
