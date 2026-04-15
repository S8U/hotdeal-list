package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.document.HotdealDocument
import com.github.s8u.hotdeallist.repository.EsDictionaryRepository
import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.io.StringReader

@Service
class EsIndexManagementService(
    private val elasticsearchOperations: ElasticsearchOperations,
    private val esDictionaryRepository: EsDictionaryRepository,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val ALIAS_NAME = "hotdeals"
        const val INDEX_BLUE = "hotdeals-blue"
        const val INDEX_GREEN = "hotdeals-green"
    }

    fun getCurrentIndex(): String? {
        return try {
            val client = getEsClient()
            val response = client.indices().getAlias { it.name(ALIAS_NAME) }
            response.aliases().keys.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun getNextIndex(): String {
        return when (getCurrentIndex()) {
            INDEX_BLUE -> INDEX_GREEN
            INDEX_GREEN -> INDEX_BLUE
            else -> INDEX_BLUE
        }
    }

    fun aliasExists(): Boolean {
        return try {
            getEsClient().indices().existsAlias { it.name(ALIAS_NAME) }.value()
        } catch (e: Exception) {
            false
        }
    }

    fun indexExists(indexName: String): Boolean {
        return try {
            getEsClient().indices().exists { it.index(indexName) }.value()
        } catch (e: Exception) {
            false
        }
    }

    fun buildSettingsJson(): String {
        val dictionaryWords = esDictionaryRepository.findAll().map { it.word }
        logger.info("사전 단어 {}개 로드: {}", dictionaryWords.size, dictionaryWords.take(10))

        val noriTokenizer = mutableMapOf<String, Any>(
            "type" to "nori_tokenizer",
            "decompound_mode" to "mixed"
        )
        if (dictionaryWords.isNotEmpty()) {
            noriTokenizer["user_dictionary_rules"] = dictionaryWords
        }

        val settings = mapOf(
            "analysis" to mapOf(
                "analyzer" to mapOf(
                    "nori_analyzer" to mapOf(
                        "type" to "custom",
                        "tokenizer" to "nori_mixed_tokenizer",
                        "filter" to listOf("lowercase", "nori_readingform", "nori_pos_filter")
                    ),
                    "ngram_analyzer" to mapOf(
                        "type" to "custom",
                        "tokenizer" to "ngram_tokenizer",
                        "filter" to listOf("lowercase")
                    ),
                    "suggest_analyzer" to mapOf(
                        "type" to "custom",
                        "tokenizer" to "nori_mixed_tokenizer",
                        "filter" to listOf("lowercase")
                    )
                ),
                "tokenizer" to mapOf(
                    "nori_mixed_tokenizer" to noriTokenizer,
                    "ngram_tokenizer" to mapOf(
                        "type" to "ngram",
                        "min_gram" to 2,
                        "max_gram" to 3,
                        "token_chars" to listOf("letter")
                    )
                ),
                "filter" to mapOf(
                    "nori_pos_filter" to mapOf(
                        "type" to "nori_part_of_speech",
                        "stoptags" to listOf(
                            "EC", "EF", "EP", "ETM", "ETN",
                            "JC", "JKB", "JKC", "JKG", "JKO", "JKQ", "JKS", "JKV", "JX",
                            "SC", "SE", "SF", "SP", "SSC", "SSO", "SY",
                            "VCN", "VCP", "VSV", "VX",
                            "XPN", "XSA", "XSN", "XSV"
                        )
                    )
                )
            )
        )

        return objectMapper.writeValueAsString(settings)
    }

    fun buildMappingsJson(): String {
        val indexOps = elasticsearchOperations.indexOps(HotdealDocument::class.java)
        val mapping = indexOps.createMapping()
        return mapping.toJson()
    }

    fun createIndex(indexName: String) {
        val client = getEsClient()

        if (indexExists(indexName)) {
            client.indices().delete { it.index(indexName) }
            logger.info("기존 인덱스 삭제: {}", indexName)
        }

        val settingsJson = buildSettingsJson()
        val mappingsJson = buildMappingsJson()

        client.indices().create { req ->
            req.index(indexName)
                .settings { s -> s.withJson(StringReader(settingsJson)) }
                .mappings { m -> m.withJson(StringReader(mappingsJson)) }
        }

        logger.info("인덱스 생성 완료: {}", indexName)
    }

    fun switchAlias(newIndex: String) {
        val client = getEsClient()
        val currentIndex = getCurrentIndex()

        client.indices().updateAliases { req ->
            req.actions { actions ->
                if (currentIndex != null) {
                    actions.remove { r -> r.index(currentIndex).alias(ALIAS_NAME) }
                }
                actions.add { a -> a.index(newIndex).alias(ALIAS_NAME).isWriteIndex(true) }
                actions
            }
        }

        logger.info("alias '{}' 스위칭: {} → {}", ALIAS_NAME, currentIndex, newIndex)
    }

    fun deleteIndex(indexName: String) {
        if (indexExists(indexName)) {
            getEsClient().indices().delete { it.index(indexName) }
            logger.info("인덱스 삭제: {}", indexName)
        }
    }

    fun getDocumentCount(indexName: String): Long {
        return getEsClient().count { it.index(indexName) }.count()
    }

    private fun getEsClient(): co.elastic.clients.elasticsearch.ElasticsearchClient {
        return (elasticsearchOperations as org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate)
            .execute { it }
    }
}
