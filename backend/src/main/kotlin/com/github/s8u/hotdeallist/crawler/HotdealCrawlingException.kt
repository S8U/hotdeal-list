package com.github.s8u.hotdeallist.crawler

import com.github.s8u.hotdeallist.enums.PlatformType

class HotdealCrawlingException(
    override val message: String? = null,
    val platformType: PlatformType? = null,
    val url: String? = null,
    val html: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)