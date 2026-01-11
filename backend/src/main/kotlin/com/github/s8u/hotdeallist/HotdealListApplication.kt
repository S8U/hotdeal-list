package com.github.s8u.hotdeallist

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
class HotdealListApplication

fun main(args: Array<String>) {
	runApplication<HotdealListApplication>(*args)
}
