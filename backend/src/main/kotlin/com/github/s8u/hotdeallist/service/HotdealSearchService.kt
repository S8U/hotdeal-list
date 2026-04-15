package com.github.s8u.hotdeallist.service

import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.elasticsearch._types.query_dsl.Like
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import com.github.s8u.hotdeallist.exception.BusinessException
import com.github.s8u.hotdeallist.document.HotdealDocument
import com.github.s8u.hotdeallist.dto.request.HotdealSearchRequest
import com.github.s8u.hotdeallist.dto.response.HotdealListResponse
import com.github.s8u.hotdeallist.dto.response.HotdealResponse
import com.github.s8u.hotdeallist.dto.response.PriceHistoryResponse
import com.github.s8u.hotdeallist.dto.response.SuggestResponse
import com.github.s8u.hotdeallist.entity.Hotdeal
import org.springframework.data.elasticsearch.core.suggest.Completion
import com.github.s8u.hotdeallist.repository.HotdealCategoryRepository
import com.github.s8u.hotdeallist.repository.HotdealElasticsearchRepository
import com.github.s8u.hotdeallist.repository.HotdealProcessRepository
import com.github.s8u.hotdeallist.repository.CategoryRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.HighlightQuery
import org.springframework.data.elasticsearch.core.query.highlight.Highlight
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Base64

