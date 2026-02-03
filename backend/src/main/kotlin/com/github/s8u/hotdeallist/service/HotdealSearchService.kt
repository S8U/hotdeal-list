package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.document.HotdealDocument
import com.github.s8u.hotdeallist.entity.Hotdeal
import com.github.s8u.hotdeallist.repository.HotdealCategoryRepository
import com.github.s8u.hotdeallist.repository.HotdealElasticsearchRepository
import com.github.s8u.hotdeallist.repository.HotdealProcessRepository
import com.github.s8u.hotdeallist.repository.CategoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HotdealSearchService(
    private val hotdealElasticsearchRepository: HotdealElasticsearchRepository,
    private val hotdealCategoryRepository: HotdealCategoryRepository,
    private val hotdealProcessRepository: HotdealProcessRepository,
    private val categoryRepository: CategoryRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun indexHotdeal(hotdeal: Hotdeal) {
        val categoryCodes = getCategoryCodes(hotdeal.id!!)
        val shoppingPlatform = getShoppingPlatform(hotdeal.hotdealRawId)

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
            wroteAt = hotdeal.wroteAt,
            createdAt = hotdeal.createdAt!!,
            updatedAt = hotdeal.updatedAt!!,
            categoryCodes = categoryCodes,
            shoppingPlatform = shoppingPlatform
        )

        hotdealElasticsearchRepository.save(document)
        logger.debug("Indexed hotdeal to ES: id={}", hotdeal.id)
    }

    fun updateHotdeal(hotdeal: Hotdeal) {
        val existingDocument = hotdealElasticsearchRepository.findById(hotdeal.id!!)

        if (existingDocument.isPresent) {
            val document = existingDocument.get()
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
                wroteAt = document.wroteAt,
                createdAt = document.createdAt,
                updatedAt = hotdeal.updatedAt!!,
                categoryCodes = document.categoryCodes,
                shoppingPlatform = document.shoppingPlatform
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
        val documents = hotdeals.map { hotdeal ->
            val categoryCodes = getCategoryCodes(hotdeal.id!!)
            val shoppingPlatform = getShoppingPlatform(hotdeal.hotdealRawId)

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
                wroteAt = hotdeal.wroteAt,
                createdAt = hotdeal.createdAt!!,
                updatedAt = hotdeal.updatedAt!!,
                categoryCodes = categoryCodes,
                shoppingPlatform = shoppingPlatform
            )
        }

        hotdealElasticsearchRepository.saveAll(documents)
        logger.info("Bulk indexed {} hotdeals to ES", documents.size)
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
}
