package com.github.s8u.hotdeallist.repository

import com.github.s8u.hotdeallist.entity.HotdealRaw
import com.github.s8u.hotdeallist.enums.PlatformType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface HotdealRawRepository : JpaRepository<HotdealRaw, Long> {
    fun findByPlatformTypeAndPlatformPostId(platformType: PlatformType, postId: String): HotdealRaw?
    fun findTop100ByIsThumbnailDownloadedFalse(): List<HotdealRaw>

    @Query("""
        SELECT r FROM HotdealRaw r
        WHERE r.id NOT IN (SELECT h.hotdealRawId FROM Hotdeal h)
        ORDER BY r.id DESC
    """)
    fun findRawsWithoutHotdeal(pageable: Pageable): List<HotdealRaw>

    @Query("""
        SELECT r.id FROM HotdealRaw r
        WHERE (:platform IS NULL OR r.platformType = :platform)
          AND (:wroteAtFrom IS NULL OR r.wroteAt >= :wroteAtFrom)
          AND (:wroteAtTo IS NULL OR r.wroteAt <= :wroteAtTo)
        ORDER BY r.wroteAt DESC, r.id DESC
    """)
    fun findIdsByCriteria(
        @Param("platform") platform: PlatformType?,
        @Param("wroteAtFrom") wroteAtFrom: LocalDateTime?,
        @Param("wroteAtTo") wroteAtTo: LocalDateTime?,
        pageable: Pageable
    ): List<Long>
}

