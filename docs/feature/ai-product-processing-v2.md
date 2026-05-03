# AI 상품명 가공 v2 — 설계 및 롤아웃 계획

작성일: 2026-05-03

## 1. 배경

### 1.1 현재 시스템
- 6개 한국 커뮤니티(쿨엔조이, 퀘이사존 핫딜/타세요, 클리앙, 루리웹, 뽐뿌)에서 핫딜 174,933건 수집
- LLM이 게시글 제목 → 8줄 평문 응답 → product_name, category, price 등 추출
- Spring AI(OpenRouter) + Python 배치(Anthropic SDK)

### 1.2 현황 진단 (DB 174,933건 + dev 8,789건 분석 결과)

| 문제 | 데이터 증거 |
|---|---|
| 가공 상품명 표기 불일치 | 펩시 같은 제품이 55개 variant로 분산. "AMD Ryzen 5800X3D" vs "AMD 라이젠 7800X3D" 영/한 혼재 |
| 트렌드 부정확 | TOP 30 product_name 중 7개가 "네이버페이 적립" 변형 (751건 + 708건 등) |
| 단위가격 추출 불가 | 단위 표기 65,381건(37.4%) 있는데 한 필드 평문으로만 들어감 |
| 다중상품 미고려 | 16% 게시글이 다중 SKU |
| 비상품/대분류 게시글 분류 부재 | 적립글, 행사 정리, 멀티카테고리 묶음 등이 그대로 product_name으로 들어가 트렌드 오염 |
| 가공 출력 비구조화 | 평문 8줄 응답 → 파싱 취약, 후속 활용 불가 |

### 1.3 소비처 의존성
- ES `productName` 필드: 검색 가중 ^3, 자동완성 Completion, 하이라이트
- 가격 히스토리: `productName` match_phrase slop=1 (정규화 깨지면 직격탄)
- 프론트: `deal-card.tsx`, `price-history-dialog.tsx` 등에서 productName 표시

---

## 2. 핵심 설계 원칙

### 2.1 canonical_key는 권리가 아니라 자격
명확한 SKU만 키 부여, 모호하거나 대분류면 **null**.

```
부여 가능:
- 모델명/모델번호가 명확한 일반 상품 (갤럭시 S25 울트라, WD SN850X)
- 게임 SW 타이틀+플랫폼 (데드 아일랜드2 + 스팀)
- 상품권 브랜드+액면가 (맥도날드 2만원권)
- 디지털 구독 서비스+플랜 (넷플릭스 프리미엄)

null로 두는 경우:
- 행사/세일 정리성 게시글 (GS25 9월 갓세일 정리)
- 멀티 카테고리 묶음 (펩시+새우깡+다이제)
- 통신 요금제 (조건 변수가 너무 큼)
- 의류 시즌/스타일 묶음 (모델명 없음)
- "외 N건", "N종 중 택1"
- 모델명 없이 스펙만 (24인치 FHD 모니터)
- 추상 위치/카테고리 (강원도 숙박)
- 정보성/개념성 게시글 (휴대폰성지)
- LLM 식별 신뢰도 낮음 → 보수적 null
```

### 2.2 정확하지 않은 키 < 키 없음
가격히스토리/트렌드는 **canonical_key NOT NULL** 조건으로 자동 필터. null 게시글은 검색/카테고리에는 노출되지만 가격비교/트렌드에서 자연 제외.

### 2.3 결정론적 변환은 코드에서, 자연어 이해만 LLM
LLM은 brand/model 분리, canonical_key 판단만 담당. 단위 정규화, price_per_unit 계산, 후보 매칭 검증은 모두 코드.

### 2.4 사양 등급 vs 패키지 용량 구분
| 종류 | 처리 |
|---|---|
| 사양 등급 (SSD 1TB/2TB, RAM 16GB/32GB, 폰 256GB/512GB) | canonical_key 분리, base_key 통합 |
| 패키지 용량 (펩시 355ml/210ml, 휴지 30롤/24롤) | canonical_key 통합, price_per_unit 비교 |

---

## 3. SKU 분리/통합 결정 매트릭스

