package com.github.s8u.hotdeallist.repository

import com.github.s8u.hotdeallist.entity.Hotdeal
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HotdealRepository : JpaRepository<Hotdeal, Long> {
    fun findByHotdealRawId(hotdealRawId: Long): Hotdeal?
}
