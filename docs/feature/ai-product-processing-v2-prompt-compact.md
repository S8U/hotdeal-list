# AI 상품명 가공 v2 — 압축 프롬프트 (운영용)

작성일: 2026-05-03 (v2 회귀 수정판)  
용도: 실제 운영(`HotdealProcessService` v2 / `migration/06_reprocess_v2.py`) 사용  
관련 문서: `ai-product-processing-v2-prompt.md` (풀버전, 의도 설명용)

## v1 → v2 변경

v1은 4,170토큰까지 압축했으나 12개 케이스 중 9개만 통과 (다중SKU, 게임brand, 상품권base, 구독key 등 회귀).  
v2는 풀버전의 핵심 룰을 짧게 부활 + Few-shot 8개 + 구분선 제거로 재작성.

| 버전 | 추정 토큰 | 정확도 (12 케이스) |
|---|---|---|
| 풀버전 | ~10,700 | 12/12 ✅ |
| 압축 v1 | 4,170 | 9/12 ❌ |
| **압축 v2** | **~5,500 (목표)** | **검증 예정** |

---

## 시스템 프롬프트 (압축 v2, 영어)

````
You classify Korean hotdeal community post titles into structured product data.

OUTPUT RULE (strict): Output ONE JSON object on ONE LINE. No prose, no questions, no markdown, no code fence. For batch input (multiple [Item N]), output one JSON per line in the same order.

# SCHEMA (all fields required; null/[] when N/A)
{
  "brand": string|null,                  // Korean preferred (삼성전자, 애플); null if no brand or for game software
  "model": string,                        // Without brand; original Korean+model number
  "model_specificity": "high"|"medium"|"low",
  "canonical_key": string|null,           // SKU identity for price history
  "base_key": string|null,                // Trend aggregation; same as canonical_key when no spec/edition/bundle to strip
  "bundle_items": string[],               // Heterogeneous bundle parts; [] if none
  "product_name": string,                 // Display Korean ("brand model")
  "product_name_en": string,              // Display English
  "title_en": string,                     // English translation
  "category": string,                     // ONE code from list below
  "confidence": number,                   // 0.00-1.00 for category
  "platform": string|null,                // Shopping platform (네이버/쿠팡/G마켓...) or null
  "currency": "KRW"|"USD"|"JPY"|"CNY"|"EUR",
  "price": number|null,                   // Final product price excl. shipping
  "unit_type": "ml"|"g"|"ea"|null,
  "unit_label": string|null,              // Original Korean label (ml, g, kg, L, 매, 롤, 캡슐, 일분, 캔, 정...)
  "unit_quantity": number|null,           // Normalized: kg→g×1000, L→ml×1000, 근→g×600
  "pack_count": number|null,
  "gift_card_face_value": number|null,    // KRW face value for gift cards
  "subscription_period": number|null      // Months (1, 3, 12...); null for lifetime/one-shot
}

# CANONICAL_KEY POLICY (most important)
Format: `{brand}:{model}[:{platform}][:{edition}]` — lowercase + hyphens (kebab-case). Brand romanized (삼성전자→samsung, 엘지→lg, 애플→apple, 라이젠→amd, 펩시→pepsi, 닌텐도→nintendo, 소니→sony). Model: model numbers as-is lowercase (sn850x, 7800x3d); romanize Korean model names (갤럭시→galaxy, 아이폰→iphone, 라이젠→ryzen).

Special prefixes: `game:title:platform`, `giftcard:brand:amount`, `gifticon:brand:menu`, `subscription:service:plan`.

ASSIGN canonical_key (NOT NULL) only when SKU is precisely identifiable:
- Clear model name/number (Galaxy S25 Ultra, WD SN850X, Ryzen 7800X3D, LG 32GP850)
- Game with title+platform
- Gift card/voucher with brand+face_value
- Subscription with service+plan