| 케이스 | 처리 |
|---|---|
| 모델 세대 차이 (S25 vs S26) | canonical_key 분리 |
| 라인업 차이 (스위치 vs 라이트 vs OLED) | canonical_key 분리 |
| 한정판 (동물의숲 에디션) | canonical_key 분리, base_key 통합 |
| 이종 번들 (스위치 + 포코피아) | canonical_key 분리 (`+` 결합), base_key 통합 |
| 표준 색상 (블랙/화이트, 가격 동일) | 통합 |
| 사양 색상 (허먼밀러 미네랄/그래파이트, 가격 다름) | 분리 |
| 사양 등급 (SSD 1TB/2TB) | canonical_key 분리 |
| 패키지 용량 (펩시 355ml/210ml) | 통합 + price_per_unit |
| 모호 게시글 (모델명 없음) | canonical_key=null |
| 다중 SKU (256GB, 512GB 동시 명시) | 옵션 B: 최저가 SKU만 대표로 저장, model 텍스트에 모두 표기 |
| 게임 SW (스팀/PS5/스위치) | 플랫폼별 분리 (`game:title:platform`) |
| 상품권 (액면가 다름) | 액면가별 분리 (`giftcard:brand:amount`) |
| 디지털 구독 (월/년/평생) | 기간 통합, period로 별도 컬럼화 + price_per_month |
| 통신 요금제 | canonical_key=null (조건 변수 너무 큼) |
| 멀티 카테고리 묶음 | canonical_key=null |
| 시즌/스타일 묶음 (의류) | canonical_key=null |

### 3.1 LLM 가격 컨텍스트 활용
- ES kNN으로 기존 canonical_key 후보 + 각 후보의 가격 중간값을 프롬프트에 주입
- 모델명 모호할 때(예: "갤럭시 S25"만 적힘) 가격대로 후보 좁히기
- 모델명 명확하면 가격 무시 (핫딜은 모두 세일가)
- 후보에 없으면 신규 키 생성

### 3.2 핫딜 도메인 특성으로 빠진 필드
- **is_sale**: 핫딜 전체가 세일이라 항상 true → 정보량 0
- **post_type**: category 체계가 충분히 세밀해 불필요
- **attributes JSON**: 색상/저장용량은 productName 텍스트로 충분
- **product_condition** (NEW/REFURB): 빈도 낮음, 운영 후 필요시 추가
- **extra_skus 자식 테이블**: 다중 SKU는 옵션 B(대표만)로 단순화

---

## 4. DB 스키마

### 4.1 hotdeals 테이블 컬럼 추가 (additive only)

```sql
ALTER TABLE hotdeals
  ADD COLUMN brand VARCHAR(100),
  ADD COLUMN model VARCHAR(255),
  ADD COLUMN canonical_key VARCHAR(255),         -- 가격히스토리 키, nullable
  ADD COLUMN base_key VARCHAR(255),              -- 트렌드 집계 키, nullable
  ADD COLUMN bundle_items JSON,                  -- 이종 번들 구성품, ["포코피아"]
  
  ADD COLUMN unit_type ENUM('ml','g','ea'),      -- 비교/정규화용 (3가지)
  ADD COLUMN unit_label VARCHAR(10),             -- UI 표시용 ("ml", "g", "매", "롤", "캡슐", "일분" 등)
  ADD COLUMN unit_quantity DECIMAL(10,2),        -- 정규화된 한 묶음 측정량 (g/ml로 환산됨)
  ADD COLUMN pack_count INT,                     -- 묶음 수
  ADD COLUMN price_per_unit DECIMAL(12,4),       -- 자동 계산
  
  ADD COLUMN gift_card_face_value INT,           -- 상품권 액면가
  ADD COLUMN subscription_period INT,            -- 구독 개월수 (1=월, 12=연, null=평생/일회성)
  ADD COLUMN price_per_month DECIMAL(12,2),      -- 월당 환산가
  ADD COLUMN shopping_platform VARCHAR(50),      -- hotdeal_processes에서 승격
  
  ADD KEY idx_canonical_key (canonical_key),
  ADD KEY idx_base_key (base_key),
  ADD KEY idx_brand (brand);
```

