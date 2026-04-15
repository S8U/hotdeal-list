package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.entity.EsDictionary
import com.github.s8u.hotdeallist.enums.DictionarySource
import com.github.s8u.hotdeallist.repository.EsDictionaryRepository
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EsDictionaryService(
    private val esDictionaryRepository: EsDictionaryRepository,
    private val elasticsearchOperations: ElasticsearchOperations,
    private val entityManager: EntityManager
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MIN_WORD_FREQUENCY = 5
        private const val MIN_KOREAN_LENGTH = 2
        private val KOREAN_ONLY_REGEX = Regex("^[가-힣]+$")
    }

    /**
     * product_name에서 빈출 한글 단어를 추출하고,
     * nori가 잘못 분해하는 단어를 es_dictionary에 AUTO로 등록
     */
    @Transactional
    fun extractAndUpdateDictionary() {
        logger.info("사전 자동 추출 시작...")

        val candidateWords = extractFrequentKoreanWords()
        logger.info("빈출 한글 단어 후보: {}개", candidateWords.size)

        val problematicWords = candidateWords.distinct().filter { isNoriMisTokenized(it) }
        logger.info("nori 오분석 단어: {}개 — {}", problematicWords.size, problematicWords.take(20))

        // AUTO 사전 갱신: 기존 AUTO 삭제 후 새로 삽입
        esDictionaryRepository.deleteAllBySource(DictionarySource.AUTO)
        entityManager.flush()
        val newEntries = problematicWords
            .filter { !esDictionaryRepository.existsByWord(it) }
            .map { word -> EsDictionary(word = word, source = DictionarySource.AUTO) }
        esDictionaryRepository.saveAll(newEntries)

        val manualCount = esDictionaryRepository.findAllBySource(DictionarySource.MANUAL).size
        logger.info("사전 갱신 완료 — AUTO: {}개, MANUAL: {}개", newEntries.size, manualCount)
    }

    /**
     * product_name을 공백으로 분리하여 빈출 한글 단어 추출
     */
    private fun extractFrequentKoreanWords(): List<String> {
        @Suppress("UNCHECKED_CAST")
        val results = entityManager.createNativeQuery(
            """
            SELECT word, cnt FROM (
                SELECT SUBSTRING_INDEX(SUBSTRING_INDEX(h.product_name, ' ', n.n), ' ', -1) AS word,
                       COUNT(*) AS cnt
                FROM hotdeals h
                CROSS JOIN (
                    SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
                ) n
                WHERE h.product_name IS NOT NULL
                AND n.n <= 1 + LENGTH(h.product_name) - LENGTH(REPLACE(h.product_name, ' ', ''))
                GROUP BY word
            ) sub
            WHERE cnt >= :minFreq
            AND CHAR_LENGTH(word) >= :minLen
            AND word REGEXP '^[가-힣]+$'
            ORDER BY cnt DESC
            """
        )
            .setParameter("minFreq", MIN_WORD_FREQUENCY)
            .setParameter("minLen", MIN_KOREAN_LENGTH)
            .resultList as List<Array<Any>>

        return results.map { it[0] as String }
    }

    /**
     * nori가 단어를 원형과 다르게 분해하는지 확인
     * - 토큰이 2개 이상이면서 원형 토큰이 없으면 오분석
     */
    private fun isNoriMisTokenized(word: String): Boolean {
        return try {
            val client = (elasticsearchOperations as org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate)
                .execute { it }

            val response = client.indices().analyze { req ->
                req.index(EsIndexManagementService.ALIAS_NAME)
                    .analyzer("nori_analyzer")
                    .text(word)
            }

            val tokens = response.tokens().map { it.token() }
            // 토큰이 2개 이상이고, 원형이 토큰에 포함되지 않으면 오분석
            tokens.size > 1 || (tokens.size == 1 && tokens[0] != word)
        } catch (e: Exception) {
            // alias가 없는 경우 (초기 기동 시) — analyzer 없이 fallback
            isNoriMisTokenizedFallback(word)
        }
    }

    /**
     * 인덱스가 없을 때 fallback: 임시 인덱스 없이 기본 nori로 분석
     */
    private fun isNoriMisTokenizedFallback(word: String): Boolean {
        return try {
            val client = (elasticsearchOperations as org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate)
                .execute { it }

            val response = client.indices().analyze { req ->
                req.tokenizer { t -> t.name("nori_tokenizer") }
                    .text(word)
            }

            val tokens = response.tokens().map { it.token() }
            tokens.size > 1 || (tokens.size == 1 && tokens[0] != word)
        } catch (e: Exception) {
            logger.warn("nori 분석 실패: {}", word, e)
            false
        }
    }
}
