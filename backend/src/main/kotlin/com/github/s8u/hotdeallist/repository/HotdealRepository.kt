package com.github.s8u.hotdeallist.repository

import com.github.s8u.hotdeallist.entity.Hotdeal
import com.github.s8u.hotdeallist.enums.PlatformType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface HotdealRepository : JpaRepository<Hotdeal, Long> {
    fun findByHotdealRawId(hotdealRawId: Long): Hotdeal?
    fun findByUpdatedAtGreaterThanEqual(updatedAt: LocalDateTime): List<Hotdeal>

    @Query("""
        SELECT h.id FROM Hotdeal h
        WHERE (:platform IS NULL OR h.platformType = :platform)
          AND (:wroteAtFrom IS NULL OR h.wroteAt >= :wroteAtFrom)
          AND (:wroteAtTo IS NULL OR h.wroteAt <= :wroteAtTo)
        ORDER BY h.wroteAt DESC, h.id DESC
    """)
    fun findIdsByCriteria(
        @Param("platform") platform: PlatformType?,
        @Param("wroteAtFrom") wroteAtFrom: LocalDateTime?,
        @Param("wroteAtTo") wroteAtTo: LocalDateTime?,
        pageable: Pageable
    ): List<Long>
}

