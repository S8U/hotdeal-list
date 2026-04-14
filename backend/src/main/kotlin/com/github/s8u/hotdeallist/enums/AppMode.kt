package com.github.s8u.hotdeallist.enums

enum class AppMode {
    API,
    CRAWLING_API;

    val isCrawlingEnabled: Boolean
        get() = this == CRAWLING_API
}