@Service
class HotdealSearchService(
    private val hotdealElasticsearchRepository: HotdealElasticsearchRepository,
    private val hotdealCategoryRepository: HotdealCategoryRepository,
    private val hotdealProcessRepository: HotdealProcessRepository,
    private val categoryRepository: CategoryRepository,
    private val thumbnailService: HotdealThumbnailService,
    private val elasticsearchOperations: ElasticsearchOperations,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun indexHotdeal(hotdeal: Hotdeal) {
        val categoryCodes = getCategoryCodes(hotdeal.id!!)
        val shoppingPlatform = getShoppingPlatform(hotdeal.hotdealRawId)
        val thumbnailUrl = thumbnailService.getThumbnailUrl(hotdeal.thumbnailPath)

        val document = HotdealDocument(
            id = hotdeal.id!!,
            hotdealRawId = hotdeal.hotdealRawId,
            platformType = hotdeal.platformType,
            platformPostId = hotdeal.platformPostId,
            url = hotdeal.url,
            title = hotdeal.title,
            titleEn = hotdeal.titleEn,
            productName = hotdeal.productName,
            productNameEn = hotdeal.productNameEn,
            price = hotdeal.price,
            currencyUnit = hotdeal.currencyUnit,
            viewCount = hotdeal.viewCount,
            commentCount = hotdeal.commentCount,
            likeCount = hotdeal.likeCount,
            isEnded = hotdeal.isEnded,
            sourceUrl = hotdeal.sourceUrl,
            thumbnailUrl = thumbnailUrl,
            wroteAt = hotdeal.wroteAt,
            createdAt = hotdeal.createdAt!!,
            updatedAt = hotdeal.updatedAt!!,
            categoryCodes = categoryCodes,
            shoppingPlatform = shoppingPlatform,
            suggest = buildSuggestInput(hotdeal.productName, hotdeal.title, hotdeal.viewCount, hotdeal.likeCount, hotdeal.commentCount)
        )

        hotdealElasticsearchRepository.save(document)
        logger.debug("Indexed hotdeal to ES: id={}", hotdeal.id)
    }

    fun updateHotdeal(hotdeal: Hotdeal) {
        val existingDocument = hotdealElasticsearchRepository.findById(hotdeal.id!!)

        if (existingDocument.isPresent) {
            val document = existingDocument.get()
            val thumbnailUrl = thumbnailService.getThumbnailUrl(hotdeal.thumbnailPath)
            val updatedDocument = HotdealDocument(
                id = document.id,
                hotdealRawId = document.hotdealRawId,
                platformType = document.platformType,
                platformPostId = document.platformPostId,
                url = document.url,
                title = document.title,
                titleEn = document.titleEn,
                productName = document.productName,
                productNameEn = document.productNameEn,
                price = document.price,
                currencyUnit = document.currencyUnit,
                viewCount = hotdeal.viewCount,
                commentCount = hotdeal.commentCount,
                likeCount = hotdeal.likeCount,
                isEnded = hotdeal.isEnded,
                sourceUrl = document.sourceUrl,
                thumbnailUrl = thumbnailUrl,
                wroteAt = document.wroteAt,
                createdAt = document.createdAt,
                updatedAt = hotdeal.updatedAt!!,
                categoryCodes = document.categoryCodes,
                shoppingPlatform = document.shoppingPlatform,
                suggest = document.suggest
            )

            hotdealElasticsearchRepository.save(updatedDocument)
            logger.debug("Updated hotdeal in ES: id={}", hotdeal.id)
        } else {
            indexHotdeal(hotdeal)
        }
    }

    fun deleteHotdeal(hotdealId: Long) {
        hotdealElasticsearchRepository.deleteById(hotdealId)
        logger.debug("Deleted hotdeal from ES: id={}", hotdealId)
    }

    fun indexAll(hotdeals: List<Hotdeal>) {
        val documents = buildDocuments(hotdeals)
        hotdealElasticsearchRepository.saveAll(documents)
        logger.info("Bulk indexed {} hotdeals to ES", documents.size)
    }

    fun indexAllToIndex(hotdeals: List<Hotdeal>, indexName: String) {
        val documents = buildDocuments(hotdeals)
        val indexCoordinates = org.springframework.data.elasticsearch.core.mapping.IndexCoordinates.of(indexName)
        val queries = documents.map { doc ->
            org.springframework.data.elasticsearch.core.query.IndexQueryBuilder()
                .withId(doc.id.toString())
                .withObject(doc)
                .build()
        }
        elasticsearchOperations.bulkIndex(queries, indexCoordinates)
        logger.info("Bulk indexed {} hotdeals to index '{}'", documents.size, indexName)
    }

    private fun buildDocuments(hotdeals: List<Hotdeal>): List<HotdealDocument> {
        val hotdealIds = hotdeals.mapNotNull { it.id }
        val rawIds = hotdeals.map { it.hotdealRawId }

        val categoryMap = hotdealCategoryRepository.findByHotdealIdIn(hotdealIds)
            .groupBy { it.hotdealId }
        val allCategoryIds = categoryMap.values.flatten().map { it.categoryId }.distinct()
        val categoryCodeMap = if (allCategoryIds.isNotEmpty()) {
            categoryRepository.findAllById(allCategoryIds).associate { it.id!! to it.code }
        } else {
            emptyMap()
        }

        val processMap = hotdealProcessRepository.findByHotdealRawIdIn(rawIds)
            .groupBy { it.hotdealRawId }

        return hotdeals.map { hotdeal ->
            val categoryCodes = categoryMap[hotdeal.id]
                ?.mapNotNull { categoryCodeMap[it.categoryId] }
                ?: emptyList()
            val shoppingPlatform = processMap[hotdeal.hotdealRawId]
                ?.maxByOrNull { it.id!! }
                ?.shoppingPlatform
            val thumbnailUrl = thumbnailService.getThumbnailUrl(hotdeal.thumbnailPath)

            HotdealDocument(
                id = hotdeal.id!!,
                hotdealRawId = hotdeal.hotdealRawId,
                platformType = hotdeal.platformType,
                platformPostId = hotdeal.platformPostId,
                url = hotdeal.url,
                title = hotdeal.title,
                titleEn = hotdeal.titleEn,
                productName = hotdeal.productName,
                productNameEn = hotdeal.productNameEn,
                price = hotdeal.price,
                currencyUnit = hotdeal.currencyUnit,
                viewCount = hotdeal.viewCount,
                commentCount = hotdeal.commentCount,
                likeCount = hotdeal.likeCount,
                isEnded = hotdeal.isEnded,
                sourceUrl = hotdeal.sourceUrl,
                thumbnailUrl = thumbnailUrl,
                wroteAt = hotdeal.wroteAt,
                createdAt = hotdeal.createdAt!!,
                updatedAt = hotdeal.updatedAt!!,
                categoryCodes = categoryCodes,
                shoppingPlatform = shoppingPlatform,
                suggest = buildSuggestInput(hotdeal.productName, hotdeal.title, hotdeal.viewCount, hotdeal.likeCount, hotdeal.commentCount)
            )
        }
    }

    private fun getCategoryCodes(hotdealId: Long): List<String> {
        val hotdealCategories = hotdealCategoryRepository.findByHotdealId(hotdealId)
        val categoryIds = hotdealCategories.map { it.categoryId }
        val categories = categoryRepository.findAllById(categoryIds)
        return categories.map { it.code }
    }

    private fun getShoppingPlatform(hotdealRawId: Long): String? {
        return hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(hotdealRawId)
            ?.shoppingPlatform
    }

    // ========== 검색 API 메서드 ==========

    fun search(request: HotdealSearchRequest): HotdealListResponse {
        val searchAfter = request.cursor?.let { decodeCursor(it) }
        
        val query = buildSearchQuery(request, searchAfter)
        val searchHits = elasticsearchOperations.search(query, HotdealDocument::class.java)
        
        val items = searchHits.searchHits.map { hit ->
            hit.content.toResponse().copy(
                highlightedTitle = hit.getHighlightField("title").firstOrNull(),
                highlightedProductName = hit.getHighlightField("productName").firstOrNull(),
            )
        }
        
        val nextCursor = if (searchHits.hasSearchHits() && items.size == request.size) {
            val lastHit = searchHits.searchHits.last()
            encodeCursor(lastHit.sortValues)
        } else {
            null
        }
        
        return HotdealListResponse(
            items = items,
            nextCursor = nextCursor,
            hasMore = nextCursor != null
        )
    }

    fun findById(id: Long): HotdealDocument? {
        return hotdealElasticsearchRepository.findById(id).orElse(null)
    }

    fun getPriceHistory(hotdealId: Long): PriceHistoryResponse {
        val baseDocument = hotdealElasticsearchRepository.findById(hotdealId).orElse(null)
            ?: throw BusinessException("핫딜을 찾을 수 없습니다: $hotdealId")

        val productName = baseDocument.productName ?: baseDocument.title
        
        val moreLikeThisQuery = buildMoreLikeThisQuery(productName)
        
        val nativeQuery = NativeQuery.builder()
            .withQuery(moreLikeThisQuery)
            .withSort(Sort.by(Sort.Direction.DESC, "wroteAt"))
            .withPageable(PageRequest.of(0, 100))
            .build()

        val searchHits = elasticsearchOperations.search(nativeQuery, HotdealDocument::class.java)
        val documents = searchHits.searchHits.map { it.content }
        
        val priceHistory = groupByDateWithHotdeals(documents)

        return PriceHistoryResponse(
            hotdealId = hotdealId,
            productName = baseDocument.productName,
            totalSimilarCount = searchHits.totalHits,
            priceHistory = priceHistory
        )
    }

    private fun buildMoreLikeThisQuery(productName: String): Query {
        return Query.of { q ->
            q.bool { bool ->
                bool.must { must ->
                    must.moreLikeThis { mlt ->
                        mlt.fields("productName", "title")
                            .like(
                                Like.of { l -> l.text(productName) }
                            )
                            .minTermFreq(1)
                            .maxQueryTerms(15)
                            .minDocFreq(1)
                            .minimumShouldMatch("30%")
                    }
                }
                bool.filter { filter ->
                    filter.exists { e -> e.field("price") }
                }
                bool.filter { filter ->
                    filter.term { t -> t.field("isEnded").value(false) }
                }
            }
        }
    }

    private fun groupByDateWithHotdeals(documents: List<HotdealDocument>): List<PriceHistoryResponse.DailyPriceStats> {
        val koreaZone = java.time.ZoneId.of("Asia/Seoul")
        
        return documents
            .groupBy { it.wroteAt.atZone(koreaZone).toLocalDate() }
            .map { (date, hotdeals) ->
                val prices = hotdeals.mapNotNull { it.price?.toDouble() }
                
                PriceHistoryResponse.DailyPriceStats(
                    date = date,
                    count = hotdeals.size,
                    minPrice = prices.minOrNull()?.let { BigDecimal.valueOf(it).setScale(0, RoundingMode.HALF_UP) },
                    maxPrice = prices.maxOrNull()?.let { BigDecimal.valueOf(it).setScale(0, RoundingMode.HALF_UP) },
                    avgPrice = prices.takeIf { it.isNotEmpty() }?.average()?.let { BigDecimal.valueOf(it).setScale(0, RoundingMode.HALF_UP) },
                    hotdeals = hotdeals.map { doc ->
                        PriceHistoryResponse.HotdealSummary(
                            id = doc.id,
                            title = doc.title,
                            productName = doc.productName,
                            price = doc.price,
                            url = doc.url,
                            thumbnailUrl = doc.thumbnailUrl
                        )
                    }.sortedByDescending { it.price }
                )
            }
            .sortedByDescending { it.date }
    }

    private fun buildSearchQuery(request: HotdealSearchRequest, searchAfter: List<Any>?): NativeQuery {
        val boolQuery = BoolQuery.Builder()

        // 종료된 핫딜 제외 (기본)
        if (!request.includeEnded) {
            boolQuery.filter(
                Query.of { q -> q.term { t -> t.field("isEnded").value(false) } }
            )
        }

        // 카테고리 필터
        if (!request.categories.isNullOrEmpty()) {
            boolQuery.filter(
                Query.of { q ->
                    q.terms { t ->
                        t.field("categoryCodes")
                            .terms { tv ->
                                tv.value(request.categories.map { FieldValue.of(it) })
                            }
                    }
                }
            )
        }

        // 플랫폼 필터
        if (!request.platforms.isNullOrEmpty()) {
            boolQuery.filter(
                Query.of { q ->
                    q.terms { t ->
                        t.field("platformType")
                            .terms { tv ->
                                tv.value(request.platforms.map { FieldValue.of(it.name) })
                            }
                    }
                }
            )
        }

        // 가격 범위 필터
        if (request.minPrice != null || request.maxPrice != null) {
            boolQuery.filter(
                Query.of { q ->
                    q.range { r ->
                        r.number { n ->
                            var range = n.field("price")
                            if (request.minPrice != null) {
                                range = range.gte(request.minPrice.toDouble())
                            }
                            if (request.maxPrice != null) {
                                range = range.lte(request.maxPrice.toDouble())
                            }
                            range
                        }
                    }
                }
            )
        }

        // 키워드 검색
        if (!request.keyword.isNullOrBlank()) {
            val hasDigit = request.keyword.any { it.isDigit() }

            if (hasDigit) {
                // 숫자 포함: nori only (ngram은 숫자 토큰이 없어 노이즈 유발)
                boolQuery.must(
                    Query.of { q ->
                        q.multiMatch { m ->
                            m.fields("productName^3", "title^2", "productNameEn", "titleEn")
                                .query(request.keyword)
                                .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                                .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                        }
                    }
                )
            } else {
                // 한글/영문만: nori AND || ngram 75% fallback
                boolQuery.must(
                    Query.of { q ->
                        q.bool { keywordBool ->
                            keywordBool.should(
                                Query.of { sq ->
                                    sq.multiMatch { m ->
                                        m.fields("productName^3", "title^2", "productNameEn", "titleEn")
                                            .query(request.keyword)
                                            .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                                            .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                                    }
                                }
                            )
                            keywordBool.should(
                                Query.of { sq ->
                                    sq.multiMatch { m ->
                                        m.fields("productName.ngram^3", "title.ngram^2", "productNameEn.ngram", "titleEn.ngram")
                                            .query(request.keyword)
                                            .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                                            .minimumShouldMatch("75%")
                                    }
                                }
                            )
                        }
                    }
                )
            }
            // should: phrase 매칭으로 순위 보정
            boolQuery.should(
                Query.of { q ->
                    q.multiMatch { m ->
                        m.fields("productName^3", "title^2", "productNameEn", "titleEn")
                            .query(request.keyword)
                            .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.Phrase)
                            .boost(5.0f)
                    }
                }
            )
        }

        val nativeQueryBuilder = NativeQuery.builder()
            .withQuery(Query.of { q -> q.bool(boolQuery.build()) })
            .withSort(Sort.by(request.sort.direction, request.sort.field))
            .withSort(Sort.by(Sort.Direction.ASC, "id")) // tiebreaker
            .withPageable(PageRequest.of(0, request.size))

        if (!request.keyword.isNullOrBlank()) {
            nativeQueryBuilder.withHighlightQuery(
                HighlightQuery(
                    Highlight(
                        listOf(
                            HighlightField("title"),
                            HighlightField("productName"),
                        )
                    ),
                    HotdealDocument::class.java
                )
            )
        }

        if (searchAfter != null) {
            nativeQueryBuilder.withSearchAfter(searchAfter)
        }

        return nativeQueryBuilder.build()
    }

    private fun encodeCursor(sortValues: List<Any>): String {
        val json = objectMapper.writeValueAsString(sortValues)
        return Base64.getUrlEncoder().encodeToString(json.toByteArray())
    }

    private fun decodeCursor(cursor: String): List<Any> {
        val json = String(Base64.getUrlDecoder().decode(cursor))
        return objectMapper.readValue(json, object : TypeReference<List<Any>>() {})
    }

    fun suggest(query: String, size: Int = 7): SuggestResponse {
        val suggest = co.elastic.clients.elasticsearch.core.search.Suggester.of { s ->
            s.suggesters("hotdeal-suggest") { sg ->
                sg.prefix(query)
                    .completion { c ->
                        c.field("suggest")
                            .size(size)
                            .skipDuplicates(true)
                    }
            }
        }

        val request = co.elastic.clients.elasticsearch.core.SearchRequest.of { r ->
            r.index("hotdeals")
                .size(0)
                .suggest(suggest)
        }

        val client = (elasticsearchOperations as org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate)
            .execute { it }

        val response = client.search(request, HotdealDocument::class.java)

        val suggestions = response.suggest()["hotdeal-suggest"]
            ?.flatMap { entry ->
                entry.completion().options().map { it.text() }
            }
            ?.distinct()
            ?: emptyList()

        return SuggestResponse(suggestions = suggestions)
    }

    private fun buildSuggestInput(productName: String?, title: String, viewCount: Int, likeCount: Int, commentCount: Int): Completion {
        val inputs = mutableListOf<String>()
        if (!productName.isNullOrBlank()) inputs.add(productName)
        inputs.add(title)
        val score = viewCount + likeCount * 5 + commentCount * 2
        val completion = Completion(inputs)
        completion.setWeight(score.coerceAtLeast(1))
        return completion
    }

    private fun HotdealDocument.toResponse(): HotdealResponse {
        return HotdealResponse(
            id = this.id,
            platformType = this.platformType,
            url = this.url,
            title = this.title,
            productName = this.productName,
            price = this.price,
            currencyUnit = this.currencyUnit,
            viewCount = this.viewCount,
            commentCount = this.commentCount,
            likeCount = this.likeCount,
            isEnded = this.isEnded,
            thumbnailUrl = this.thumbnailUrl,
            shoppingPlatform = this.shoppingPlatform,
            categoryCodes = this.categoryCodes,
            wroteAt = this.wroteAt,
            createdAt = this.createdAt
        )
    }
}
