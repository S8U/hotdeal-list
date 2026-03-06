package com.github.s8u.hotdeallist.scheduler

import com.github.s8u.hotdeallist.service.HotdealServiceFacade
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class HotdealCrawlSchedulerTest {

    @MockK
    private lateinit var hotdealServiceFacade: HotdealServiceFacade

    @InjectMockKs
    private lateinit var scheduler: HotdealCrawlScheduler

    @Test
    @DisplayName("스케줄러가 1~3페이지를 순차적으로 크롤링한다")
    fun `should crawl pages 1 to 3 sequentially`() {
        every { hotdealServiceFacade.createHotdealAllPlatforms(any()) } just runs

        scheduler.crawlHotdeal()

        verifyOrder {
            hotdealServiceFacade.createHotdealAllPlatforms(1)
            hotdealServiceFacade.createHotdealAllPlatforms(2)
            hotdealServiceFacade.createHotdealAllPlatforms(3)
        }
    }

    @Test
    @DisplayName("스케줄러가 정확히 3번 호출한다")
    fun `should call createHotdealAllPlatforms exactly 3 times`() {
        every { hotdealServiceFacade.createHotdealAllPlatforms(any()) } just runs

        scheduler.crawlHotdeal()

        verify(exactly = 3) { hotdealServiceFacade.createHotdealAllPlatforms(any()) }
    }
}
