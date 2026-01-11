package com.github.s8u.hotdeallist.repository

import com.github.s8u.hotdeallist.entity.HotdealProcess
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HotdealProcessRepository : JpaRepository<HotdealProcess, Long> {
    fun findFirstByHotdealRawIdOrderByIdDesc(hotdealRawId: Long): HotdealProcess?
}
