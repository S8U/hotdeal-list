package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.entity.HotdealProcess
import com.github.s8u.hotdeallist.entity.HotdealRaw
import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.exception.BusinessException
import com.github.s8u.hotdeallist.repository.HotdealProcessRepository
import com.github.s8u.hotdeallist.repository.HotdealRawRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.prompt.Prompt
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class HotdealProcessServiceTest {

    @Mock
    lateinit var hotdealRawRepository: HotdealRawRepository

    @Mock
    lateinit var hotdealProcessRepository: HotdealProcessRepository

    @Mock
    lateinit var chatModel: ChatModel

    private lateinit var service: HotdealProcessService

    @BeforeEach
    fun setUp() {
        service = HotdealProcessService(hotdealRawRepository, hotdealProcessRepository, chatModel)
    }

    private fun createHotdealRaw(id: Long, title: String, category: String? = null): HotdealRaw {
        val raw = HotdealRaw(
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "100",
            url = "https://coolenjoy.net/bbs/jirum/100",
            title = title,
            category = category,
            wroteAt = LocalDateTime.now()
        )
        HotdealRaw::class.java.superclass.getDeclaredField("id").apply {
            isAccessible = true
            set(raw, id)
        }
        return raw
    }

    private fun mockChatResponse(responseText: String): ChatResponse {
        val assistantMessage = AssistantMessage(responseText)
        val generation = Generation(assistantMessage)
        return ChatResponse(listOf(generation))
    }

    @Test
    fun `존재하지 않는 rawId로 처리 시 BusinessException을 발생시킨다`() {
        whenever(hotdealRawRepository.findById(999L)).thenReturn(Optional.empty())

        val exception = assertThrows(BusinessException::class.java) {
            service.processHotdealFromRaw(999L)
        }
        assertEquals("핫딜 원본 데이터를 찾을 수 없습니다.", exception.message)
    }

    @Test
    fun `AI 응답이 null이면 BusinessException을 발생시킨다`() {
        val raw = createHotdealRaw(1L, "[네이버] 갤럭시 S25")

        whenever(hotdealRawRepository.findById(1L)).thenReturn(Optional.of(raw))

        val nullMessage = AssistantMessage(null as String?)
        val generation = Generation(nullMessage)
        val chatResponse = ChatResponse(listOf(generation))
        whenever(chatModel.call(any<Prompt>())).thenReturn(chatResponse)

        val exception = assertThrows(BusinessException::class.java) {
            service.processHotdealFromRaw(1L)
        }
        assertEquals("AI 응답이 없습니다.", exception.message)
    }

    @Test
    fun `AI 응답 라인이 8줄 미만이면 BusinessException을 발생시킨다`() {
        val raw = createHotdealRaw(1L, "[네이버] 갤럭시 S25")

        whenever(hotdealRawRepository.findById(1L)).thenReturn(Optional.of(raw))

        val chatResponse = mockChatResponse("line1\nline2\nline3")
        whenever(chatModel.call(any<Prompt>())).thenReturn(chatResponse)

        val exception = assertThrows(BusinessException::class.java) {
            service.processHotdealFromRaw(1L)
        }
        assertEquals("AI 응답 형식이 올바르지 않습니다.", exception.message)
    }

    @Test
    fun `정상 AI 응답을 파싱하여 HotdealProcess를 저장한다`() {
        val raw = createHotdealRaw(1L, "[네이버] 삼성전자 갤럭시 S25 울트라 256GB")

        whenever(hotdealRawRepository.findById(1L)).thenReturn(Optional.of(raw))

        val aiResponse = """
            [Naver] Samsung Galaxy S25 Ultra 256GB
            갤럭시 S25 울트라 256GB
            Galaxy S25 Ultra 256GB
            smartphone
            0.97
            네이버
            KRW
            1320000
        """.trimIndent()
        val chatResponse = mockChatResponse(aiResponse)
        whenever(chatModel.call(any<Prompt>())).thenReturn(chatResponse)
        whenever(hotdealProcessRepository.save(any<HotdealProcess>())).thenAnswer { it.getArgument(0) }

        service.processHotdealFromRaw(1L)

        verify(hotdealProcessRepository).save(argThat<HotdealProcess> { process ->
            process.hotdealRawId == 1L &&
            process.titleEn == "[Naver] Samsung Galaxy S25 Ultra 256GB" &&
            process.productName == "갤럭시 S25 울트라 256GB" &&
            process.productNameEn == "Galaxy S25 Ultra 256GB" &&
            process.categoryCode == "smartphone" &&
            process.categoryConfidence == BigDecimal("0.97") &&
            process.shoppingPlatform == "네이버" &&
            process.currencyUnit == "KRW" &&
            process.price == BigDecimal("1320000")
        })
    }

    @Test
    fun `AI 응답에서 null 값은 null로 처리된다`() {
        val raw = createHotdealRaw(1L, "[네이버페이] 일일적립")

        whenever(hotdealRawRepository.findById(1L)).thenReturn(Optional.of(raw))

        val aiResponse = """
            [Naver Pay] Daily Rewards
            네이버페이 일일적립
            Naver Pay Daily Rewards
            point
            0.85
            네이버페이
            null
            null
        """.trimIndent()
        val chatResponse = mockChatResponse(aiResponse)
        whenever(chatModel.call(any<Prompt>())).thenReturn(chatResponse)
        whenever(hotdealProcessRepository.save(any<HotdealProcess>())).thenAnswer { it.getArgument(0) }

        service.processHotdealFromRaw(1L)

        verify(hotdealProcessRepository).save(argThat<HotdealProcess> { process ->
            process.shoppingPlatform == "네이버페이" &&
            process.currencyUnit == null &&
            process.price == null
        })
    }

    @Test
    fun `무료 모델 실패 시 유료 모델로 폴백한다`() {
        val raw = createHotdealRaw(1L, "테스트 핫딜")

        whenever(hotdealRawRepository.findById(1L)).thenReturn(Optional.of(raw))

        val aiResponse = """
            Test Hotdeal
            테스트 상품
            Test Product
            etc
            0.50
            null
            KRW
            10000
        """.trimIndent()

        // 첫 번째 호출(무료 모델)은 실패, 두 번째 호출(유료 모델)은 성공
        whenever(chatModel.call(any<Prompt>()))
            .thenThrow(RuntimeException("free model error"))
            .thenReturn(mockChatResponse(aiResponse))
        whenever(hotdealProcessRepository.save(any<HotdealProcess>())).thenAnswer { it.getArgument(0) }

        service.processHotdealFromRaw(1L)

        verify(chatModel, times(2)).call(any<Prompt>())
        verify(hotdealProcessRepository).save(argThat<HotdealProcess> { process ->
            process.aiModel == "openai/gpt-oss-120b"
        })
    }

    @Test
    fun `카테고리가 있는 raw 데이터의 프롬프트에 카테고리가 포함된다`() {
        val raw = createHotdealRaw(1L, "테스트 핫딜", category = "전자기기")

        whenever(hotdealRawRepository.findById(1L)).thenReturn(Optional.of(raw))

        val aiResponse = """
            Test
            테스트
            Test
            electronics
            0.90
            null
            KRW
            null
        """.trimIndent()
        whenever(chatModel.call(any<Prompt>())).thenReturn(mockChatResponse(aiResponse))
        whenever(hotdealProcessRepository.save(any<HotdealProcess>())).thenAnswer { it.getArgument(0) }

        service.processHotdealFromRaw(1L)

        verify(hotdealProcessRepository).save(argThat<HotdealProcess> { process ->
            process.aiPrompt.contains("Category: 전자기기")
        })
    }

    @Test
    fun `카테고리가 없는 raw 데이터의 프롬프트에 카테고리가 비어있다`() {
        val raw = createHotdealRaw(1L, "테스트 핫딜", category = null)

        whenever(hotdealRawRepository.findById(1L)).thenReturn(Optional.of(raw))

        val aiResponse = """
            Test
            테스트
            Test
            etc
            0.50
            null
            KRW
            null
        """.trimIndent()
        whenever(chatModel.call(any<Prompt>())).thenReturn(mockChatResponse(aiResponse))
        whenever(hotdealProcessRepository.save(any<HotdealProcess>())).thenAnswer { it.getArgument(0) }

        service.processHotdealFromRaw(1L)

        verify(hotdealProcessRepository).save(argThat<HotdealProcess> { process ->
            !process.aiPrompt.contains("Category:")
        })
    }
}
