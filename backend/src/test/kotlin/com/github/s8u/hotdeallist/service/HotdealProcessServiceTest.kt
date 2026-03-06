package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.entity.HotdealProcess
import com.github.s8u.hotdeallist.entity.HotdealRaw
import com.github.s8u.hotdeallist.enums.PlatformType
import com.github.s8u.hotdeallist.exception.BusinessException
import com.github.s8u.hotdeallist.repository.HotdealProcessRepository
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
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.messages.AssistantMessage
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class HotdealProcessServiceTest {

    @MockK
    private lateinit var hotdealRawRepository: HotdealRawRepository

    @MockK
    private lateinit var hotdealProcessRepository: HotdealProcessRepository

    @MockK
    private lateinit var chatModel: ChatModel

    @InjectMockKs
    private lateinit var hotdealProcessService: HotdealProcessService

    private val now = LocalDateTime.of(2025, 1, 1, 12, 0)

    private fun setEntityId(entity: Any, id: Long) {
        val idField = entity.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }

    private fun createHotdealRaw(): HotdealRaw {
        val raw = HotdealRaw(
            platformType = PlatformType.COOLENJOY_JIRUM,
            platformPostId = "12345",
            url = "https://coolenjoy.net/bbs/jirum/12345",
            title = "[쿠팡] 삼성 갤럭시 S25 울트라 256GB (1,500,000원/무배)",
            category = "디지털",
            wroteAt = now
        )
        setEntityId(raw, 1L)
        return raw
    }

    private val validAiResponse = """
[Coupang] Samsung Galaxy S25 Ultra 256GB (1,500,000 KRW/Free Shipping)
삼성 갤럭시 S25 울트라 256GB
Samsung Galaxy S25 Ultra 256GB
smartphone
0.97
쿠팡
KRW
1500000
""".trim()

    private fun mockChatResponse(text: String?): ChatResponse {
        val chatResponse = mockk<ChatResponse>()
        val generation = mockk<Generation>()
        val assistantMessage = mockk<AssistantMessage>()

        every { chatResponse.result } returns generation
        every { generation.output } returns assistantMessage
        every { assistantMessage.text } returns text

        return chatResponse
    }

    @Nested
    @DisplayName("processHotdealFromRaw")
    inner class ProcessHotdealFromRaw {

        @Test
        @DisplayName("정상적으로 AI 가공 데이터를 생성한다")
        fun `should create process data from AI response`() {
            val raw = createHotdealRaw()

            every { hotdealRawRepository.findById(1L) } returns Optional.of(raw)
            every { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns mockChatResponse(validAiResponse)
            every { hotdealProcessRepository.save(any()) } answers {
                val process = firstArg<HotdealProcess>()
                setEntityId(process, 1L)
                process
            }

            hotdealProcessService.processHotdealFromRaw(1L)

            verify { hotdealProcessRepository.save(match<HotdealProcess> {
                it.productName == "삼성 갤럭시 S25 울트라 256GB" &&
                it.categoryCode == "smartphone" &&
                it.shoppingPlatform == "쿠팡" &&
                it.price?.toInt() == 1500000
            }) }
        }

        @Test
        @DisplayName("Raw 데이터가 없으면 BusinessException을 던진다")
        fun `should throw BusinessException when raw not found`() {
            every { hotdealRawRepository.findById(999L) } returns Optional.empty()

            assertThrows<BusinessException> {
                hotdealProcessService.processHotdealFromRaw(999L)
            }
        }

        @Test
        @DisplayName("AI 응답이 null이면 BusinessException을 던진다")
        fun `should throw BusinessException when AI response is null`() {
            val raw = createHotdealRaw()

            every { hotdealRawRepository.findById(1L) } returns Optional.of(raw)
            every { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns mockChatResponse(null)

            assertThrows<BusinessException> {
                hotdealProcessService.processHotdealFromRaw(1L)
            }
        }

        @Test
        @DisplayName("AI 응답 라인 수가 8줄 미만이면 BusinessException을 던진다")
        fun `should throw BusinessException when AI response has less than 8 lines`() {
            val raw = createHotdealRaw()
            val shortResponse = "Line1\nLine2\nLine3"

            every { hotdealRawRepository.findById(1L) } returns Optional.of(raw)
            every { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns mockChatResponse(shortResponse)

            assertThrows<BusinessException> {
                hotdealProcessService.processHotdealFromRaw(1L)
            }
        }

        @Test
        @DisplayName("무료 모델 실패 시 유료 모델로 폴백한다")
        fun `should fallback to paid model when free model fails`() {
            val raw = createHotdealRaw()

            every { hotdealRawRepository.findById(1L) } returns Optional.of(raw)
            every { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } throws
                RuntimeException("Free model error") andThen mockChatResponse(validAiResponse)
            every { hotdealProcessRepository.save(any()) } answers {
                val process = firstArg<HotdealProcess>()
                setEntityId(process, 1L)
                process
            }

            hotdealProcessService.processHotdealFromRaw(1L)

            verify(exactly = 2) { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) }
            verify { hotdealProcessRepository.save(match<HotdealProcess> { it.aiModel == "openai/gpt-oss-120b" }) }
        }

        @Test
        @DisplayName("쇼핑 플랫폼이 null이면 null로 저장한다")
        fun `should save null shopping platform when AI response is null`() {
            val raw = createHotdealRaw()
            val response = """
Title En
상품명
Product Name
electronics
0.80
null
KRW
null
""".trim()

            every { hotdealRawRepository.findById(1L) } returns Optional.of(raw)
            every { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns mockChatResponse(response)
            every { hotdealProcessRepository.save(any()) } answers {
                val process = firstArg<HotdealProcess>()
                setEntityId(process, 1L)
                process
            }

            hotdealProcessService.processHotdealFromRaw(1L)

            verify { hotdealProcessRepository.save(match<HotdealProcess> {
                it.shoppingPlatform == null && it.price == null
            }) }
        }

        @Test
        @DisplayName("카테고리가 있는 Raw 데이터의 프롬프트에 카테고리 정보를 포함한다")
        fun `should include category in prompt when raw has category`() {
            val raw = createHotdealRaw()

            every { hotdealRawRepository.findById(1L) } returns Optional.of(raw)
            every { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns mockChatResponse(validAiResponse)
            every { hotdealProcessRepository.save(any()) } answers {
                val process = firstArg<HotdealProcess>()
                setEntityId(process, 1L)
                process
            }

            hotdealProcessService.processHotdealFromRaw(1L)

            verify { hotdealProcessRepository.save(match<HotdealProcess> {
                it.aiPrompt.contains("Category: 디지털")
            }) }
        }
    }
}
