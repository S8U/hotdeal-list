package com.github.s8u.hotdeallist.repository

import com.github.s8u.hotdeallist.entity.HotdealCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HotdealCategoryRepository : JpaRepository<HotdealCategory, Long> {
    fun findByHotdealId(hotdealId: Long): List<HotdealCategory>
    fun findByHotdealIdIn(hotdealIds: List<Long>): List<HotdealCategory>
}