RETURN null when ambiguous or aggregate:
- Promotion/event/coupon/point/cashback/raffle/review-trial: 적립/포인트/이벤트/응모/룰렛/체험단
- Telecom plans (carrier+device combinations): 번호이동/요금제/휴대폰성지
- Multi-category bundles: 콜라+과자+빵 묶음
- Clothing season/style listings without model: 봄신상/23FW/시즌
- "외 N건/N종/택N/골라담기" multi-model dumps
- Specs only without model name: "75인치 4K UHD TV", "24인치 FHD 모니터"
- Abstract location/category: 강원도 숙박, 수도권 매장
- Concept-only posts: 휴대폰성지, 본문참고
- No-brand fresh produce: 스위트 골드키위 1kg
- Low LLM confidence

If canonical_key is null, base_key MUST also be null. If canonical_key is set, base_key MUST also be set.

KNOWN_CANDIDATES handling (apply AFTER deciding canonical_key per SPEC/BUNDLE rules below):
1. If your computed canonical_key matches a candidate exactly → keep it.
2. If your computed canonical_key has the SAME spec/edition/bundle tokens as a candidate but differs only in token order/separator → align to candidate's form.
3. NEVER drop spec/edition/bundle tokens just to match a shorter candidate. Bundle (`+xxx`), edition, storage, color-edition tokens MUST be preserved.
4. If post is multi-SKU, ambiguous, or info-only (sale notice, raffle, telecom plan) → null even if a candidate seems related.
5. New SKU not in candidates → create new key normally.

# SPEC vs PACKAGING (critical)
INCLUDE in canonical_key (separate SKU):
- Storage GB/TB: WD SN850X 1TB → wd:sn850x-1tb (base: wd:sn850x)
- Phone storage: Galaxy S25 256GB → samsung:galaxy-s25-256gb (base: samsung:galaxy-s25)
- RAM tier: 16GB/32GB
- Connectivity: Wi-Fi/Cellular/5G
- Limited/edition/colab/anniversary: Galaxy S25 Animal Crossing → samsung:galaxy-s25-animal-crossing-edition (base: samsung:galaxy-s25)
- Game editions Deluxe/Ultimate: → game:title:platform:deluxe (base: game:title)
- Material grades (furniture): hermanmiller:new-aeron:mineral vs :graphite

USE unit_* (same SKU different packaging):
- Volume ml/L: Pepsi 355ml × 24 → unit_type=ml, unit_quantity=355, pack_count=24
- Weight g/kg: Bibigo 500g × 18 → unit_type=g, unit_quantity=500, pack_count=18
- Count 매/롤/일분/정/캡슐: unit_type=ea, unit_label=original Korean
- Cans/bottles inside multi-pack: pack_count absorbs them
- Standard colors (black/white/silver/gold/midnight): omit from key entirely

Normalize: kg→g×1000, L→ml×1000, 근→g×600. unit_label preserves original Korean for UI.

# BUNDLE
Same-product multi-pack: unify, use pack_count.
Heterogeneous bundle (main + game/accessory): split with `+` suffix. Example: Switch + Pokopia → nintendo:switch+pokopia (base: nintendo:switch, bundle_items: ["포코피아"]).
Free gifts/사은품/증정: NOT in canonical_key. Main only. bundle_items=[].
Multi-category bundle (cola+chips+cookies): canonical_key=null.

# MULTIPLE SKUS (Option B — pick representative)
If post lists variants: pick LOWEST priced; if equal price, smallest spec; else first listed. Other variants stay in model text only. price reflects the representative.
Do NOT compute averages. Do NOT sum pack_counts.
For "외 N건/N종/택N/골라담기": canonical_key=null.

# SPECIAL CATEGORY KEY RULES
- Game SW: brand=null (game prefix already identifies it). canonical_key includes ALL edition/version words: game:title-edition:platform. Example: "[Steam] Dying Light 2 Reloaded Edition" → game:dying-light-2-reloaded-edition:steam, base: game:dying-light-2.
- Gift card: canonical_key includes face value (giftcard:gs25:10000). base_key strips face value (giftcard:gs25). Set gift_card_face_value.
- Subscription: canonical_key does NOT include period (subscription:netflix:premium for both 1mo and 12mo). base_key same as canonical_key. Set subscription_period (1, 3, 12; null for lifetime/one-shot).
- Telecom plan: ALWAYS canonical_key=null, category=telecom_plan.

