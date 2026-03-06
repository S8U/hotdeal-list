package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.crawler.HotdealCrawler
import com.github.s8u.hotdeallist.crawler.HotdealCrawlerResolver
import com.github.s8u.hotdeallist.crawler.HotdealCrawlingException
import com.github.s8u.hotdeallist.crawler.dto.HotdealCrawlDetailDto
import com.github.s8u.hotdeallist.crawler.dto.HotdealCrawlListDto
import com.github.s8u.hotdeallist.crawler.dto.HotdealCrawlListItemDto
import com.github.s8u.hotdeallist.entity.HotdealRaw
import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.exception.BusinessException
import com.github.s8u.hotdeallist.repository.HotdealRawRepository
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class HotdealRawServiceTest {

    @MockK
    private lateinit var hotdealCrawlerResolver: HotdealCrawlerResolver

    @MockK
    private lateinit var hotdealRawRepository: HotdealRawRepository

    @InjectMockKs
    private lateinit var hotdealRawService: HotdealRawService

    @MockK
    private lateinit var crawler: HotdealCrawler

    private val now = LocalDateTime.of(2025, 1, 1, 12, 0)

    private fun setEntityId(entity: Any, id: Long) {
        val idField = entity.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }

    @Nested
    @DisplayName("crawlHotdealRaw")
    inner class CrawlHotdealRaw {

        @Test
        @DisplayName("지원하지 않는 플랫폼이면 BusinessException을 던진다")
        fun `should throw BusinessException for unsupported platform`() {
            every { hotdealCrawlerResolver.getByPlatformType(PlatformType.COOLENJOY_JIRUM) } returns null

            assertThrows<BusinessException> {
                hotdealRawService.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)
            }
        }

        @Test
        @DisplayName("목록 크롤링 실패 시 BusinessException을 던진다")
        fun `should throw BusinessException when list crawling fails`() {
            every { hotdealCrawlerResolver.getByPlatformType(PlatformType.COOLENJOY_JIRUM) } returns crawler
            every { crawler.crawlList(1) } throws RuntimeException("Connection failed")

            assertThrows<BusinessException> {
                hotdealRawService.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)
            }
        }

        @Test
        @DisplayName("새 게시글을 크롤링하고 저장한다")
        fun `should crawl and save new post`() {
            val listItems = listOf(
                HotdealCrawlListItemDto(
                    url = "https://coolenjoy.net/bbs/jirum/1",
                    platformPostId = "1",
                    category = "디지털",
                    viewCount = 100,
                    commentCount = 10,
                    likeCount = 5,
                    isEnded = false
                )
            )
            val listDto = HotdealCrawlListDto(isSuccess = true, count = 1, maxCount = 20, items = listItems)
            val detailDto = HotdealCrawlDetailDto(
                platformType = PlatformType.COOLENJOY_JIRUM,
                platformPostId = "1",
                url = "https://coolenjoy.net/bbs/jirum/1",
                title = "테스트 핫딜",
                category = "디지털",
                contentHtml = "<p>내용</p>",
                price = null,
                wroteAt = now
            )

            every { hotdealCrawlerResolver.getByPlatformType(PlatformType.COOLENJOY_JIRUM) } returns crawler
            every { crawler.crawlList(1) } returns listDto
            every { hotdealRawRepository.findByPlatformTypeAndPlatformPostId(PlatformType.COOLENJOY_JIRUM, "1") } returns null
            every { crawler.crawlDetail("https://coolenjoy.net/bbs/jirum/1") } returns detailDto
            every { hotdealRawRepository.save(any()) } answers {
                val raw = firstArg<HotdealRaw>()
                setEntityId(raw, 1L)
                raw
            }

            val result = hotdealRawService.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)

            assertEquals(1, result.size)
            assertTrue(result[1L] == true) // isNew = true
            verify { crawler.crawlDetail("https://coolenjoy.net/bbs/jirum/1") }
            verify { hotdealRawRepository.save(any()) }
        }

        @Test
        @DisplayName("기존 게시글의 조회수/댓글수/좋아요수를 업데이트한다")
        fun `should update existing post counts`() {
            val listItems = listOf(
                HotdealCrawlListItemDto(
                    url = "https://coolenjoy.net/bbs/jirum/1",
                    platformPostId = "1",
                    viewCount = 200,
                    commentCount = 20,
                    likeCount = 15,
                    isEnded = true
                )
            )
            val listDto = HotdealCrawlListDto(isSuccess = true, count = 1, maxCount = 20, items = listItems)
            val existingRaw = HotdealRaw(
                platformType = PlatformType.COOLENJOY_JIRUM,
                platformPostId = "1",
                url = "https://coolenjoy.net/bbs/jirum/1",
                title = "기존 핫딜",
                viewCount = 100,
                commentCount = 10,
                likeCount = 5,
                isEnded = false,
                wroteAt = now
            )
            setEntityId(existingRaw, 1L)

            every { hotdealCrawlerResolver.getByPlatformType(PlatformType.COOLENJOY_JIRUM) } returns crawler
            every { crawler.crawlList(1) } returns listDto
            every { hotdealRawRepository.findByPlatformTypeAndPlatformPostId(PlatformType.COOLENJOY_JIRUM, "1") } returns existingRaw
            every { hotdealRawRepository.save(any()) } answers {
                val raw = firstArg<HotdealRaw>()
                raw
            }

            val result = hotdealRawService.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)

            assertEquals(1, result.size)
            assertTrue(result[1L] == false) // isNew = false
            assertEquals(200, existingRaw.viewCount)
            assertEquals(20, existingRaw.commentCount)
            assertEquals(15, existingRaw.likeCount)
            assertEquals(true, existingRaw.isEnded)
            verify(exactly = 0) { crawler.crawlDetail(any()) }
        }

        @Test
        @DisplayName("개별 아이템 크롤링 중 HotdealCrawlingException 발생 시 해당 아이템을 건너뛴다")
        fun `should skip item when HotdealCrawlingException occurs`() {
            val listItems = listOf(
                HotdealCrawlListItemDto(url = "https://example.com/1", platformPostId = "1"),
                HotdealCrawlListItemDto(url = "https://example.com/2", platformPostId = "2")
            )
            val listDto = HotdealCrawlListDto(isSuccess = true, count = 2, maxCount = 20, items = listItems)
            val detailDto2 = HotdealCrawlDetailDto(
                platformType = PlatformType.COOLENJOY_JIRUM,
                platformPostId = "2",
                url = "https://example.com/2",
                title = "핫딜 2",
                wroteAt = now
            )

            every { hotdealCrawlerResolver.getByPlatformType(PlatformType.COOLENJOY_JIRUM) } returns crawler
            every { crawler.crawlList(1) } returns listDto
            every { hotdealRawRepository.findByPlatformTypeAndPlatformPostId(PlatformType.COOLENJOY_JIRUM, "1") } returns null
            every { crawler.crawlDetail("https://example.com/1") } throws
                HotdealCrawlingException("크롤링 실패", PlatformType.COOLENJOY_JIRUM, "https://example.com/1")
            every { hotdealRawRepository.findByPlatformTypeAndPlatformPostId(PlatformType.COOLENJOY_JIRUM, "2") } returns null
            every { crawler.crawlDetail("https://example.com/2") } returns detailDto2
            every { hotdealRawRepository.save(any()) } answers {
                val raw = firstArg<HotdealRaw>()
                setEntityId(raw, 2L)
                raw
            }

            val result = hotdealRawService.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)

            assertEquals(1, result.size)
            assertTrue(result.containsKey(2L))
        }

        @Test
        @DisplayName("개별 아이템에서 일반 Exception 발생 시 해당 아이템을 건너뛴다")
        fun `should skip item when general Exception occurs`() {
            val listItems = listOf(
                HotdealCrawlListItemDto(url = "https://example.com/1", platformPostId = "1")
            )
            val listDto = HotdealCrawlListDto(isSuccess = true, count = 1, maxCount = 20, items = listItems)

            every { hotdealCrawlerResolver.getByPlatformType(PlatformType.COOLENJOY_JIRUM) } returns crawler
            every { crawler.crawlList(1) } returns listDto
            every { hotdealRawRepository.findByPlatformTypeAndPlatformPostId(PlatformType.COOLENJOY_JIRUM, "1") } returns null
            every { crawler.crawlDetail("https://example.com/1") } throws RuntimeException("Unexpected error")

            val result = hotdealRawService.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)

            assertEquals(0, result.size)
        }

        @Test
        @DisplayName("빈 게시글 목록이면 빈 맵을 반환한다")
        fun `should return empty map for empty post list`() {
            val listDto = HotdealCrawlListDto(isSuccess = true, count = 0, maxCount = 20, items = emptyList())

            every { hotdealCrawlerResolver.getByPlatformType(PlatformType.COOLENJOY_JIRUM) } returns crawler
            every { crawler.crawlList(1) } returns listDto

            val result = hotdealRawService.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)

            assertEquals(0, result.size)
        }
    }
}
