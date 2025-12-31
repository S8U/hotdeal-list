package com.github.s8u.hotdeallist.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "base-url")
data class BaseUrlProperties(
    var api: String = "",
    var frontend: String = ""
)