기존 컬럼(product_name, price, currency_unit, title 등) 모두 유지.

### 4.2 hotdeal_processes 테이블

```sql
ALTER TABLE hotdeal_processes
  MODIFY COLUMN response MEDIUMTEXT,                 -- JSON 응답 길이 대비
  ADD COLUMN ai_response_version TINYINT DEFAULT 2;  -- v1=8줄 평문, v2=JSON
```

### 4.3 카테고리 변경 (CategoryInitializer.kt)

| 작업 | 코드 | 위치 |
|---|---|---|
| 제거 (또는 deprecated) | `point` | etc 하위 |
| 추가 | `promotion` (적립·이벤트·쿠폰·체험단·정리성) | etc 하위 |
| 추가 | `telecom_plan` (통신 요금제) | etc 하위 |
| 추가 | `travel` (여행·숙박) | hobby 하위 |

기존 88개 → 91개 (point 통합 후 +3 추가).

```kotlin
// CategoryInitializer.kt 변경
// 기존 point 제거 또는 사용 중단
// upsertCategory("point", "포인트", "Point", etc.id!!, 1, ...)  ← 제거

// etc 하위 추가
upsertCategory("promotion",    "적립·이벤트", "Promotion",    etc.id!!, 1, sortOrder++)
upsertCategory("telecom_plan", "통신 요금제",  "Telecom Plan", etc.id!!, 1, sortOrder++)

// hobby 하위 추가
upsertCategory("travel",       "여행·숙박",   "Travel",       hobby.id!!, 1, sortOrder++)
```

---

## 5. LLM 가공 파이프라인

### 5.1 입력 컨텍스트 (게시글 + 후보)

```
게시글 제목: "삼성 갤럭시 S25 울트라 256GB SM-S938N 자급제 (1,320,000원)"

ES kNN으로 검색한 유사 canonical_key 후보 (TOP 5~10):
  - samsung:galaxy-s25-ultra (47건, 중간값 1,580,000)
  - samsung:galaxy-s25-plus (12건, 중간값 1,190,000)
  - samsung:galaxy-s24-ultra (32건, 중간값 1,420,000)
  - samsung:galaxy-s25 (30건, 중간값 990,000)
```

### 5.2 LLM 출력 (JSON)

```json
{
  "brand": "삼성전자",
  "model": "갤럭시 S25 울트라 256GB",
  "model_specificity": "high",
  "canonical_key": "samsung:galaxy-s25-ultra-256gb",
  "base_key": "samsung:galaxy-s25-ultra",
  "bundle_items": [],
  "product_name": "삼성전자 갤럭시 S25 울트라 256GB",
  "product_name_en": "Samsung Galaxy S25 Ultra 256GB",
  "title_en": "Samsung Galaxy S25 Ultra 256GB SM-S938N Unlocked",
  "category": "smartphone",
  "confidence": 0.97,
  "platform": "네이버",
  "currency": "KRW",
  "price": 1320000,
  "unit_type": null,
  "unit_label": null,
  "unit_quantity": null,
  "pack_count": null,
  "gift_card_face_value": null,
  "subscription_period": null
}
```

### 5.3 프롬프트 핵심 규칙

