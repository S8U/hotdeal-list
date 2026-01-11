package com.github.s8u.hotdeallist.repository

import com.github.s8u.hotdeallist.entity.HotdealRaw
import com.github.s8u.hotdeallist.enums.PlatformType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HotdealRawRepository : JpaRepository<HotdealRaw, Long> {
    fun findByPlatformTypeAndPlatformPostId(platformType: PlatformType, postId: String): HotdealRaw?
}
