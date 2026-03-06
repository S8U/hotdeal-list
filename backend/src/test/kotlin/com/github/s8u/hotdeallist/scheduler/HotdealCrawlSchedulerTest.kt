package com.github.s8u.hotdeallist.scheduler

import com.github.s8u.hotdeallist.service.HotdealServiceFacade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
class HotdealCrawlSchedulerTest {

    @Mock
    lateinit var hotdealServiceFacade: HotdealServiceFacade

    private lateinit var scheduler: HotdealCrawlScheduler

    @BeforeEach
    fun setUp() {
        scheduler = HotdealCrawlScheduler(hotdealServiceFacade)
    }

    @Test
    fun `crawlHotdeal은 1부터 3페이지까지 모든 플랫폼을 크롤링한다`() {
        scheduler.crawlHotdeal()

        verify(hotdealServiceFacade).createHotdealAllPlatforms(1)
        verify(hotdealServiceFacade).createHotdealAllPlatforms(2)
        verify(hotdealServiceFacade).createHotdealAllPlatforms(3)
        verify(hotdealServiceFacade, times(3)).createHotdealAllPlatforms(any())
    }
}
