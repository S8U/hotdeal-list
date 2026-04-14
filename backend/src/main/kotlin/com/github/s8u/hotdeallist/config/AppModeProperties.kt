package com.github.s8u.hotdeallist.config

import com.github.s8u.hotdeallist.enums.AppMode
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppModeProperties(
    val mode: AppMode = AppMode.API
)
