package com.github.s8u.hotdeallist.exception

import org.springframework.http.HttpStatus

class BusinessException(
    override val message: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST
) : RuntimeException(message)
