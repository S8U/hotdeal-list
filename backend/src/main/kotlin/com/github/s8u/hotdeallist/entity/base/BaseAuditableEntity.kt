package com.github.s8u.hotdeallist.entity.base

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy

@MappedSuperclass
abstract class BaseAuditableEntity(
    @CreatedBy
    @Column(updatable = false)
    val createdBy: Long? = null,

    @LastModifiedBy
    var updatedBy: Long? = null
) : BaseEntity()
