package com.github.s8u.hotdeallist.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.net.InetSocketAddress
import java.net.Proxy

@Configuration
@ConfigurationProperties(prefix = "crawler")
data class CrawlerProperties(
    var ruliweb: PlatformCrawlerProperties = PlatformCrawlerProperties()
) {
    data class PlatformCrawlerProperties(
        var socks: SocksProxyProperties = SocksProxyProperties()
    )

    data class SocksProxyProperties(
        var host: String = "",
        var port: Int = 0
    ) {
        fun toProxy(): Proxy? {
            if (host.isBlank() || port <= 0) return null

            return Proxy(Proxy.Type.SOCKS, InetSocketAddress(host, port))
        }
    }
}