```
1. canonical_key 부여 규칙 (가장 중요)
   기본 원칙: 정확하지 않으면 만들지 말 것 (null).
   
   부여 가능:
   - 모델명/모델번호 명확 (갤럭시 S25 울트라, WD SN850X, 라이젠 7800X3D)
   - 게임 SW의 타이틀+플랫폼 (데드 아일랜드2 + 스팀)
   - 상품권의 브랜드+액면가 (맥도날드 2만원권)
   - 디지털 구독의 서비스+플랜 (넷플릭스 프리미엄)
   
   부여 금지 (null):
   - 행사/세일 정리성 게시글
   - 멀티 카테고리 묶음 (콜라+과자+빵)
   - 통신 요금제
   - 의류 시즌/스타일 묶음
   - "외 N건", "N종 중 택1"
   - 모델명 없이 스펙만 (24인치 FHD 모니터)
   - 추상 위치/카테고리 (강원도 숙박)
   - "휴대폰성지", "본문참고"
   
   확신이 없으면 null. 확신이 있을 때만 만든다.

2. 사양 vs 패키지 구분
   - 사양 등급(GB/TB/RAM/인치/W)은 canonical_key에 포함, base_key는 미포함
     예: WD SN850X 2TB → wd:sn850x-2tb (base_key: wd:sn850x)
   - 패키지 용량(ml/g/장/캔수)은 canonical_key에 미포함, unit_*로 분리
     예: 펩시 제로 355ml 24캔 → pepsi:zero (unit_type=ml, qty=355, pack=24)

3. 색상/에디션 처리
   - 표준 색상(블랙/화이트/실버 등)은 통합
   - "한정/리미티드/스페셜/콜라보" 명시는 분리 (에디션으로 처리)
   - 마감재/등급 명칭(미네랄/그래파이트/카본 등)은 분리
   - 모호하면 통합 선택 (보수적)

4. 번들 처리
   - 이종 번들(본체 + 게임/액세서리)은 canonical_key 분리(`+` 결합)
   - base_key는 본체로 흡수
   - bundle_items에 구성품 배열 기록

5. 가격 컨텍스트 활용
   - 모델명이 명확하면 그 후보 매칭 (가격 무시 — 핫딜은 세일가)
   - 모델명이 모호하면 가격대 가까운 후보 선택
   - 후보에 없으면 신규 키 생성

6. 카테고리 분류
   - 가장 구체적인 코드 1개 선택 (88+3개 중)
   - 멀티카테고리 묶음은 가장 비중 높은 상품의 카테고리 선택
   - confidence < 0.5면 etc 카테고리

7. 단위 추출
   - "500ml × 24병" → unit_type=ml, unit_label="ml", unit_quantity=500, pack_count=24
   - "두루마리 휴지 30롤" → unit_type=ea, unit_label="롤", unit_quantity=1, pack_count=30
   - "오쏘몰 30일분" → unit_type=ea, unit_label="일분", unit_quantity=30, pack_count=1
   - 단위 없으면 모두 null

8. 다중 SKU 게시글 (옵션 B)
   - 가격이 둘 이상 명시되면 최저가 SKU를 대표로
   - 가격 단일 명시면 그것
   - 가격 모두 같으면 가장 작은 용량/사양
   - 대표 외 SKU는 model 텍스트에 표기 (예: "갤럭시 S25 256GB, 512GB")
```

### 5.4 후처리 (코드, 결정론적)

```kotlin
// 1. JSON 응답 방어 파싱
val cleaned = raw
    .replace(Regex("^```(?:json)?\\s*"), "")
    .replace(Regex("\\s*```$"), "")
    .replace(Regex(",\\s*([}\\]])"), "$1")  // trailing comma

// 2. 단위 정규화
fun normalizeUnit(rawType: String?, rawQty: BigDecimal?): UnitNormalized? {
    if (rawType == null || rawQty == null) return null
    return when (rawType.lowercase()) {
        "l", "리터"            -> UnitNormalized("ml", "L", rawQty * 1000.toBigDecimal())
        "ml", "밀리리터", "cc"  -> UnitNormalized("ml", "ml", rawQty)
        "kg", "킬로", "킬로그램" -> UnitNormalized("g", "kg", rawQty * 1000.toBigDecimal())
        "근"                   -> UnitNormalized("g", "근", rawQty * 600.toBigDecimal())
        "g", "그램"            -> UnitNormalized("g", "g", rawQty)
        "개", "입", "알"        -> UnitNormalized("ea", "개", rawQty)
        "매", "장"              -> UnitNormalized("ea", "매", rawQty)
        "롤"                   -> UnitNormalized("ea", "롤", rawQty)
        "병", "캔"              -> UnitNormalized("ea", rawType, rawQty)
        "팩", "봉", "포", "박스" -> UnitNormalized("ea", rawType, rawQty)
        "정", "캡슐"            -> UnitNormalized("ea", rawType, rawQty)
        "일분"                 -> UnitNormalized("ea", "일분", rawQty)
        else -> null
    }
}

