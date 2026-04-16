package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.entity.Category
import com.github.s8u.hotdeallist.entity.Hotdeal
import com.github.s8u.hotdeallist.entity.HotdealCategory
import com.github.s8u.hotdeallist.exception.BusinessException
import com.github.s8u.hotdeallist.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class HotdealService(
    private val hotdealRepository: HotdealRepository,
    private val hotdealCategoryRepository: HotdealCategoryRepository,
    private val hotdealRawRepository: HotdealRawRepository,
    private val hotdealProcessRepository: HotdealProcessRepository,
    private val categoryRepository: CategoryRepository,
    private val hotdealSearchService: HotdealSearchService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun createHotdealFromRawAndProcess(rawId: Long) {
        logger.info("Creating hotdeal from rawId={}", rawId)

        // 핫딜 원본 데이터
        val hotdealRaw = hotdealRawRepository.findById(rawId)
            .orElseThrow { BusinessException("핫딜 원본 데이터를 찾을 수 없습니다.") }

        // 핫딜 가공 데이터
        // 가공 데이터가 없더라도 핫딜은 생성되어야 함. 나중에 재시도 처리
        val hotdealProcess = hotdealProcessRepository.findFirstByHotdealRawIdOrderByIdDesc(rawId)

        // 핫딜 생성
        var hotdeal = Hotdeal(
            hotdealRawId = rawId,
            platformType = hotdealRaw.platformType,
            platformPostId = hotdealRaw.platformPostId,
            url = hotdealRaw.url,
            title = hotdealRaw.title,
            titleEn = hotdealProcess?.titleEn,
            productName = hotdealProcess?.productName,
            productNameEn = hotdealProcess?.productNameEn,
            price = hotdealRaw.price ?: hotdealProcess?.price ?: BigDecimal.ZERO,
            currencyUnit = hotdealRaw.currencyUnit ?: hotdealProcess?.currencyUnit ?: "KRW",
            viewCount = hotdealRaw.viewCount ?: 0,
            commentCount = hotdealRaw.commentCount ?: 0,
            likeCount = hotdealRaw.likeCount ?: 0,
            isEnded = hotdealRaw.isEnded ?: false,
            sourceUrl = hotdealRaw.sourceUrl,
            thumbnailPath = hotdealRaw.thumbnailPath,
            wroteAt = hotdealRaw.wroteAt
        )

        hotdeal = hotdealRepository.save(hotdeal)

        // 핫딜 카테고리 생성
        if (hotdealProcess != null) {
            val category = categoryRepository.findByCode(hotdealProcess.categoryCode)
                ?: throw BusinessException("카테고리를 찾을 수 없습니다.")

            val hotdealCategories = mutableListOf<HotdealCategory>()
            var currentCategory: Category? = category

            while (currentCategory != null) {
                hotdealCategories.add(
                    HotdealCategory(
                        hotdealId = hotdeal.id!!,
                        categoryId = currentCategory.id!!
                    )
                )
                currentCategory = currentCategory.parentId?.let { categoryRepository.findById(it).orElse(null) }
            }

            hotdealCategoryRepository.saveAll(hotdealCategories)
        }

        hotdealSearchService.indexHotdeal(hotdeal)

        logger.info("Created hotdeal from rawId={}, id={}", rawId, hotdeal.id)
    }

    fun updateHotdealFromRaw(rawId: Long) {
        logger.info("Updating hotdeal from rawId={},", rawId)

        val hotdealRaw = hotdealRawRepository.findById(rawId)
            .orElseThrow { BusinessException("핫딜 원본 데이터를 찾을 수 없습니다.") }

        val hotdeal = hotdealRepository.findByHotdealRawId(rawId)
            ?: throw BusinessException("핫딜을 찾을 수 없습니다.")

        hotdeal.viewCount = hotdealRaw.viewCount ?: 0
        hotdeal.commentCount = hotdealRaw.commentCount ?: 0
        hotdeal.likeCount = hotdealRaw.likeCount ?: 0
        hotdeal.isEnded = hotdealRaw.isEnded ?: false

        hotdealRepository.save(hotdeal)
        hotdealSearchService.updateHotdeal(hotdeal)

        logger.info("Updated hotdeal from rawId={}, viewCount={}, commentCount={}, likeCount={}, isEnded={}", rawId, hotdeal.viewCount, hotdeal.commentCount, hotdeal.likeCount, hotdeal.isEnded)
    }

}