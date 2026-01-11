package com.github.s8u.hotdeallist.service

import com.github.s8u.hotdeallist.entity.HotdealProcess
import com.github.s8u.hotdeallist.exception.BusinessException
import com.github.s8u.hotdeallist.repository.HotdealProcessRepository
import com.github.s8u.hotdeallist.repository.HotdealRawRepository
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class HotdealProcessService(
    private val hotdealRawRepository: HotdealRawRepository,
    private val hotdealProcessRepository: HotdealProcessRepository,
    private val chatModel: ChatModel
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun processHotdealFromRaw(rawId: Long) {
        logger.info("Processing hotdeal from rawId={}", rawId)

        val hotdealRaw = hotdealRawRepository.findById(rawId)
            .orElseThrow { BusinessException("핫딜 원본 데이터를 찾을 수 없습니다.") }

        // 요청
        val prompt = PROMPT_TEMPLATE
            .replace("{title}", hotdealRaw.title)
            .replace("{category}", hotdealRaw.category?.let { "Category: $it" } ?: "")

        // 무료 모델로 요청
        var model = ""
        val chatResponse = try {
            logger.debug("Sending AI request: rawId={}, title={}", rawId, hotdealRaw.title)

            model = "openai/gpt-oss-120b:free"
            val options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.0)
                .build()
            chatModel.call(Prompt(prompt, options))
        }
        // 실패 시 유료 모델로 요청
        catch (e: Exception) {
            logger.warn("Free model failed, falling back to paid: rawId={}, error={}", rawId, e.message)

            model = "openai/gpt-oss-120b"
            val options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.0)
                .build()
            chatModel.call(Prompt(prompt, options))
        }

        // 응답
        val response = chatResponse.result.output.text
        if (response == null) {
            logger.error("AI response is null: rawId={}", rawId)
            throw BusinessException("AI 응답이 없습니다.")
        }

        logger.debug("Received AI response: rawId={}, response={}", rawId, response)

        val lines = response.trim().split("\n").filter { it.isNotBlank() }
        if (lines.size < 8) {
            logger.error("Invalid AI response format: rawId={}, linesCount={}, response={}", rawId, lines.size, response)
            throw BusinessException("AI 응답 형식이 올바르지 않습니다.")
        }

        val titleEn = lines[0].trim()
        val productName = lines[1].trim()
        val productNameEn = lines[2].trim()
        val categoryCode = lines[3].trim()
        val categoryConfidence = BigDecimal(lines[4].trim())
        val shoppingPlatform = lines[5].trim().let { if (it == "null" || it.isEmpty()) null else it }
        val currencyUnit = lines[6].trim().let { if (it == "null" || it.isEmpty()) null else it }
        val price = lines[7].trim().let { if (it == "null" || it.isEmpty()) null else it.toBigDecimalOrNull() }

        // 저장
        var hotdealProcess = HotdealProcess(
            hotdealRawId = rawId,
            aiPrompt = prompt,
            aiResponse = response,
            aiModel = model,
            title = hotdealRaw.title,
            titleEn = titleEn,
            productName = productName,
            productNameEn = productNameEn,
            categoryCode = categoryCode,
            categoryConfidence = categoryConfidence,
            shoppingPlatform = shoppingPlatform,
            price = price,
            currencyUnit = currencyUnit
        )

        hotdealProcess = hotdealProcessRepository.save(hotdealProcess)
        logger.info("AI process created: id={}, rawId={}, title={}, titleEn={}, productName={}, productNameEn={}, categoryCode={}, categoryConfidence={}, shoppingPlatform={}, price={}, currencyUnit={}",
                     hotdealProcess.id, rawId, hotdealRaw.title, titleEn, productName, productNameEn, categoryCode, categoryConfidence, shoppingPlatform, price, currencyUnit)
    }

    companion object {
        private const val PROMPT_TEMPLATE = """
You are a product classifier for community hotdeal posts.

CONTEXT: You are analyzing titles from Korean online community hotdeal boards. These are user-written posts sharing product deals, not official product listings.

Classify products into the hierarchy below ONLY.

RULES:
- Output exactly 8 lines. Each line must contain ONLY the raw value, NO labels or prefixes like "Title:" or "Product name:".
- Category: Select ONE most specific category code from the list below. (e.g., prefer 'smartphone' over 'mobile' or 'electronics').
  * If confidence is low (<0.5), classify as 'etc' category instead.
- Category Confidence: 0.00–1.00, reflect certainty of category classification only.
- Product name:
  * FORMAT: {brandName | if exists} {productName | if exists, comma-separated} {capacity|quantity | if exists, comma-separated}
  * IF NOT PRODUCT, FORMAT: {brandName | if exists} {summary}
  * Keep brand, model, and capacity/quantity if they exist
  Keep product category/type (e.g., 도어락, 게이밍마우스, 이어폰) and key features (e.g., 지문인식, 푸시풀, 무선)
  * If multiple models exist, list all models
  * REMOVE shopping platforms UNLESS the platform itself is the product
  * REMOVE prices, promotions, discount info
- Platform: Extract shopping platform from title (e.g., 네이버, 쿠팡, G마켓). If none, output "null".
- Currency: Extract currency unit (KRW, USD, JPY, CNY, EUR). If not explicitly mentioned, default to "KRW".
- Price: Extract FINAL PRODUCT price (Number only, e.g., 1320000).
  * If format is "price1/price2", price1 is usually product price, price2 is shipping fee → use price1
  * Ignore shipping fees (배송비, 무료배송, etc.)
  * If multiple product prices exist, use LOWEST final price
  * If none, output "null"

OUTPUT FORMAT (8 lines, values only, no labels):
Line 1: Title translated to English
Line 2: Product name in Korean
Line 3: Product name in English
Line 4: Category code
Line 5: Category confidence
Line 6: Shopping platform
Line 7: Currency unit
Line 8: Price

---

CATEGORIES:

L1: electronics, auto_tools, fashion, beauty, food, living, hobby, etc

L2:
electronics: mobile, computer, av, game, appliance
auto_tools: auto_accessory, tools
fashion: men_clothing, women_clothing, fashion_accessory
beauty: skincare, makeup, hair_body
food: fresh_food, processed_food, ready_meal, beverage
living: furniture, interior, living_goods
hobby: sports, book_stationery
etc: subscription, gift_card, gifticon, point

L3:
mobile: smartphone, tablet, smartwatch, mobile_accessory
computer: laptop, desktop, computer_parts, peripheral
av: tv, mic, speaker, earphone_headphone, home_theater
game: console, vr, game_software, game_peripheral
appliance: refrigerator, washing_machine, vacuum, air_purifier, kitchen_appliance
auto_accessory: blackbox, car_accessory, tire_wheel
tools: power_tool, hand_tool
men_clothing: men_top, men_bottom, men_outer, suit
women_clothing: women_top, women_bottom, dress, women_outer
fashion_accessory: bag, shoes, wallet, hat_belt
fresh_food: fruit, vegetable, meat_seafood
processed_food: snack, chocolate_candy, bakery, frozen_food, instant_food, canned_food
ready_meal: meal_kit, lunchbox, ramen
beverage: coffee, tea, juice_soda
furniture: bed, desk, chair, storage_furniture
interior: lighting, curtain, interior_accessory
living_goods: kitchen_goods, bathroom_goods, cleaning_goods
sports: fitness, bicycle, camping
book_stationery: book, stationery, diary

L4:
computer_parts: cpu, mainboard, ram, gpu, storage, psu, computer_case, cooling, expansion_card, cable_gender
storage: ssd, hdd, nas, external_storage
cooling: cpu_cooler, case_fan, water_cooling
expansion_card: sound_card, network_card
peripheral: monitor, keyboard, mouse, printer_scanner, webcam, usb
mobile_accessory: case, screen_protector, charger_cable

---

EXAMPLES:

"[네이버] 삼성전자 갤럭시 S25 울트라 256GB 512GB SM-S938N 자급제 (150만/실체감가132만)"
[Naver] Samsung Galaxy S25 Ultra 256GB 512GB SM-S938N Unlocked (1.5M/1.32M Effective)
갤럭시 S25 울트라 256GB, 512GB
Galaxy S25 Ultra 256GB, 512GB
smartphone
0.97
네이버
KRW
1320000

"[네이버페이] 일일적립"
[Naver Pay] Daily Rewards
네이버페이 일일적립
Naver Pay Daily Rewards
point
0.85
네이버페이
null
null

"[쿠팡] 펩시콜라 500ml x 24병 (15,900원/3,000원)"
[Coupang] Pepsi Cola 500ml x 24 bottles (15,900 KRW/3,000 KRW)
펩시콜라 500ml 24병
Pepsi Cola 500ml 24 bottles
beverage
0.95
쿠팡
KRW
15900

"[Amazon] Apple AirPods Pro 2nd Gen ($189.99)"
[Amazon] Apple AirPods Pro 2nd Gen ($189.99)
애플 에어팟 프로 2세대
Apple AirPods Pro 2nd Gen
earphone_headphone
0.98
Amazon
USD
189

---

Title: {title}
{category}
"""
    }
}