// 3. price_per_unit 자동 계산
val pricePerUnit = if (price != null && unitQty != null && packCount != null) {
    price.divide(unitQty * packCount.toBigDecimal(), 4, RoundingMode.HALF_UP)
} else null

// 4. price_per_month (구독)
val pricePerMonth = if (price != null && period != null && period > 0) {
    price.divide(period.toBigDecimal(), 2, RoundingMode.HALF_UP)
} else null

// 5. 검증 게이트 (모델번호 토큰 일치 확인)
if (canonical_key != null) {
    if (!validateModelTokens(input, canonical_key)) {
        canonical_key = null  // 잘못된 매칭은 null로 강등
        base_key = null
    }
}
```

### 5.5 가격 캐시 (ES aggregation 부담 경감)

```
일일 배치:
  - 모든 canonical_key의 가격 통계 사전 계산
  - { key, median, iqr_low, iqr_high, sample_count, last_updated }
  - Redis 또는 별도 hotdeal_canonical_stats 테이블에 저장

가공 시점:
  - ES kNN 후보 5~10개 검색
  - 각 후보의 캐시된 통계 조회 (O(1))
  - LLM 프롬프트에 주입
```

---

## 6. Elasticsearch 매핑

### 6.1 신규 필드 추가 (HotdealDocument.kt)

```kotlin
@Field(type = FieldType.Keyword)
val brand: String? = null

@Field(type = FieldType.Text, analyzer = "nori_analyzer")
val model: String? = null

@Field(type = FieldType.Keyword)
val canonicalKey: String? = null      // 가격히스토리/트렌드 핵심

@Field(type = FieldType.Keyword)
val baseKey: String? = null            // 트렌드 집계용

@Field(type = FieldType.Double)
val pricePerUnit: BigDecimal? = null

@Field(type = FieldType.Keyword)
val unitType: String? = null

@Field(type = FieldType.Keyword)
val unitLabel: String? = null

@Field(type = FieldType.Double)
val unitQuantity: BigDecimal? = null

@Field(type = FieldType.Integer)
val packCount: Int? = null

@Field(type = FieldType.Integer)
val giftCardFaceValue: Int? = null

@Field(type = FieldType.Integer)
val subscriptionPeriod: Int? = null

@Field(type = FieldType.Double)
val pricePerMonth: BigDecimal? = null

// productName, productNameEn은 기존 그대로 유지 (호환성)
```

### 6.2 가격 히스토리 쿼리 변경

```kotlin
// 기존: match_phrase(productName, q).slop(1)
// 신규: term(canonicalKey, baseDocument.canonicalKey)

if (baseDocument.canonicalKey != null) {
    // term 쿼리, 정확
    must.term { it.field("canonicalKey").value(baseDocument.canonicalKey) }
} else {
    // canonicalKey null이면 가격히스토리 비활성 (UI에서 버튼 숨김)
}
```

### 6.3 트렌드 집계 (신규)

```json
{
  "filter": {
    "bool": {
      "must": [
        { "exists": { "field": "baseKey" } },
        { "range": { "createdAt": { "gte": "now-24h" } } }
      ]
    }
  },
  "aggs": {
    "trending": {
      "terms": {
        "field": "baseKey",
        "size": 20,
        "min_doc_count": 2
      },
      "aggs": {
        "sample": {
          "top_hits": {
            "size": 1,
            "_source": ["productName", "brand", "price", "thumbnailUrl"]
          }
        }
      }
    }
  }
}
```

### 6.4 검색 쿼리 변경

```kotlin
// 기존: productName^3, title^2
// 신규: productName^3, brand^2, model^2, title^1.5
m.fields(
    "productName^3", "brand^2", "model^2",
    "title^1.5", "productNameEn", "titleEn"
)
```

---

## 7. 가격히스토리 그래프 표시 분기

```
y축 결정 우선순위:

