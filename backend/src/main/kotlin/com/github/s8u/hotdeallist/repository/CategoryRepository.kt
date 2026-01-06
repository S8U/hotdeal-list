package com.github.s8u.hotdeallist.repository

import com.github.s8u.hotdeallist.entity.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {
    fun findByCode(code: String): Category?
}
