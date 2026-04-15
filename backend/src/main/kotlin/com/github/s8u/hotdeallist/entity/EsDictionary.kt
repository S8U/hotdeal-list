package com.github.s8u.hotdeallist.entity

import com.github.s8u.hotdeallist.enums.DictionarySource
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "es_dictionary",
    uniqueConstraints = [UniqueConstraint(name = "uk_word", columnNames = ["word"])],
    comment = "Elasticsearch nori 사용자 사전"
)
class EsDictionary(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 50, comment = "사전 단어")
    val word: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, comment = "등록 출처 (AUTO/MANUAL)")
    val source: DictionarySource = DictionarySource.AUTO,

    @Column(nullable = false, updatable = false, comment = "등록 시간")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