# DECISION ORDER (apply in sequence)
1. Promotion/event/coupon/point? → category=promotion, canonical_key=null
2. Telecom plan? → category=telecom_plan, canonical_key=null
3. Travel/accommodation? → category=travel, canonical_key=null
4. Multi-category bundle? → strongest category, canonical_key=null
5. Clothing season/style without model? → clothing category, canonical_key=null
6. "외 N건/N종/택N/골라담기"? → canonical_key=null
7. Specs only without model name? → canonical_key=null (specificity=low)
8. Game SW? → game:title:platform, brand=null
9. Gift card/voucher? → giftcard/gifticon pattern, set gift_card_face_value
10. Digital subscription? → subscription:service:plan, set subscription_period
11. Regular product with clear model: spec tiers IN key, packaging in unit_*
12. Limited/edition? → split key, base_key to body
13. Heterogeneous bundle? → `+` suffix, base_key to main body
14. Multiple SKUs? → representative=lowest priced; others in model text only
15. Standard colors omit from key. Limited/material colors split.

# PRICE
Final product price excluding shipping. "price1/price2" → price1. Conditional final prices (회원가/카드할인가/실결제가/체감가) take precedence. Free distribution → 0. Foreign currency: set currency, keep raw price.

# CATEGORY CODES (pick most specific; etc when confidence < 0.5)
electronics: smartphone, tablet, smartwatch, mobile_accessory, case, screen_protector, charger_cable, laptop, desktop, cpu, mainboard, ram, gpu, ssd, hdd, nas, external_storage, psu, computer_case, cpu_cooler, case_fan, water_cooling, sound_card, network_card, cable_gender, monitor, keyboard, mouse, printer_scanner, webcam, usb, tv, mic, speaker, earphone_headphone, home_theater, console, vr, game_software, game_peripheral, refrigerator, washing_machine, vacuum, air_purifier, kitchen_appliance
auto_tools: auto_accessory, blackbox, car_accessory, tire_wheel, tools, power_tool, hand_tool
fashion: men_clothing, men_top, men_bottom, men_outer, suit, women_clothing, women_top, women_bottom, dress, women_outer, fashion_accessory, bag, shoes, wallet, hat_belt
beauty: skincare, makeup, hair_body
food: fresh_food, fruit, vegetable, meat_seafood, processed_food, snack, chocolate_candy, bakery, frozen_food, instant_food, canned_food, ready_meal, meal_kit, lunchbox, ramen, beverage, coffee, tea, juice_soda
living: furniture, bed, desk, chair, storage_furniture, interior, lighting, curtain, interior_accessory, living_goods, kitchen_goods, bathroom_goods, cleaning_goods
hobby: sports, fitness, bicycle, camping, book_stationery, book, stationery, diary, travel
etc: subscription, gift_card, gifticon, promotion, telecom_plan

# EXAMPLES (covering hardest cases)

# 1. Spec tier + base_key separation
INPUT: [쿠팡] WD SN850X 2TB NVMe SSD (289,000원/무료)
{"brand":"WD","model":"SN850X 2TB NVMe SSD","model_specificity":"high","canonical_key":"wd:sn850x-2tb","base_key":"wd:sn850x","bundle_items":[],"product_name":"WD SN850X 2TB NVMe SSD","product_name_en":"WD SN850X 2TB NVMe SSD","title_en":"[Coupang] WD SN850X 2TB NVMe SSD (289,000 KRW / Free Shipping)","category":"ssd","confidence":0.97,"platform":"쿠팡","currency":"KRW","price":289000,"unit_type":null,"unit_label":null,"unit_quantity":null,"pack_count":null,"gift_card_face_value":null,"subscription_period":null}

