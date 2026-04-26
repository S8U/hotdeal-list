package com.github.s8u.hotdeallist.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val baseUrlProperties: BaseUrlProperties,
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        val origins = baseUrlProperties.frontend
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toTypedArray()

        if (origins.isEmpty()) return

        registry.addMapping("/v1/**")
            .allowedOrigins(*origins)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }
}
