### 데이터 수집 Flow

1. 커뮤니티 핫딜 크롤링
2. AI 가공
- 제목 -> 상품명 (한글/영어)
- 제목 -> 카테고리
- 제목, 본문 -> 가격
3. ES 추가
- 제목 (한글/영어), 상품명 (한글/영어) 저장하여 검색 (nori, ngram)

### 테이블
```sql
CREATE TABLE hotdeal_raws (
    id INT PRIMARY KEY AUTO_INCREMENT,
    platform_type VARCHAR(255) NOT NULL,
    platform_post_id VARCHAR(255) NOT NULL,
    url VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    source_url VARCHAR(255),
    content_html TEXT,
    price INT,
    currency_unit VARCHAR(3) DEFAULT 'KRW',
    like_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    is_ended TINYINT(1) DEFAULT 0,
    thumbnail_image_url VARCHAR(255),
    first_image_url VARCHAR(255),
    wrote_at DATETIME,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_platform_post (platform_type, platform_post_id),
    INDEX idx_platform_type (platform_type),
    INDEX idx_wrote_at (wrote_at DESC),
    INDEX idx_is_ended (is_ended),
    INDEX idx_created_at (created_at DESC)
);

CREATE TABLE hotdeals (
    id INT PRIMARY KEY AUTO_INCREMENT,
    raw_id INT NOT NULL,
    platform_type VARCHAR(255) NOT NULL,
    url VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    title_en VARCHAR(255),
    product_name VARCHAR(255),
    product_name_en VARCHAR(255),
    price INT,
    currency_unit VARCHAR(3) DEFAULT 'KRW',
    source_url VARCHAR(255),
    like_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    wrote_at DATETIME,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_raw_id (raw_id),
    INDEX idx_platform_type (platform_type),
    INDEX idx_wrote_at (wrote_at DESC),
    INDEX idx_created_at (created_at DESC),
    INDEX idx_price (price)
);

CREATE TABLE categories (
    id INT PRIMARY KEY AUTO_INCREMENT,
    parent_id INT,
    code VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    name_en VARCHAR(255),
    depth INT NOT NULL DEFAULT 0,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_parent_id (parent_id),
    INDEX idx_depth (depth),
    INDEX idx_sort_order (sort_order)
);

CREATE TABLE hotdeal_categories (
    id INT PRIMARY KEY AUTO_INCREMENT,
    hotdeal_id INT NOT NULL,
    category_id INT NOT NULL,
    confidence_score DECIMAL(3,2),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_hotdeal_category (hotdeal_id, category_id),
    INDEX idx_hotdeal_id (hotdeal_id),
    INDEX idx_category_id (category_id),
    INDEX idx_confidence (confidence_score DESC)
);
```

### 프롬프트

```
You are a product classifier.

Classify products into the hierarchy below ONLY.

RULES:
- Output exactly 3 lines: product (KR), product (EN), category.
- Category: Select ONE most specific category code from the list below. (e.g., prefer 'smartphone' over 'mobile' or 'electronics').
- Confidence: 0.00–1.00, reflect certainty only.
- Product name:
  * ALWAYS keep product brand + model + capacity/quantity
  * If multiple models exist, list all models
  * If capacity/quantity does not exist, omit it
  * Normalize quantity format (e.g., "x 60캔" → "60캔")
  * Replace separators (/, |, ·) with commas
  * REMOVE shopping platforms UNLESS the platform itself is the product
  * REMOVE prices, promotions, discount info
- Use platform/brand context if name is unclear; otherwise summarize key content.

FORMAT:
Product name (Original)
Product name (English)
category_code,confidence

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

"[네이버] 삼성전자 갤럭시 S25 울트라 256GB 512GB SM-S938N 자급제 (체감가 132만)"
갤럭시 S25 울트라 256GB, 512GB
Galaxy S25 Ultra 256GB, 512GB
smartphone,0.97

"[네이버페이] 일일적립"
네이버페이 일일적립
Naver Pay Daily Rewards
point,0.85

```