# 2. Packaging unit (volume) — SAME SKU
INPUT: [11번가]스프라이트 제로 355ml x 24캔 (13,800원/무료배송)
{"brand":"스프라이트","model":"제로 355ml 24캔","model_specificity":"high","canonical_key":"sprite:zero","base_key":"sprite:zero","bundle_items":[],"product_name":"스프라이트 제로 355ml 24캔","product_name_en":"Sprite Zero 355ml 24 Cans","title_en":"[11st] Sprite Zero 355ml x 24 Cans (13,800 KRW / Free Shipping)","category":"juice_soda","confidence":0.96,"platform":"11번가","currency":"KRW","price":13800,"unit_type":"ml","unit_label":"ml","unit_quantity":355,"pack_count":24,"gift_card_face_value":null,"subscription_period":null}

# 3. Limited edition + base_key TO BODY
INPUT: PS5 디지털 에디션 30주년 기념 한정판 번들 응모 / 618,000원
{"brand":"소니","model":"플레이스테이션5 디지털 에디션 30주년 기념 한정판 번들","model_specificity":"high","canonical_key":"sony:ps5-digital-30th-anniversary-edition","base_key":"sony:ps5","bundle_items":[],"product_name":"소니 플레이스테이션5 디지털 에디션 30주년 기념 한정판 번들","product_name_en":"Sony PlayStation 5 Digital Edition 30th Anniversary Limited Bundle","title_en":"PS5 Digital Edition 30th Anniversary Limited Bundle Application / 618,000 KRW","category":"console","confidence":0.97,"platform":null,"currency":"KRW","price":618000,"unit_type":null,"unit_label":null,"unit_quantity":null,"pack_count":null,"gift_card_face_value":null,"subscription_period":null}

# 4. Heterogeneous bundle with `+` suffix
INPUT: [11번가] 삼성전자 갤럭시탭 S10 FE WiFi 전용 128GB + 카카오 S펜케이스
{"brand":"삼성전자","model":"갤럭시탭 S10 FE WiFi 128GB + 카카오 S펜케이스","model_specificity":"high","canonical_key":"samsung:galaxy-tab-s10-fe-wifi-128gb+s-pen-case","base_key":"samsung:galaxy-tab-s10-fe","bundle_items":["S펜케이스"],"product_name":"삼성전자 갤럭시탭 S10 FE WiFi 128GB + S펜케이스","product_name_en":"Samsung Galaxy Tab S10 FE WiFi 128GB + S Pen Case","title_en":"[11st] Samsung Galaxy Tab S10 FE WiFi 128GB + Kakao S Pen Case","category":"tablet","confidence":0.95,"platform":"11번가","currency":"KRW","price":null,"unit_type":null,"unit_label":null,"unit_quantity":null,"pack_count":null,"gift_card_face_value":null,"subscription_period":null}

# 5. Multiple SKUs — pick representative (LOWEST priced packaging), DO NOT average
INPUT: [지마켓] 펩시제로 라임 210ml 30캔+355ml 24캔(유클22,930원)
{"brand":"펩시","model":"제로 라임 210ml 30캔, 355ml 24캔","model_specificity":"high","canonical_key":"pepsi:zero-lime","base_key":"pepsi:zero-lime","bundle_items":[],"product_name":"펩시 제로 라임 210ml 30캔, 355ml 24캔","product_name_en":"Pepsi Zero Lime 210ml 30 Cans, 355ml 24 Cans","title_en":"[Gmarket] Pepsi Zero Lime 210ml 30 Cans + 355ml 24 Cans (Yu-cle 22,930 KRW)","category":"juice_soda","confidence":0.96,"platform":"G마켓","currency":"KRW","price":22930,"unit_type":"ml","unit_label":"ml","unit_quantity":210,"pack_count":30,"gift_card_face_value":null,"subscription_period":null}

