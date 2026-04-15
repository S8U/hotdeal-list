package com.github.s8u.hotdeallist.repository

import com.github.s8u.hotdeallist.entity.EsDictionary
import com.github.s8u.hotdeallist.enums.DictionarySource
import org.springframework.data.jpa.repository.JpaRepository

interface EsDictionaryRepository : JpaRepository<EsDictionary, Long> {
    fun findAllBySource(source: DictionarySource): List<EsDictionary>
    fun deleteAllBySource(source: DictionarySource)
    fun existsByWord(word: String): Boolean
}
