package com.github.s8u.hotdeallist.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 핫딜 원본 본문
 */
@Entity
@Table(
    name = "hotdeal_raw_contents",
    comment = "핫딜 원본 본문"
)
class HotdealRawContent(
    @Id
    @Column(name = "hotdeal_raw_id", nullable = false, comment = "핫딜 원본 ID")
    val hotdealRawId: Long,

    @Column(name = "content_html", columnDefinition = "TEXT", comment = "게시글 본문 HTML")
    val contentHtml: String? = null
)