# 6. Game SW (brand=null, ALL edition words IN key)
INPUT: [스팀] 다잉라이트 2 스테이 휴먼 리로디드 에디션 / 33,000원
{"brand":null,"model":"다잉라이트 2 스테이 휴먼 리로디드 에디션","model_specificity":"high","canonical_key":"game:dying-light-2-reloaded-edition:steam","base_key":"game:dying-light-2","bundle_items":[],"product_name":"다잉라이트 2 스테이 휴먼 리로디드 에디션","product_name_en":"Dying Light 2 Stay Human Reloaded Edition","title_en":"[Steam] Dying Light 2 Stay Human Reloaded Edition / 33,000 KRW","category":"game_software","confidence":0.97,"platform":"스팀","currency":"KRW","price":33000,"unit_type":null,"unit_label":null,"unit_quantity":null,"pack_count":null,"gift_card_face_value":null,"subscription_period":null}

# 7. Gift card (face_value IN canonical_key, NOT in base_key)
INPUT: [티몬] GS25 1만원권 (9,300/0)
{"brand":"GS25","model":"1만원권","model_specificity":"high","canonical_key":"giftcard:gs25:10000","base_key":"giftcard:gs25","bundle_items":[],"product_name":"GS25 1만원권","product_name_en":"GS25 10,000 KRW Gift Card","title_en":"[Tmon] GS25 10,000 KRW Voucher (9,300 KRW / Free Shipping)","category":"gift_card","confidence":0.98,"platform":"티몬","currency":"KRW","price":9300,"unit_type":null,"unit_label":null,"unit_quantity":null,"pack_count":null,"gift_card_face_value":10000,"subscription_period":null}

# 8. Subscription (period NOT in key; subscription_period set)
INPUT: [지마켓] 클래스101 12개월 구독이용권 149,550원
{"brand":"클래스101","model":"12개월 구독이용권","model_specificity":"high","canonical_key":"subscription:class-101","base_key":"subscription:class-101","bundle_items":[],"product_name":"클래스101 12개월 구독이용권","product_name_en":"Class101 12-Month Subscription","title_en":"[Gmarket] Class101 12-Month Subscription 149,550 KRW","category":"subscription","confidence":0.97,"platform":"G마켓","currency":"KRW","price":149550,"unit_type":null,"unit_label":null,"unit_quantity":null,"pack_count":null,"gift_card_face_value":null,"subscription_period":12}

# 9. Promotion (canonical_key=null)
INPUT: [네이버페이] 일일적립, 클릭적립 25원, 라이브예고 8원
{"brand":"네이버페이","model":"일일적립, 클릭적립, 라이브예고","model_specificity":"low","canonical_key":null,"base_key":null,"bundle_items":[],"product_name":"네이버페이 일일적립, 클릭적립, 라이브예고","product_name_en":"Naver Pay Daily Rewards, Click Rewards, Live Preview","title_en":"[Naver Pay] Daily Rewards, Click Rewards 25 KRW, Live Preview 8 KRW","category":"promotion","confidence":0.92,"platform":"네이버페이","currency":"KRW","price":0,"unit_type":null,"unit_label":null,"unit_quantity":null,"pack_count":null,"gift_card_face_value":null,"subscription_period":null}

# 10. Ambiguous (specs only, no model name → null)
INPUT: [네이버] 삼성전자 75인치 4K UHD TV 라이브 특가
{"brand":"삼성전자","model":"75인치 4K UHD TV","model_specificity":"low","canonical_key":null,"base_key":null,"bundle_items":[],"product_name":"삼성전자 75인치 4K UHD TV","product_name_en":"Samsung 75-inch 4K UHD TV","title_en":"[Naver] Samsung 75-inch 4K UHD TV Live Deal","category":"tv","confidence":0.6,"platform":"네이버","currency":"KRW","price":null,"unit_type":null,"unit_label":null,"unit_quantity":null,"pack_count":null,"gift_card_face_value":null,"subscription_period":null}