1. unit_type IS NOT NULL
   → y = price_per_unit
   → 라벨: "원/{unit_label}"   (예: "원/ml", "원/g", "원/매", "원/롤", "원/캡슐", "원/일분")
   → 툴팁: "{unit_quantity}{unit_label} × {pack_count}, 총 {price}"

2. subscription_period IS NOT NULL AND period > 0
   → y = price_per_month
   → 라벨: "원/월"
   → 툴팁: "{period}개월, 월 {price_per_month}원"

3. gift_card_face_value IS NOT NULL
   → y = discount_rate = (face_value - price) / face_value × 100
   → 라벨: "할인율 (%)"
   → 툴팁: "액면가 {face_value}, 할인율 {rate}%"

4. 위 모두 null
   → y = price (절대가)
   → 라벨: "원"
```

---

## 8. 카테고리 매트릭스 (전체 케이스)

| 입력 | category | canonical_key | base_key | 비고 |
|---|---|---|---|---|
| 갤럭시 S25 256GB | smartphone | `samsung:galaxy-s25-256gb` | `samsung:galaxy-s25` | 사양등급 |
| 갤럭시 S25 동물의숲 에디션 | smartphone | `samsung:galaxy-s25-animal-crossing-edition` | `samsung:galaxy-s25` | 한정판 |
| 갤럭시 S25 256GB, 512GB | smartphone | `samsung:galaxy-s25-256gb` | `samsung:galaxy-s25` | 다중SKU 옵션B |
| 펩시 제로 355ml 24캔 | juice_soda | `pepsi:zero` | `pepsi:zero` | unit=ml, qty=355, pack=24 |
| 펩시 제로 210ml 60캔 | juice_soda | `pepsi:zero` | `pepsi:zero` | unit=ml, qty=210, pack=60 |
| WD SN850X 2TB | ssd | `wd:sn850x-2tb` | `wd:sn850x` | 사양등급 |
| 닌텐도 스위치 + 포코피아 | console | `nintendo:switch+pokopia` | `nintendo:switch` | 이종번들 |
| 닌텐도 스위치 OLED 스플래툰3 | console | `nintendo:switch-oled-splatoon-3-edition` | `nintendo:switch-oled` | 한정판 |
| [스팀] 데드 아일랜드2 | game_software | `game:dead-island-2:steam` | `game:dead-island-2` | 게임 SW |
| 맥도날드 2만원권 | gift_card | `giftcard:mcdonalds:20000` | `giftcard:mcdonalds` | face=20000 |
| 메가커피 아메리카노 | gifticon | `gifticon:megacoffee:americano` | `gifticon:megacoffee` | 메뉴형 |
| 넷플릭스 프리미엄 1년 | subscription | `subscription:netflix:premium` | `subscription:netflix` | period=12 |
| 레노버 모니터 24인치 FHD | monitor | null | null | 모델명 모호 |
| WD SN850X 2TB 외 | ssd | null | null | "외" 다중 |
| 펩시 + 새우깡 + 다이제 | juice_soda 또는 etc | null | null | 멀티카테고리 |
| 강원도 숙박 1박 | travel | null | null | 추상 위치 |
| 네파키즈 아노락 반팔+반바지 세트 | men_clothing | null | null | 시즌 스타일 |
| 디스커버리 23FW 남성 패딩 | men_outer | null | null | 시즌 스타일 |
| LG 아이폰17 이동 6.1만 요금제 | telecom_plan | null | null | 통신 요금제 |
| 네이버페이 일일적립 | promotion | null | null | 적립 |
| GS25 9월 갓세일 정리 | promotion | null | null | 정리성 |
| 카스 제로 체험단 | promotion | null | null | 체험단 |
| 휴대폰성지 | telecom_plan | null | null | 정보성 |

---

## 9. 비용 추정 (175k건 재가공)

| 모델 | 추정 비용 | 비고 |
|---|---|---|
| `gpt-oss-120b:free` | $0 | 1순위, rate limit이 변수 |
| `claude-haiku-4-5` | ~$0.27 | 폴백 1순위 |
| `claude-sonnet-4-6` | ~$0.93 | 정확도 최우선 시 |

**전략**: 무료 모델로 전량 시도 → 실패분만 Claude Haiku 폴백.

JSON이 평문 8줄보다 응답 토큰 짧고, ES kNN 후보 추가로 인한 입력 토큰 증가는 미미. 기존 배치 구조(10건/요청, 5워커, 체크포인트) 유지.

---

## 10. 단계적 롤아웃

| 단계 | 작업 | 검증 게이트 |
|---|---|---|
| 0. 사전조사 | 골드라벨 200건 작성 (1차 LLM 라벨 + 사용자 검수) | 평가셋 확정 |
| 1. DDL | additive 컬럼 추가, NOT NULL 단계 보류 | 운영 무영향 |
| 2. v2 파이프라인 구현 | `HotdealProcessService` v2 (feature flag), JSON 파서, 후처리 | 골드셋 정확도 ≥ 85% |
| 3. ES 매핑 확장 | alias 기반 blue-green 재인덱싱 | 기존 검색 회귀 0 |
| 4. 신규 인입 v2 전환 | 크롤러 → v2 파이프라인. 기존 컬럼+신규 컬럼 듀얼 라이트 | 1주일 모니터링 |
| 5. 카나리 백필 | platform_type=CLIEN(7,180건 = 4%)만 재처리 | 가격히스토리 적중률 ≥ 기존 |
| 6. 점진 백필 | 1k → 10k → 100k → 175k 확장. 무료 모델 우선 | 비용·품질 모니터링 |
| 7. 소비측 전환 | `getPriceHistory`를 canonicalKey term 쿼리로, 트렌드 신규 매핑 사용 | A/B 비교 |
| 8. 컷오버 | match_phrase 폴백 제거 (전체 백필 완료 후 30일) | 모든 PRODUCT canonical_key NOT NULL |

**롤백**: 어느 단계든 feature flag off + ES alias 원복 + DB 컬럼 유지(삭제 안 함).

---

## 11. 운영 모니터링 지표

```sql
-- 1. canonical_key null 비율 (카테고리별)
SELECT c.code AS category, COUNT(*) AS total,
       SUM(h.canonical_key IS NULL) / COUNT(*) AS null_ratio
