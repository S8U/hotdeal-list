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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class HotdealRawServiceTest {

    @Mock
    lateinit var hotdealCrawlerResolver: HotdealCrawlerResolver

    @Mock
    lateinit var hotdealRawRepository: HotdealRawRepository

    @Mock
    lateinit var crawler: HotdealCrawler

    private lateinit var service: HotdealRawService

    @BeforeEach
    fun setUp() {
        service = HotdealRawService(hotdealCrawlerResolver, hotdealRawRepository)
    }

    @Test
    fun `지원하지 않는 플랫폼이면 BusinessException을 발생시킨다`() {
        whenever(hotdealCrawlerResolver.getByPlatformType(any())).thenReturn(null)

        val exception = assertThrows(BusinessException::class.java) {
            service.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)
        }
        assertEquals("지원하지 않는 플랫폼입니다.", exception.message)
    }

    @Test
    fun `목록 크롤링 실패 시 BusinessException을 발생시킨다`() {
        whenever(hotdealCrawlerResolver.getByPlatformType(any())).thenReturn(crawler)
        whenever(crawler.crawlList(any())).thenThrow(RuntimeException("connection error"))

        val exception = assertThrows(BusinessException::class.java) {
            service.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)
        }
        assertEquals("게시글 목록 크롤링에 실패했습니다.", exception.message)
    }

    @Test
    fun `신규 게시글은 상세 크롤링 후 저장하고 true로 표시한다`() {
        val now = LocalDateTime.now()
        val listItem = HotdealCrawlListItemDto(
            url = "https://coolenjoy.net/bbs/jirum/100",
            platformPostId = "100",
            category = "전자",
            viewCount = 50,
            commentCount = 3,
            likeCount = 10,
            isEnded = false
        )
        val listDto = HotdealCrawlListDto(isSuccess = true, count = 1, maxCount = 25, items = listOf(listItem))
        val detailDto = HotdealCrawlDetailDto(
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "100",
            url = "https://coolenjoy.net/bbs/jirum/100",
            title = "테스트 핫딜",
            category = "전자",
            contentHtml = "<p>내용</p>",
            viewCount = 50,
            commentCount = 3,
            likeCount = 10,
            isEnded = false,
            wroteAt = now
        )

        whenever(hotdealCrawlerResolver.getByPlatformType(any())).thenReturn(crawler)
        whenever(crawler.crawlList(1)).thenReturn(listDto)
        whenever(hotdealRawRepository.findByPlatformTypeAndPlatformPostId(any(), eq("100"))).thenReturn(null)
        whenever(crawler.crawlDetail("https://coolenjoy.net/bbs/jirum/100")).thenReturn(detailDto)
        whenever(hotdealRawRepository.save(any<HotdealRaw>())).thenAnswer { invocation ->
            val raw = invocation.getArgument<HotdealRaw>(0)
            // id를 리플렉션으로 설정
            HotdealRaw::class.java.superclass.getDeclaredField("id").apply {
                isAccessible = true
                set(raw, 1L)
            }
            raw
        }

        val result = service.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)

        assertEquals(1, result.size)
        assertTrue(result[1L]!!)
        verify(crawler).crawlDetail("https://coolenjoy.net/bbs/jirum/100")
        verify(hotdealRawRepository).save(any<HotdealRaw>())
    }

    @Test
    fun `기존 게시글은 조회수 댓글수 좋아요수만 업데이트하고 false로 표시한다`() {
        val now = LocalDateTime.now()
        val existingRaw = HotdealRaw(
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "100",
            url = "https://coolenjoy.net/bbs/jirum/100",
            title = "기존 핫딜",
            viewCount = 10,
            commentCount = 1,
            likeCount = 2,
            wroteAt = now
        )
        HotdealRaw::class.java.superclass.getDeclaredField("id").apply {
            isAccessible = true
            set(existingRaw, 5L)
        }

        val listItem = HotdealCrawlListItemDto(
            url = "https://coolenjoy.net/bbs/jirum/100",
            platformPostId = "100",
            viewCount = 100,
            commentCount = 20,
            likeCount = 50,
            isEnded = true
        )
        val listDto = HotdealCrawlListDto(isSuccess = true, count = 1, maxCount = 25, items = listOf(listItem))

        whenever(hotdealCrawlerResolver.getByPlatformType(any())).thenReturn(crawler)
        whenever(crawler.crawlList(1)).thenReturn(listDto)
        whenever(hotdealRawRepository.findByPlatformTypeAndPlatformPostId(any(), eq("100"))).thenReturn(existingRaw)
        whenever(hotdealRawRepository.save(any<HotdealRaw>())).thenReturn(existingRaw)

        val result = service.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)

        assertEquals(1, result.size)
        assertFalse(result[5L]!!)
        assertEquals(100, existingRaw.viewCount)
        assertEquals(20, existingRaw.commentCount)
        assertEquals(50, existingRaw.likeCount)
        assertTrue(existingRaw.isEnded)
        verify(crawler, never()).crawlDetail(any())
    }

    @Test
    fun `상세 크롤링 중 HotdealCrawlingException 발생 시 해당 항목을 건너뛴다`() {
        val listItem = HotdealCrawlListItemDto(
            url = "https://coolenjoy.net/bbs/jirum/100",
            platformPostId = "100"
        )
        val listDto = HotdealCrawlListDto(isSuccess = true, count = 1, maxCount = 25, items = listOf(listItem))

        whenever(hotdealCrawlerResolver.getByPlatformType(any())).thenReturn(crawler)
        whenever(crawler.crawlList(1)).thenReturn(listDto)
        whenever(hotdealRawRepository.findByPlatformTypeAndPlatformPostId(any(), eq("100"))).thenReturn(null)
        whenever(crawler.crawlDetail(any())).thenThrow(
            HotdealCrawlingException("파싱 실패", PlatformType.COOLENJOY_JIRUM, "https://coolenjoy.net/bbs/jirum/100")
        )

        val result = service.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `상세 크롤링 중 일반 예외 발생 시 해당 항목을 건너뛴다`() {
        val listItem = HotdealCrawlListItemDto(
            url = "https://coolenjoy.net/bbs/jirum/100",
            platformPostId = "100"
        )
        val listDto = HotdealCrawlListDto(isSuccess = true, count = 1, maxCount = 25, items = listOf(listItem))

        whenever(hotdealCrawlerResolver.getByPlatformType(any())).thenReturn(crawler)
        whenever(crawler.crawlList(1)).thenReturn(listDto)
        whenever(hotdealRawRepository.findByPlatformTypeAndPlatformPostId(any(), eq("100"))).thenReturn(null)
        whenever(crawler.crawlDetail(any())).thenThrow(RuntimeException("unexpected error"))

        val result = service.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `빈 목록 크롤링 시 빈 맵을 반환한다`() {
        val listDto = HotdealCrawlListDto(isSuccess = true, count = 0, maxCount = 25, items = emptyList())

        whenever(hotdealCrawlerResolver.getByPlatformType(any())).thenReturn(crawler)
        whenever(crawler.crawlList(1)).thenReturn(listDto)

        val result = service.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `기존 게시글 업데이트 시 viewCount가 null이면 기존 값을 유지한다`() {
        val now = LocalDateTime.now()
        val existingRaw = HotdealRaw(
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "100",
            url = "https://coolenjoy.net/bbs/jirum/100",
            title = "기존 핫딜",
            viewCount = 10,
            commentCount = 5,
            likeCount = 3,
            wroteAt = now
        )
        HotdealRaw::class.java.superclass.getDeclaredField("id").apply {
            isAccessible = true
            set(existingRaw, 5L)
        }

        val listItem = HotdealCrawlListItemDto(
            url = "https://coolenjoy.net/bbs/jirum/100",
            platformPostId = "100",
            viewCount = null,
            commentCount = null,
            likeCount = null,
            isEnded = false
        )
        val listDto = HotdealCrawlListDto(isSuccess = true, count = 1, maxCount = 25, items = listOf(listItem))

        whenever(hotdealCrawlerResolver.getByPlatformType(any())).thenReturn(crawler)
        whenever(crawler.crawlList(1)).thenReturn(listDto)
        whenever(hotdealRawRepository.findByPlatformTypeAndPlatformPostId(any(), eq("100"))).thenReturn(existingRaw)
        whenever(hotdealRawRepository.save(any<HotdealRaw>())).thenReturn(existingRaw)

        service.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)

        assertEquals(10, existingRaw.viewCount)
        assertEquals(5, existingRaw.commentCount)
        assertEquals(3, existingRaw.likeCount)
    }

    @Test
    fun `신규 게시글의 카테고리는 목록 아이템 카테고리를 우선 사용한다`() {
        val now = LocalDateTime.now()
        val listItem = HotdealCrawlListItemDto(
            url = "https://coolenjoy.net/bbs/jirum/200",
            platformPostId = "200",
            category = "목록카테고리"
        )
        val listDto = HotdealCrawlListDto(isSuccess = true, count = 1, maxCount = 25, items = listOf(listItem))
        val detailDto = HotdealCrawlDetailDto(
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "200",
            url = "https://coolenjoy.net/bbs/jirum/200",
            title = "테스트",
            category = "상세카테고리",
            wroteAt = now
        )

        whenever(hotdealCrawlerResolver.getByPlatformType(any())).thenReturn(crawler)
        whenever(crawler.crawlList(1)).thenReturn(listDto)
        whenever(hotdealRawRepository.findByPlatformTypeAndPlatformPostId(any(), eq("200"))).thenReturn(null)
        whenever(crawler.crawlDetail(any())).thenReturn(detailDto)
        whenever(hotdealRawRepository.save(any<HotdealRaw>())).thenAnswer { invocation ->
            val raw = invocation.getArgument<HotdealRaw>(0)
            assertEquals("목록카테고리", raw.category)
            HotdealRaw::class.java.superclass.getDeclaredField("id").apply {
                isAccessible = true
                set(raw, 2L)
            }
            raw
        }

        service.crawlHotdealRaw(PlatformType.COOLENJOY_JIRUM, 1, 0L)

        verify(hotdealRawRepository).save(any<HotdealRaw>())
    }
}