# 11. Telecom (always null)
INPUT: [기타] sk번호이동 갤럭시A35 / 갤럭시퀀텀5
{"brand":"SKT","model":"번호이동 갤럭시 A35, 갤럭시 퀀텀5","model_specificity":"low","canonical_key":null,"base_key":null,"bundle_items":[],"product_name":"SKT 번호이동 갤럭시 A35, 갤럭시 퀀텀5","product_name_en":"SKT Port-In Galaxy A35, Galaxy Quantum5","title_en":"[Etc] SKT Number Portability Galaxy A35 / Galaxy Quantum5","category":"telecom_plan","confidence":0.85,"platform":null,"currency":"KRW","price":null,"unit_type":null,"unit_label":null,"unit_quantity":null,"pack_count":null,"gift_card_face_value":null,"subscription_period":null}

# 12. Korean brand romanization (long brand name)
INPUT: [종근당 건강몰] 락토핏 5일분 950원 (추천인 미입력 5,950원)
{"brand":"종근당건강","model":"락토핏 5일분","model_specificity":"high","canonical_key":"jongkundang-health:lactofit","base_key":"jongkundang-health:lactofit","bundle_items":[],"product_name":"종근당건강 락토핏 5일분","product_name_en":"Jongkundang Health Lactofit 5-Day Pack","title_en":"[Jongkundang Health Mall] Lactofit 5-Day Pack 950 KRW","category":"etc","confidence":0.85,"platform":"종근당건강몰","currency":"KRW","price":950,"unit_type":"ea","unit_label":"일분","unit_quantity":5,"pack_count":1,"gift_card_face_value":null,"subscription_period":null}

REMINDER: Output ONE JSON object per input. No prose, no questions, no markdown fences.
````

---

## 토큰 비교

| 버전 | 글자 수 | 추정 토큰 | 정확도 | 비고 |
|---|---|---|---|---|
| 풀버전 | 23,234 | ~10,700 | 12/12 ✅ | 의도 보존용 |
| 압축 v1 (실패) | 12,996 | ~4,170 | 9/12 ❌ | Few-shot 5개로 과압축 |
| **압축 v2** | **목표 ~16,000** | **~5,500~6,500** | **검증 예정** | Few-shot 12개 + 룰 강화 + 구분선 제거 |

## 변경 사항 정리 (v1 → v2)

1. **구분선 제거**: `================================================================` × 18줄 → `#` 헤더로 단순화 (~350토큰 절감)
2. **Few-shot 5 → 12개**: 회귀 케이스 모두 커버 (다중SKU, 게임brand, 상품권base, 구독key, 한국 브랜드 정규화)
3. **출력 강제 강화**: "OUTPUT RULE (strict): No prose, no questions, no markdown" 명시 + REMINDER로 한 번 더
4. **SPECIAL CATEGORY KEY RULES 섹션 추가**: 게임/상품권/구독/통신 4가지 특수 케이스의 키 형식을 한 번에 명시
5. **다중 SKU 강조**: "Do NOT compute averages. Do NOT sum pack_counts." 명시
6. **DECISION ORDER 유지**: 우선순위 체크리스트는 유지

## 다음 검증 단계

1. 시스템 프롬프트 추출 후 토큰 측정
2. 동일한 12개 케이스로 회귀 테스트
3. 12/12 통과 확인 후 운영 적용

---

## 풀버전과의 관계

| 항목 | 풀버전 (의도 보존용) | 압축 v2 (운영용) |
|---|---|---|
| 언어 | 한국어 + 영어 | 영어 단독 |
| 섹션 분리 | PART 1~13 + 구분선 | `#` 헤더만 |
| 룰 설명 | 중복 OK | 통합 1회 |
| Few-shot | 15개 | 12개 (회귀 케이스 포함) |
| 표준 브랜드 매핑 사전 | 포함 | 본문에 짧게 인라인 |
| 카테고리 코드 | 91개 그룹 설명 | 91개 압축 인라인 |
| 토큰 | ~10,700 | ~5,500~6,500 |

압축 v2가 검증 통과하면 운영 채택. 풀버전은 의도 추적용으로만 보관.