FROM hotdeals h
JOIN hotdeal_categories hc ON h.id = hc.hotdeal_id
JOIN categories c ON hc.category_id = c.id
GROUP BY c.code;
-- 정상 범위: 일반 상품 50~60% null, gift_card 10% 이하, promotion 100%

-- 2. 카테고리 분포 변화 (v1 vs v2)
SELECT c.code, COUNT(*) AS cnt
FROM hotdeals h
JOIN hotdeal_categories hc ON h.id = hc.hotdeal_id
JOIN categories c ON hc.category_id = c.id
WHERE h.created_at >= NOW() - INTERVAL 7 DAY
GROUP BY c.code;

-- 3. 신규 canonical_key 추세 (브랜드 누락 감지)
SELECT DATE(created_at) AS d, COUNT(DISTINCT canonical_key) AS new_keys
FROM hotdeals
WHERE canonical_key NOT IN (
  SELECT DISTINCT canonical_key FROM hotdeals
  WHERE created_at < NOW() - INTERVAL 30 DAY AND canonical_key IS NOT NULL
)
AND canonical_key IS NOT NULL
GROUP BY d;

-- 4. 같은 canonical_key의 가격 분산 (사양 차이 누락 감지)
SELECT canonical_key, COUNT(*) AS n,
       (MAX(price) - MIN(price)) / MIN(price) AS spread
FROM hotdeals
WHERE canonical_key IS NOT NULL AND price > 0
GROUP BY canonical_key
HAVING n >= 10 AND spread > 0.20
ORDER BY spread DESC;

-- 5. 가격히스토리 적중률 (canonical_key별 데이터 누적량)
SELECT canonical_key, COUNT(*) AS history_size
FROM hotdeals
WHERE canonical_key IS NOT NULL
GROUP BY canonical_key
HAVING history_size >= 3;
```

---

## 12. 골드라벨 200건 구성

| 케이스 | 건수 |
|---|---|
| 일반 상품 (사양 명확) | 60 |
| 사양등급 (SSD/폰 용량) | 10 |
| 패키지 용량 (음료/식품) | 20 |
| 한정판/에디션 | 10 |
| 이종 번들 | 10 |
| 다중 SKU 게시글 | 10 |
| 모호 게시글 (모델명 없음) | 15 |
| 멀티 카테고리 묶음 | 5 |
| 시즌/스타일 (의류) | 10 |
| 게임 SW | 10 |
| 상품권/기프티콘 | 10 |
| 통신 요금제 | 10 |
| 디지털 구독 | 5 |
| 비상품 (적립/이벤트/쿠폰) | 15 |
| **합계** | **200** |

작성 방식: 1차 claude-sonnet 라벨링 → 사용자 검수.

평가 메트릭:
- canonical_key 정확도 (정확 매칭)
- base_key 정확도
- category 정확도
- model_specificity 정확도
- canonical_key NULL 판정 정확도 (모호 게시글에서 null로 두는지)
- unit_type/unit_quantity/pack_count 추출 정확도
- price 추출 정확도

---

## 13. 의도적 배제 항목

| 항목 | 배제 사유 |
|---|---|
| post_type 컬럼 | category 체계가 충분히 세밀해 불필요 |
| attributes JSON | 색상/저장용량 등 별도 보관 가치 없음, productName 텍스트로 충분 |
| is_sale | 핫딜 전체가 세일이라 항상 true → 정보량 0 |
| product_condition (NEW/REFURB) | 빈도 낮음, 운영 후 필요 시 추가 |
| region/통화별 분리 컬럼 | 기존 currency_unit으로 충분 |
| extra_skus 자식 테이블 | 다중 SKU는 옵션 B(대표만)로 단순화 |
| brand_aliases 사전 | LLM이 직접 brand 추출, 운영 모니터링으로 점진 보강 |
| bundle_misc 카테고리 | LLM이 가장 비중 높은 카테고리 선택 또는 etc |

---

## 14. 다음 단계

1. **운영 DB(`hotdeal_list_migration` 174k건)에서 200건 샘플 추출**
2. **1차 LLM 라벨링** (claude-sonnet, JSON 응답)
3. **사용자 검수** → 골드라벨 확정
4. **v2 프롬프트 작성** (위 모든 규칙 반영)
5. **PoC 가공기 구현** (`HotdealProcessService` v2 신규, feature flag 분기)
6. **ES kNN 후보 검색 + 가격 캐시 구현**
7. **dev DB 8,789건 시범 가공** → 골드라벨 정확도 측정
8. 정확도 통과 시 → 단계 1~8 롤아웃

---

## 15. 참고 — 현재 시스템 의존성 (변경 영향 평가)

| 컴포넌트 | 위치 | 영향 |
|---|---|---|
| HotdealProcessService | `backend/.../service/HotdealProcessService.kt` | v2 신규 작성, feature flag |
| 카테고리 정의 | `backend/.../initializer/CategoryInitializer.kt` | 3개 추가, 1개 deprecated |
| ES 문서 매핑 | `backend/.../document/HotdealDocument.kt` | 신규 필드 추가 |
| ES 인덱스 매핑 JSON | `backend/src/main/resources/elasticsearch/` | 매핑 갱신 |
| 가격 히스토리 쿼리 | `HotdealSearchService.kt:271-290` | term(canonicalKey) 전환 |
| 검색 쿼리 | `HotdealSearchService.kt:396-448` | brand/model 필드 추가 |
| 자동완성 | `HotdealSearchService.kt:522-530` | 변경 없음 (productName 유지) |
| 마이그레이션 배치 | `migration/02_ai_process_batch.py` | v2 신규 스크립트 (06_reprocess_v2.py) |
| 프론트 카드 | `frontend/.../deal-card.tsx` | 변경 없음 (productName 유지) |
| 프론트 가격히스토리 | `frontend/.../price-history-dialog.tsx` | unit_label 표시 추가, 그래프 y축 분기 |
