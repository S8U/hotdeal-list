# AI 상품명 가공 v2 — 골드라벨 (1차 라벨링)

작성일: 2026-05-03  
출처: `hotdeal_list_migration.hotdeals` 무작위 추출 (174,933건 중 200건)  
용도: v2 LLM 프롬프트 정확도 평가용 정답지

## 라벨 스키마

각 행은 다음 필드를 포함:
- `id`: hotdeal 행 ID
- `platform_type`: 커뮤니티 출처
- `title`: 게시글 원본 제목
- `brand`: 브랜드명 (한글 우선)
- `model`: 모델명/상품명 (브랜드 제외)
- `model_specificity`: high | medium | low
- `canonical_key`: 가격히스토리 키 (없으면 null)
- `base_key`: 트렌드 집계 키 (없으면 null)
- `bundle_items`: 이종 번들 구성품 배열
- `category`: 카테고리 코드
- `unit_type`: ml | g | ea | null
- `unit_label`: 표시명 (ml, g, 매, 롤, 캡슐, 일분, 캔, 정 등) | null
- `unit_quantity`: 정규화된 수량 | null
- `pack_count`: 묶음 수 | null
- `gift_card_face_value`: 상품권 액면가 | null
- `subscription_period`: 구독 개월수 | null
- `price`: 추출 가격 | null
- `currency`: KRW | USD | JPY | CNY | EUR
- `platform`: 쇼핑 플랫폼 | null
- `note`: 분류 사유/특이사항

## 검토 가이드

각 행을 보고 정답이 맞는지 검토해주세요. 특히:
1. canonical_key가 모호하면 null로 두는 게 맞는지 (대분류성, 모델명 부재 등)
2. base_key가 한정판/번들/사양등급에서 본체로 통합되는지
3. unit_type/unit_quantity/pack_count가 가격비교에 의미 있게 추출됐는지
4. 카테고리가 가장 구체적인 코드로 분류됐는지
5. 의류 시즌묶음, 통신 요금제, 정리성 게시글 등 대분류 케이스가 null로 처리됐는지

---

## A. 일반 상품 — 사양 명확 (60건)

### A-1. 스마트폰/태블릿/스마트워치

| id | title | brand | model | specificity | canonical_key | base_key | category | unit_type | unit_label | unit_qty | pack | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 47726 | [쿠팡] 갤럭시 S23 울트라 512G (1,505,420원) | 삼성전자 | 갤럭시 S23 울트라 512GB | high | samsung:galaxy-s23-ultra-512gb | samsung:galaxy-s23-ultra | smartphone | null | null | null | null | 1505420 | 쿠팡 | 사양등급 분리 |
| 20264 | [G마켓]삼성전자 갤럭시 A34 5G 8+128GB 그라파이트 자급제 | 삼성전자 | 갤럭시 A34 5G 8GB 128GB 그라파이트 | high | samsung:galaxy-a34-5g-128gb | samsung:galaxy-a34-5g | smartphone | null | null | null | null | 319460 | G마켓 | RAM+저장 분리, 색상은 표준이라 통합 |
| 18891 | [쿠팡] 아이폰13미니 자급제 128gb 미드나이트 | 애플 | 아이폰 13 미니 128GB 미드나이트 | high | apple:iphone-13-mini-128gb | apple:iphone-13-mini | smartphone | null | null | null | null | 729000 | 쿠팡 | 표준 색상 통합 |
| 53684 | [쿠팡] Apple 아이폰 13 mini 자급제 (722,000원 / 무료) | 애플 | 아이폰 13 미니 자급제 | medium | apple:iphone-13-mini | apple:iphone-13-mini | smartphone | null | null | null | null | 722000 | 쿠팡 | 저장용량 미명시 |
| 171009 | [기타] 갤럭시 S25 울트라 256G | 삼성전자 | 갤럭시 S25 울트라 256GB | high | samsung:galaxy-s25-ultra-256gb | samsung:galaxy-s25-ultra | smartphone | null | null | null | null | null | null | 가격 미명시 |
| 168894 | [알리] 삼성 갤럭시z 폴드 7 512gb 자급제 | 삼성전자 | 갤럭시 Z 폴드 7 512GB 자급제 | high | samsung:galaxy-z-fold-7-512gb | samsung:galaxy-z-fold-7 | smartphone | null | null | null | null | null | 알리 | 사양등급 분리 |
| 1648 | [11번가](종료)갤럭시탭S9울트라 Wi-Fi 512GB(130만원) | 삼성전자 | 갤럭시탭 S9 울트라 Wi-Fi 512GB | high | samsung:galaxy-tab-s9-ultra-wifi-512gb | samsung:galaxy-tab-s9-ultra | tablet | null | null | null | null | 1300000 | 11번가 | 통신타입+저장 분리 |
| 131831 | [11번가] 삼성전자 갤럭시탭 S10 FE WiFi 전용 128GB + 카카오 S펜케이스 | 삼성전자 | 갤럭시탭 S10 FE WiFi 128GB + 카카오 S펜케이스 | high | samsung:galaxy-tab-s10-fe-wifi-128gb+s-pen-case | samsung:galaxy-tab-s10-fe | tablet | null | null | null | null | null | 11번가 | 이종번들 |
| 54979 | [네이버] 갤럭시 워치6 44mm 블루투스 (247,000원) | 삼성전자 | 갤럭시 워치6 44mm 블루투스 | high | samsung:galaxy-watch-6-44mm-bluetooth | samsung:galaxy-watch-6 | smartwatch | null | null | null | null | 247000 | 네이버 | 사이즈+통신 분리 |
| 117826 | [기타] sk번호이동 갤럭시A35 / 갤럭시퀀텀5 | null | 갤럭시 A35, 갤럭시 퀀텀5 | low | null | null | telecom_plan | null | null | null | null | null | null | 통신 요금제 (대분류) |
| 60613 | [지마켓] 삼성 갤럭시북4 프로 16인치 NT960XGQ-A51A | 삼성전자 | 갤럭시북4 프로 16인치 NT960XGQ-A51A | high | samsung:galaxy-book-4-pro-16-nt960xgq-a51a | samsung:galaxy-book-4-pro | laptop | null | null | null | null | 1360000 | G마켓 | 모델번호로 정확 식별 |
| 62401 | [쿠팡] 샤오미 레드미 노트13 5G 글로벌 버전 스마트폰 | 샤오미 | 레드미 노트13 5G 글로벌 버전 | high | xiaomi:redmi-note-13-5g-global | xiaomi:redmi-note-13-5g | smartphone | null | null | null | null | 273820 | 쿠팡 | 지역판 분리 |

### A-2. PC 부품/SSD/메모리/그래픽카드

| id | title | brand | model | specificity | canonical_key | base_key | category | unit_type | unit_label | unit_qty | pack | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 99640 | [알리] 라이젠 7700 | AMD | 라이젠 7 7700 | high | amd:ryzen-7-7700 | amd:ryzen-7-7700 | cpu | null | null | null | null | null | 알리 | 모델번호 명확 |
| 21737 | [인터파크] GAINWARD 지포스 RTX 4060 고스트 D6 8GB | 게인워드 | 지포스 RTX 4060 고스트 D6 8GB | high | gainward:rtx-4060-ghost-8gb | gainward:rtx-4060-ghost | gpu | null | null | null | null | 369750 | 인터파크 | 사양 분리 |
| 120208 | [지마켓] GAINWARD 지포스 RTX 5080 피닉스 D7 16GB | 게인워드 | 지포스 RTX 5080 피닉스 D7 16GB | high | gainward:rtx-5080-phoenix-16gb | gainward:rtx-5080-phoenix | gpu | null | null | null | null | null | G마켓 | |
| 136838 | [지마켓] COLORFUL iGame 지포스 RTX 5080 ULTRA OC D7 16GB | 컬러풀 | iGame 지포스 RTX 5080 ULTRA OC D7 16GB | high | colorful:igame-rtx-5080-ultra-oc-16gb | colorful:igame-rtx-5080-ultra-oc | gpu | null | null | null | null | null | G마켓 | |
| 63174 | [티몬] ZOTAC GAMING 지포스 RTX 4070 Ti SUPER TRINITY BLACK | 조탁 | GAMING 지포스 RTX 4070 Ti SUPER TRINITY BLACK | high | zotac:rtx-4070-ti-super-trinity-black | zotac:rtx-4070-ti-super-trinity | gpu | null | null | null | null | null | 티몬 | |
| 169064 | [알리] JUHOR 메모리 램 DDR5 RGB 16GBX2 6000MHz | JUHOR | 메모리 램 DDR5 RGB 16GBX2 6000MHz | high | juhor:ddr5-rgb-16gbx2-6000mhz | juhor:ddr5-rgb | ram | null | null | null | null | null | 알리 | 사양등급 분리 |
| 161019 | [아마존] WD 2TB SN850 NVMe SSD for PS5 | WD | SN850 2TB NVMe SSD | high | wd:sn850-2tb | wd:sn850 | ssd | null | null | null | null | null | 아마존 | 사양등급 분리 |
| 122761 | [알리] ORICO Taichi 포터블 SSD 500GB 외장하드 | 오리코 | 타이치 포터블 SSD 500GB | high | orico:taichi-portable-ssd-500gb | orico:taichi-portable-ssd | external_storage | null | null | null | null | null | 알리 | 사양등급 분리 |
| 480 | [11번가]제플PC 라이젠7800X3D+RTX4080 (2,918,700원) | null | 라이젠 7800X3D + RTX 4080 PC | low | null | null | desktop | null | null | null | null | 2918700 | 11번가 | PC 견적 (대분류) |
| 86848 | 본체 I5-12400F, RTX4060 | null | i5-12400F + RTX 4060 본체 | low | null | null | desktop | null | null | null | null | null | null | PC 견적 (대분류) |
| 129720 | [쿠팡] HP 2025 프로데스크 285 타워 G1a 라이젠5 라이젠 8000 시리즈 | HP | 2025 프로데스크 285 타워 G1a 라이젠5 8000 시리즈 | high | hp:prodesk-285-tower-g1a | hp:prodesk-285-tower | desktop | null | null | null | null | 549000 | 쿠팡 | |

### A-3. 모니터/TV

| id | title | brand | model | specificity | canonical_key | base_key | category | unit_type | unit_label | unit_qty | pack | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 63958 | [티몬]LG 32GQ950 나노IPS 4K게이밍모니터 | LG | 32GQ950 나노IPS 4K 게이밍 모니터 | high | lg:32gq950 | lg:32gq950 | monitor | null | null | null | null | 1010000 | 티몬 | 모델번호 명확 |
| 93463 | [11번가] LG 32GP850 32인치 한정 타임딜 | LG | 32GP850 32인치 | high | lg:32gp850 | lg:32gp850 | monitor | null | null | null | null | 459000 | 11번가 | |
| 136718 | [지마켓] 오디세이 OLED G9 모니터 S49CG934 | 삼성전자 | 오디세이 OLED G9 S49CG934 | high | samsung:odyssey-oled-g9-s49cg934 | samsung:odyssey-oled-g9 | monitor | null | null | null | null | null | G마켓 | |
| 77341 | [쿠팡] 빅트랙 휴대용 모니터 16.1" IPS 144Hz FHD 161PM01 | 빅트랙 | 휴대용 모니터 16.1" IPS 144Hz FHD 161PM01 | high | bigtrak:161pm01 | bigtrak:portable-monitor | monitor | null | null | null | null | null | 쿠팡 | |
| 110542 | [알리] 제우스랩 P16K 휴대용 모니터 | ZEUSLAP | P16K 휴대용 모니터 | high | zeuslap:p16k | zeuslap:p16k | monitor | null | null | null | null | null | 알리 | |
| 161225 | [알리] ZEUSLAP 15.6인치, 16인치 포터블 모니터 | ZEUSLAP | 15.6인치, 16인치 포터블 모니터 | low | null | null | monitor | null | null | null | null | null | 알리 | 모델명 모호, 다중사이즈 |
| 87805 | [알리] 단독 스페셜-딜 32GQ950 외 특가리스트 | null | 32GQ950 외 모니터 특가 | low | null | null | monitor | null | null | null | null | null | 알리 | "외" 다중 |
| 15226 | [십일절] 알파스캔 34인치 울트라와이드 144Hz 게이밍 모니터 U34G3XM 외 5건 | 알파스캔 | 34인치 울트라와이드 144Hz 게이밍 모니터 U34G3XM 외 5건 | low | null | null | monitor | null | null | null | null | null | null | "외 5건" 다중 |
| 126992 | [옥션] 알파스캔, 240Hz & 0.3ms 27G11 24G11 게이밍 모니터 2종 | 알파스캔 | 27G11, 24G11 게이밍 모니터 | low | null | null | monitor | null | null | null | null | null | 옥션 | 2종 다중 모델 |
| 169271 | [알리] 광군제 LG 게이밍모니터 24G411, 커브드 32MR 외 | LG | 게이밍모니터 24G411, 커브드 32MR | low | null | null | monitor | null | null | null | null | null | 알리 | 다중 모델 |
| 104870 | [네이버] LG전자 새해특별 기획전 커브드모니터 32MR50C 외 1건 | LG | 커브드모니터 32MR50C | medium | lg:32mr50c | lg:32mr50c | monitor | null | null | null | null | null | 네이버 | "외 1건" 있지만 메인 모델 명확 |
| 126357 | [네이버] 오늘 저녁 7시! 삼성전자 75인치 4K UHD TV 라이브 특가 | 삼성전자 | 75인치 4K UHD TV | low | null | null | tv | null | null | null | null | null | 네이버 | 모델명 미명시 |
| 129671 | [네이버] 삼성전자 55인치 4K UHD TV 라이브 특가 | 삼성전자 | 55인치 4K UHD TV | low | null | null | tv | null | null | null | null | null | 네이버 | 모델명 미명시 |
| 113651 | [네이버] 브랜드 위크! 삼성 비즈니스 75인치 UHD TV LH75BED | 삼성전자 | 비즈니스 75인치 UHD TV LH75BED | high | samsung:lh75bed | samsung:lh75bed | tv | null | null | null | null | null | 네이버 | 모델번호 명확 |
| 172489 | 삼성전자 Neo QLED TV KQ75QNF70BFXKR 75인치 | 삼성전자 | Neo QLED TV KQ75QNF70BFXKR 75인치 | high | samsung:neo-qled-kq75qnf70bfxkr | samsung:neo-qled-kq75qnf70 | tv | null | null | null | null | 1970890 | null | |
| 115461 | [네이버] 스마트모니터 M7 쇼핑라이브 특가 사전 안내 | 삼성전자 | 스마트모니터 M7 | medium | samsung:smart-monitor-m7 | samsung:smart-monitor-m7 | monitor | null | null | null | null | null | 네이버 | |

### A-4. 음향/주변기기

| id | title | brand | model | specificity | canonical_key | base_key | category | unit_type | unit_label | unit_qty | pack | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 68593 | [알리] 갤럭시 버즈 라이브 (79,221원 / 무배) | 삼성전자 | 갤럭시 버즈 라이브 | high | samsung:galaxy-buds-live | samsung:galaxy-buds-live | earphone_headphone | null | null | null | null | 79221 | 알리 | |
| 63158 | [쿠팡] BOSE 홈 스피커 500 (299000/무배) | 보스 | 홈 스피커 500 | high | bose:home-speaker-500 | bose:home-speaker-500 | speaker | null | null | null | null | 299000 | 쿠팡 | |
| 165558 | [네이버]MCHOSE V9 PRO 블루투스 7.1채널 헤드셋 | MCHOSE | V9 PRO 블루투스 7.1채널 헤드셋 | high | mchose:v9-pro | mchose:v9-pro | earphone_headphone | null | null | null | null | 30980 | 네이버 | |
| 162700 | [알리] 수월우 Nice Buds (유선 오픈형 이어폰) | 수월우 | Nice Buds 유선 오픈형 이어폰 | high | moondrop:nice-buds | moondrop:nice-buds | earphone_headphone | null | null | null | null | null | 알리 | |
| 28937 | [큐텐] BUBRILL 귀걸이형 스포츠 무선 블루투스 이어폰 | 버브릴 | 귀걸이형 스포츠 무선 블루투스 이어폰 | medium | bubrill:sport-wireless-earphone | bubrill:wireless-earphone | earphone_headphone | null | null | null | null | 14040 | 큐텐 | 모델명 모호 |
| 115227 | [알리] 프리플로우 아콘 Archon RE:AL IX KUSTOM 풀 알루미늄 기계식 키보드 | 프리플로우 | 아콘 Archon RE:AL IX KUSTOM 기계식 키보드 | high | preeflow:archon-real-ix-kustom | preeflow:archon-real | keyboard | null | null | null | null | null | 알리 | |
| 166146 | [기타] 일렉트로마트 앱코 CQK108 저소음 풀배열 기계식키보드 | 앱코 | CQK108 저소음 풀배열 기계식 키보드 | high | abko:cqk108 | abko:cqk108 | keyboard | null | null | null | null | null | 일렉트로마트 | |
| 28058 | [G마켓] HP OMEN 16-wf1156TX + 오멘 게이밍마우스 | HP | OMEN 16-wf1156TX + 오멘 게이밍마우스 | high | hp:omen-16-wf1156tx+omen-mouse | hp:omen-16 | laptop | null | null | null | null | 1549790 | G마켓 | 이종 번들 |
| 161055 | [네이버] 아이폰 사생활 보호 필름 2매 | null | 아이폰 사생활 보호 필름 2매 | low | null | null | screen_protector | ea | 매 | 1 | 2 | 0 | 네이버 | 노브랜드 액세서리 |
| 24079 | [네이버] 갤럭시/아이폰 투명 카드수납 범퍼케이스 (아이폰15) | null | 갤럭시/아이폰 투명 카드수납 범퍼케이스 | low | null | null | case | null | null | null | null | 990 | 네이버 | 노브랜드 액세서리 |

### A-5. 가전

| id | title | brand | model | specificity | canonical_key | base_key | category | unit_type | unit_label | unit_qty | pack | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 28175 | [G마켓]삼성전자 비스포크 제트 VS20B956D5E 무선 청소기 220W 산토리니 베이지 | 삼성전자 | 비스포크 제트 VS20B956D5E 무선 청소기 220W | high | samsung:bespoke-jet-vs20b956d5e | samsung:bespoke-jet | vacuum | null | null | null | null | 477620 | G마켓 | 모델번호 명확 |
| 66354 | [11번가] 삼성전자 2도어 냉장고 RT25NARAHS8 | 삼성전자 | 2도어 냉장고 RT25NARAHS8 | high | samsung:rt25narahs8 | samsung:rt25narahs8 | refrigerator | null | null | null | null | 339000 | 11번가 | |
| 103069 | [쿠팡] 미디어 일반형 236L 2도어 냉장고 | 미디어 | 일반형 236L 2도어 냉장고 | medium | midea:236l-2door-fridge | midea:fridge | refrigerator | null | null | null | null | 314900 | 쿠팡 | 모델명 미명시, 사양으로만 식별 |
| 20340 | [쿠팡와우] 미디어 6인용 식기세척기 | 미디어 | 6인용 식기세척기 | low | null | null | kitchen_appliance | null | null | null | null | 201520 | 쿠팡 | 모델명 모호 |
| 53490 | [티몬] [10분어택] 퀸메이드 에어프라이어 모음전 | 퀸메이드 | 에어프라이어 모음전 | low | null | null | kitchen_appliance | null | null | null | null | 39900 | 티몬 | 다중 모델 모음 |
| 155897 | [11번가] LG 트롬 오브제 컬렉션 세탁건조기 세트 | LG | 트롬 오브제 컬렉션 세탁건조기 세트 | medium | lg:tromm-objet-washer-dryer-set | lg:tromm-objet | washing_machine | null | null | null | null | null | 11번가 | 시리즈만 명확, 정확 모델 없음 |
| 136932 | [지마켓] 나르왈 프레오 Z10 울트라 올인원 로봇청소기 | 나르왈 | 프레오 Z10 울트라 올인원 로봇청소기 | high | narwal:freo-z10-ultra | narwal:freo-z10 | vacuum | null | null | null | null | null | G마켓 | |
| 163492 | 샤오미 미지아 무선선풍기 써큘레이터 5세대 BPLDS05DM | 샤오미 | 미지아 무선선풍기 써큘레이터 5세대 BPLDS05DM | high | xiaomi:mijia-circulator-5gen-bplds05dm | xiaomi:mijia-circulator | etc | null | null | null | null | 97980 | null | |

### A-6. 자동차/공구/생활

| id | title | brand | model | specificity | canonical_key | base_key | category | unit_type | unit_label | unit_qty | pack | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 170827 | [오늘의집] 오비스 모션데스크 높이조절책상 | 오비스 | 모션데스크 높이조절책상 | medium | obis:motion-desk | obis:motion-desk | desk | null | null | null | null | 197000 | 오늘의집 | |
| 21780 | [쿠팡] 스탠리 아이스플로우 플립 스트로 텀블러 폴라화이트 887ml | 스탠리 | 아이스플로우 플립 스트로 텀블러 폴라화이트 887ml | high | stanley:iceflow-flip-straw-tumbler | stanley:iceflow-flip-straw-tumbler | kitchen_goods | ml | ml | 887 | 1 | 29900 | 쿠팡 | 표준 색상 통합 가능하나 887ml가 사양 |
| 55122 | [쿠팡] 스탠리 아이스플로우 플립 스트로 텀블러, 블랙, 887ml | 스탠리 | 아이스플로우 플립 스트로 텀블러 블랙 887ml | high | stanley:iceflow-flip-straw-tumbler | stanley:iceflow-flip-straw-tumbler | kitchen_goods | ml | ml | 887 | 1 | 30750 | 쿠팡 | 같은 키 (색상 통합) |
| 102454 | [쿠팡] CUKTECH 쿡테크 10 파워뱅크 PD 150W 보조배터리 10000mAh | 쿡테크 | 10 파워뱅크 PD 150W 보조배터리 10000mAh | high | cuktech:10-powerbank-150w-10000mah | cuktech:10-powerbank | charger_cable | null | null | null | null | null | 쿠팡 | |
| 66190 | [쿠팡] TESSAN 35W 범용 여행용 올인원 충전기 USB 2개, C타입 3개 | TESSAN | 35W 범용 여행용 올인원 충전기 USB 2개, C타입 3개 | high | tessan:35w-travel-charger | tessan:35w-travel-charger | charger_cable | null | null | null | null | null | 쿠팡 | |
| 163194 | [알리] Toocki 투키 GaN 유니버설 여행 어댑터 3C1A 45W | 투키 | GaN 유니버설 여행 어댑터 3C1A 45W | high | toocki:gan-universal-3c1a-45w | toocki:gan-universal | charger_cable | null | null | null | null | null | 알리 | |
| 161837 | [컴퓨존] 컴퓨존 x 카멜마운트 이동식 TV스탠드 단독 특가 | 카멜마운트 | 이동식 TV스탠드 | medium | camel-mount:portable-tv-stand | camel-mount:portable-tv-stand | interior_accessory | null | null | null | null | 0 | 컴퓨존 | |
| 102259 | [옥션] 이지라이프 이동식 사이드 선반 테이블 2컬러 | 이지라이프 | 이동식 사이드 선반 테이블 | medium | ezlife:portable-side-table | ezlife:portable-side-table | furniture | null | null | null | null | null | 옥션 | 표준 색상 통합 |

---

## B. 패키지 용량 (음료/식품/생필품) — 20건

| id | title | brand | model | specificity | canonical_key | base_key | category | unit_type | unit_label | unit_qty | pack | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 115385 | [쿠팡] 코카콜라 490ml, 24개 19120원(와우) | 코카콜라 | 코카콜라 490ml 24개 | high | coca-cola:cola | coca-cola:cola | juice_soda | ml | ml | 490 | 24 | 19120 | 쿠팡 | 패키지 통합 |
| 109570 | [알리] 펩시제로라임 355ml 업소용 24캔 | 펩시 | 제로 라임 355ml 24캔 | high | pepsi:zero-lime | pepsi:zero-lime | juice_soda | ml | ml | 355 | 24 | null | 알리 | 패키지 통합 |
| 158009 | [지마켓] 펩시제로 라임 210ml 30캔+355ml 24캔 | 펩시 | 제로 라임 210ml 30캔, 355ml 24캔 | high | pepsi:zero-lime | pepsi:zero-lime | juice_soda | ml | ml | 210 | 30 | 0 | G마켓 | 다중 패키지, 대표 1개 |
| 165669 | [쿠팡] 토레타 제로 500ml 24개 (13,990원) | 토레타 | 제로 500ml 24병 | high | toreta:zero | toreta:zero | juice_soda | ml | ml | 500 | 24 | 13990 | 쿠팡 | |
| 65405 | [G마켓] 백산수 무라벨 500ML X 40병 | 백산수 | 무라벨 500ml 40병 | high | baeksansoo:no-label | baeksansoo:no-label | juice_soda | ml | ml | 500 | 40 | 14960 | G마켓 | |
| 24964 | [11번가]스프라이트 제로 355ml x 24캔 | 스프라이트 | 제로 355ml 24캔 | high | sprite:zero | sprite:zero | juice_soda | ml | ml | 355 | 24 | 13800 | 11번가 | |
| 93556 | [쿠팡]비락식혜 제로 238ml 18개 | 비락 | 식혜 제로 238ml 18개 | high | birak:sikhye-zero | birak:sikhye-zero | juice_soda | ml | ml | 238 | 18 | 8160 | 쿠팡 | |
| 114269 | [11번가] 스파클 생수 2L 30병 | 스파클 | 생수 2L 30병 | high | sparkle:water | sparkle:water | juice_soda | ml | ml | 2000 | 30 | null | 11번가 | L→ml 환산 |
| 114643 | [쿠팡] 물하나 ECO 생수, 2L, 24개 (와우전용) | 물하나 | ECO 생수 2L 24개 | high | mulhana:eco-water | mulhana:eco-water | juice_soda | ml | ml | 2000 | 24 | null | 쿠팡 | |
| 46569 | [티몬] 지리산 물하나 생수 2LX12병 | 물하나 | 지리산 생수 2L 12병 | high | mulhana:jirisan-water | mulhana:jirisan-water | juice_soda | ml | ml | 2000 | 12 | 3990 | 티몬 | |
| 21151 | [오늘의집] 라인바싸 탄산수 500ml 40개 | 라인바싸 | 탄산수 500ml 40개 | high | line-barssa:sparkling-water | line-barssa:sparkling-water | juice_soda | ml | ml | 500 | 40 | 12932 | 오늘의집 | |
| 157957 | [11번가] 티코 말차 510ml, 3개 | 티코 | 말차 510ml 3개 | high | tico:matcha | tico:matcha | tea | ml | ml | 510 | 3 | 15960 | 11번가 | |
| 170405 | 대상웰라이프 영양한잔 저당식혜 120ml 24개 | 대상웰라이프 | 영양한잔 저당식혜 120ml 24개 | high | daesang-welllife:youngyang-sikhye | daesang-welllife:youngyang-sikhye | juice_soda | ml | ml | 120 | 24 | 9900 | null | |
| 41020 | [카카오]아로마뷰 옐로우가든 섬유유연제 5L x 2개 | 아로마뷰 | 옐로우가든 섬유유연제 5L 2개 | high | aromaview:yellow-garden-softener | aromaview:yellow-garden-softener | cleaning_goods | ml | ml | 5000 | 2 | 21510 | 카카오 | L→ml |
| 118303 | [옥션] 배상면주가 느린마을막걸리 750ml x 5입 | 배상면주가 | 느린마을막걸리 750ml 5입 | high | baesangmyun:neurinmaeul-makgeolli | baesangmyun:neurinmaeul-makgeolli | juice_soda | ml | ml | 750 | 5 | 12860 | 옥션 | |
| 153471 | [옥션] 국산 흑마늘진액 80ml 30포 x 2박스 | null | 국산 흑마늘진액 80ml 30포 2박스 | low | null | null | processed_food | ml | ml | 80 | 60 | 20900 | 옥션 | 노브랜드. 단위는 추출 (총 60포) |
| 90838 | [기타] [하프클럽] 햇반 210g 48입 | 햇반 | 210g 48입 | high | cj:hetbahn | cj:hetbahn | instant_food | g | g | 210 | 48 | 38970 | 하프클럽 | |
| 66715 | [위메프] 23년 햅쌀 신동진 상등급 10kg | 신동진 | 햅쌀 상등급 10kg | medium | shindongjin:hapssal | shindongjin:hapssal | etc | g | kg | 10000 | 1 | 23990 | 위메프 | kg→g, 표시는 kg |
| 144769 | 다우니 호텔향기 화이트티 1L 6개+200ml 3개 증정 | 다우니 | 호텔향기 화이트티 1L 6개 | high | downy:hotel-white-tea | downy:hotel-white-tea | cleaning_goods | ml | ml | 1000 | 6 | 29910 | null | 사은품 무시, 메인만 |
| 172960 | 헤드앤숄더 샴푸 850ml 3개+미니2개 증정 | 헤드앤숄더 | 샴푸 850ml 3개 | high | head-shoulders:shampoo | head-shoulders:shampoo | hair_body | ml | ml | 850 | 3 | 22438 | null | 사은품 무시 |

---

## C. 식품 (g/ea) — 15건

| id | title | brand | model | specificity | canonical_key | base_key | category | unit_type | unit_label | unit_qty | pack | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 67656 | [네이버] 천지인 꼬마버스타요 키노피오 홍삼스틱 15g 30포 | 천지인 | 꼬마버스타요 키노피오 홍삼스틱 15g 30포 | high | cheonjiin:tayo-kinopio-red-ginseng-stick | cheonjiin:tayo-red-ginseng-stick | etc | g | g | 15 | 30 | 22900 | 네이버 | |
| 21254 | [쿠팡골드박스] 바이오믹스 스테비아 에리스리톨 설탕 400g X2개 | 바이오믹스 | 스테비아 에리스리톨 설탕 400g 2개 | high | biomics:stevia-erythritol | biomics:stevia-erythritol | processed_food | g | g | 400 | 2 | 8440 | 쿠팡 | |
| 154254 | 참프레 우리쌀 순살 치킨 1kg | 참프레 | 우리쌀 순살 치킨 1kg | high | chamfre:rice-fed-chicken | chamfre:rice-fed-chicken | meat_seafood | g | kg | 1000 | 1 | 7900 | null | |
| 148022 | 초벌 소곱창 200g + 소스 30g | null | 초벌 소곱창 200g + 소스 30g | low | null | null | meat_seafood | g | g | 200 | 1 | 5290 | null | 노브랜드 + 사은품 |
| 105231 | [롯데온] 더미식 매움주의 장인라면 135g 4개 | 하림 | 더미식 매움주의 장인라면 135g 4개 | high | harim:themyeisic-jangin-ramen-spicy | harim:themyeisic-jangin-ramen | ramen | g | g | 135 | 4 | null | 롯데온 | |
| 33804 | [지마켓] 왕특대 사이즈 고등어 180g 220g 사이 x 10팩 | null | 왕특대 고등어 180~220g 10팩 | medium | null | null | meat_seafood | g | g | 200 | 10 | 16140 | G마켓 | 노브랜드, 사이즈 범위라 모호 |
| 70611 | [공홈] 한우1++ 밀푀유나베 밀키트 1,280g 2~3인분+우동사리 | null | 한우 1++ 밀푀유나베 밀키트 1280g + 우동사리 | low | null | null | meal_kit | g | g | 1280 | 1 | 9000 | null | 노브랜드 |
| 53228 | [위메프플러스]두레식품 무뼈 순살족발 300g | 두레식품 | 무뼈 순살족발 300g | high | duresikpum:boneless-jokbal | duresikpum:boneless-jokbal | meat_seafood | g | g | 300 | 1 | 5000 | 위메프 | |
| 30305 | [네이버스토어] 스위트 골드키위 대과 1kg | null | 스위트 골드키위 대과 1kg | medium | null | null | fruit | g | kg | 1000 | 1 | 14900 | 네이버 | 노브랜드 농산물 |
| 142694 | 국산 남해안 활 바지락 1kg 55미 내외X2팩+칼국수 증정 | null | 남해안 활 바지락 1kg 55미 2팩 | low | null | null | meat_seafood | g | kg | 1000 | 2 | 15600 | null | 노브랜드 |
| 56103 | [롯데온] 피코크 조선호텔 포기김치 4kg | 피코크 | 조선호텔 포기김치 4kg | high | peacock:chosun-hotel-kimchi | peacock:chosun-hotel-kimchi | etc | g | kg | 4000 | 1 | 25920 | 롯데온 | |
| 74970 | [쿠팡] 엠바레밀크캬라멜(720g), 720g, 1개 | 엠바레 | 밀크캬라멜 720g | high | embare:milk-caramel | embare:milk-caramel | chocolate_candy | g | g | 720 | 1 | null | 쿠팡 | |
| 152660 | 배홍동 비빔면 8입 + 배홍동 칼빔면 8입 | 배홍동 | 비빔면 8입, 칼빔면 8입 | high | baehongdong:bibim-myeon | baehongdong:bibim-myeon | ramen | ea | 입 | 8 | 1 | 13390 | null | 다중 SKU 대표만 |
| 160591 | 애슐리 볶음밥 920g 3팩 총12인분+핫도그증정 | 애슐리 | 볶음밥 920g 3팩 | high | ashley:fried-rice | ashley:fried-rice | frozen_food | g | g | 920 | 3 | 19890 | null | 사은품 무시 |
| 55200 | [단하루] 서울우유 프로틴에너지 커피 250ml 18팩 / 36팩 개당 800원 | 서울우유 | 프로틴에너지 커피 250ml 18팩 | high | seoul-milk:protein-energy-coffee | seoul-milk:protein-energy-coffee | coffee | ml | ml | 250 | 18 | 800 | null | 다중 패키지 대표 |

---

## D. 생활용품/위생/영양제 (개수 단위) — 10건

| id | title | brand | model | specificity | canonical_key | base_key | category | unit_type | unit_label | unit_qty | pack | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 23675 | [위메프] 무형광 호텔수건 30수 170g 타올 10장 | null | 무형광 호텔수건 30수 170g 10장 | low | null | null | bathroom_goods | ea | 장 | 1 | 10 | 22410 | 위메프 | 노브랜드 |
| 36455 | [티몬] 럭스리브 호텔수건 200g 40수 코마사 10장 | 럭스리브 | 호텔수건 200g 40수 10장 | medium | luxlive:hotel-towel-200g | luxlive:hotel-towel | bathroom_goods | ea | 장 | 1 | 10 | 23550 | 티몬 | |
| 28033 | [하이버] 신동엽 울트라씬 무꼭지형 콘돔 컴팩트에디션 40개입 + 무꼭지 16P+극초박 8P 증정 | 신동엽 | 울트라씬 무꼭지형 콘돔 컴팩트에디션 40개입 | high | sindongyeop:ultrathin-no-cap-compact-edition | sindongyeop:ultrathin-no-cap | etc | ea | 개 | 1 | 40 | 20070 | 하이버 | 한정판 분리 |
| 59427 | [Hmall] GC녹십자 맥스바이오틱스 30포 9박스 | GC녹십자 | 맥스바이오틱스 30포 9박스 | high | gcgreencross:maxbiotics | gcgreencross:maxbiotics | etc | ea | 포 | 30 | 9 | 86870 | Hmall | |
| 144768 | 이뮨비타민 멀티비타민 미네랄 올인원 7일분 | 이뮨비타민 | 멀티비타민 미네랄 올인원 7일분 | high | imune-vitamin:multi-mineral-allinone | imune-vitamin:multi-mineral-allinone | etc | ea | 일분 | 7 | 1 | 11610 | null | |
| 82257 | [종근당 건강몰] 락토핏 5일분 950원 | 종근당건강 | 락토핏 5일분 | high | jongkundang:lactofit | jongkundang:lactofit | etc | ea | 일분 | 5 | 1 | 950 | 종근당건강몰 | 일분 단위 |
| 146991 | 혈당케어엔 바나바60정 | 혈당케어엔 | 바나바 60정 | high | hyeoldang-care:banaba | hyeoldang-care:banaba | etc | ea | 정 | 60 | 1 | 6900 | null | 정 단위 |
| 43167 | [쿠팡] 클락스 왈라비 (32,500원) | 클락스 | 왈라비 | medium | clarks:wallabee | clarks:wallabee | shoes | null | null | null | null | 32500 | 쿠팡 | 사이즈 통합 |

---

## E. 한정판/에디션 — 5건

| id | title | brand | model | specificity | canonical_key | base_key | category | unit_type | unit_label | unit_qty | pack | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 17496 | [티몬] 애경 선물세트 리미티드아트에디션 6개 | 애경 | 선물세트 리미티드아트에디션 6개 | medium | aekyung:gift-set-limited-art-edition | aekyung:gift-set | etc | null | null | null | null | 38430 | 티몬 | 한정판 분리 |
| 28033 | 신동엽 울트라씬 무꼭지형 콘돔 컴팩트에디션 40개입 | 신동엽 | 울트라씬 무꼭지형 콘돔 컴팩트에디션 40개입 | high | sindongyeop:ultrathin-no-cap-compact-edition | sindongyeop:ultrathin-no-cap | etc | ea | 개 | 1 | 40 | 20070 | 하이버 | 컴팩트 에디션 분리 |
| 101951 | [XBOX] 스타워즈 제다이: 서바이버 디럭스에디션 90%할인 | null | 스타워즈 제다이 서바이버 디럭스에디션 | high | game:star-wars-jedi-survivor:xbox:deluxe | game:star-wars-jedi-survivor | game_software | null | null | null | null | 1160 | XBOX | 게임 디럭스 에디션 분리 |
| 109542 | [다이렉트게임즈] 휴먼카인드 디지털 디럭스 에디션 | null | 휴먼카인드 디지털 디럭스 에디션 | medium | game:humankind:digital:deluxe | game:humankind | game_software | null | null | null | null | 7900 | null | 게임 에디션 |
| 94562 | PS5 디지털 에디션 30주년 기념 한정판 번들 | 소니 | 플레이스테이션5 디지털 에디션 30주년 한정판 번들 | high | sony:ps5-digital-30th-anniversary-edition | sony:ps5 | console | null | null | null | null | 618000 | null | PS5 한정판, base는 PS5 |

---

## F. 게임 SW (플랫폼 분리) — 10건

| id | title | brand | model | specificity | canonical_key | base_key | category | unit_type | unit_label | unit_qty | pack | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 152347 | [스팀] Chamber Survival (무료) | null | Chamber Survival | high | game:chamber-survival:steam | game:chamber-survival | game_software | null | null | null | null | 0 | 스팀 | 무료 (가격히스토리 제외) |
| 46393 | [스팀] FC24 (23,100/무료) | EA Sports | FC24 | high | game:ea-sports-fc-24:steam | game:ea-sports-fc-24 | game_software | null | null | null | null | 23100 | 스팀 | |
| 105574 | [기타] [스팀] Red Dead Redemption 2 할인 | null | Red Dead Redemption 2 | high | game:red-dead-redemption-2:steam | game:red-dead-redemption-2 | game_software | null | null | null | null | null | 스팀 | |
| 60748 | [스팀] 다잉라이트 2 스테이 휴먼 리로디드 에디션 / 33,000원 | null | 다잉라이트 2 스테이 휴먼 리로디드 에디션 | high | game:dying-light-2-reloaded-edition:steam | game:dying-light-2 | game_software | null | null | null | null | 33000 | 스팀 | 에디션 분리 |
| 55625 | [카카오쇼핑] PS5 파이널 판타지7 리버스 디럭스 예약판 | null | PS5 파이널 판타지7 리버스 디럭스 예약판 | high | game:final-fantasy-7-rebirth:ps5:deluxe | game:final-fantasy-7-rebirth | game_software | null | null | null | null | null | 카카오쇼핑 | |
| 147952 | [겜우리] PS5 붉은사막 디럭스 에디션 | null | PS5 붉은사막 디럭스 에디션 | high | game:crimson-desert:ps5:deluxe | game:crimson-desert | game_software | null | null | null | null | 99800 | null | |
| 2363 | [네이버] PS5 렘넌트2 44800원 | null | PS5 렘넌트2 | high | game:remnant-2:ps5 | game:remnant-2 | game_software | null | null | null | null | 43800 | 네이버 | |
| 106717 | (무신사)ps5 디스크드라이브 158,000원 | 소니 | PS5 디스크드라이브 | high | sony:ps5-disc-drive | sony:ps5-disc-drive | console | null | null | null | null | 158000 | 무신사 | 액세서리, console로 |
| 809 | [스위치] 주요 할인 모음(젤다, 대항해시대, 슈로대, 삼국지, 제노블) | null | 닌텐도 스위치 게임 모음 | low | null | null | game_software | null | null | null | null | null | null | 다중 게임 모음 (대분류) |
| 97198 | [한국닌텐도] 네오위즈 퍼블리셔 세일 | null | 닌텐도 네오위즈 퍼블리셔 세일 | low | null | null | game_software | null | null | null | null | null | 한국닌텐도 | 퍼블리셔 세일 (대분류) |

---

## G. 모바일 앱 일시무료 (canonical_key=null) — 4건

| id | title | brand | model | specificity | canonical_key | base_key | category | unit_type | unit_label | unit_qty | pack | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 117712 | [iOS] Video star pro - Movie Maker 인앱 구매 평생 무료 | null | Video Star Pro Movie Maker | low | null | null | etc | null | null | null | null | null | null | iOS 앱, 1회성 |
| 16896 | [iOS] Long Exposure Toolkit 무료 | null | Long Exposure Toolkit | low | null | null | etc | null | null | null | null | null | null | iOS 앱 일시무료 |

---

## H. 상품권/기프티콘 (액면가 분리) — 10건

| id | title | brand | model | specificity | canonical_key | base_key | category | unit_type | gift_card_face_value | unit_qty | pack | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 38979 | [카카오톡] 요기요 상품권 3만원권 | 요기요 | 상품권 3만원권 | high | giftcard:yogiyo:30000 | giftcard:yogiyo | gift_card | null | 30000 | null | null | 27600 | 카카오톡 | |
| 38489 | [SSG] 온라인문화상품권 7%할인 (46,500원) | null | 온라인문화상품권 5만원권 | medium | giftcard:culture-online:50000 | giftcard:culture-online | gift_card | null | 50000 | null | null | 46500 | SSG | 7% 할인이라 액면가 추정 5만 |
| 74810 | [티몬] GS25 1만원권 (9,300원) | GS25 | 1만원권 | high | giftcard:gs25:10000 | giftcard:gs25 | gift_card | null | 10000 | null | null | 9300 | 티몬 | |
| 77128 | [11번가] 배달의민족 배민마트/스토어 3만원권 | 배달의민족 | 배민마트/스토어 3만원권 | high | giftcard:baemin-mart:30000 | giftcard:baemin-mart | gift_card | null | 30000 | null | null | 27900 | 11번가 | |
| 144769 | 제일제면소 모바일금액권 5만원권 | 제일제면소 | 모바일금액권 5만원권 | high | giftcard:jeil-myeonso:50000 | giftcard:jeil-myeonso | gift_card | null | 50000 | null | null | 39900 | null | |
| 173617 | [네이버] 파스쿠찌 모바일상품권 3만원권 | 파스쿠찌 | 모바일상품권 3만원권 | high | giftcard:pascucci:30000 | giftcard:pascucci | gift_card | null | 30000 | null | null | 22950 | 네이버 | |
| 164642 | [네이버] 뚜레쥬르 제품교환권 1만원, 2만원권 | 뚜레쥬르 | 제품교환권 1만원, 2만원권 | high | giftcard:tousles-jours:10000 | giftcard:tousles-jours | gift_card | null | 10000 | null | null | 10000 | 네이버 | 다중 액면가, 대표 1만원권 |
| 15 | [11번가] 구글플레이 기프트코드 1~10만원권 8% 할인 | 구글플레이 | 기프트코드 1만~10만원권 | low | null | null | gift_card | null | null | null | null | null | 11번가 | 액면가 범위 (대분류) |
| 21295 | [티몬] 이디야 아메리카노 2종 20% 할인 | 이디야 | 아메리카노 2종 | medium | gifticon:ediya:americano | gifticon:ediya | gifticon | null | null | null | null | 2560 | 티몬 | 메뉴형 기프티콘 |
| 48925 | [지마켓]페브리즈 섬유탈취제 370ml 3개+cu상품권 2천원 | 페브리즈 | 섬유탈취제 370ml 3개 | high | febreze:fabric-deodorizer | febreze:fabric-deodorizer | cleaning_goods | ml | 370 | 3 | 12940 | G마켓 | 메인은 페브리즈, CU상품권은 사은품 |

---

## I. 디지털 구독 (기간 분리) — 6건

| id | title | brand | model | specificity | canonical_key | base_key | category | subscription_period | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 137519 | [카톡선물하기] 디즈니플러스 3개월 이용권 50% 할인 | 디즈니플러스 | 3개월 이용권 | high | subscription:disney-plus | subscription:disney-plus | subscription | 3 | null | 카톡선물하기 | |
| 28707 | [지마켓] 클래스101 12개월 구독이용권 | 클래스101 | 12개월 구독이용권 | high | subscription:class-101 | subscription:class-101 | subscription | 12 | 149550 | G마켓 | |
| 16754 | EBS Play 연간 구독 할인 89,000 → 59,000 | EBS Play | 연간 구독 | high | subscription:ebs-play | subscription:ebs-play | subscription | 12 | 59000 | null | |
| 65323 | [티몬] 밀리의서재 1년 구독 | 밀리의서재 | 1년 구독 | high | subscription:millie-library | subscription:millie-library | subscription | 12 | 68778 | 티몬 | |
| 157308 | [카카오톡딜] ChatGPT Pro, 카카오 | 카카오 | ChatGPT Pro | medium | subscription:chatgpt-pro | subscription:chatgpt-pro | subscription | null | 0 | 카카오톡딜 | 기간 미명시 |
| 157375 | [기타] ChatGPT Pro 1개월 이용권 | OpenAI | ChatGPT Pro 1개월 이용권 | high | subscription:chatgpt-pro | subscription:chatgpt-pro | subscription | 1 | 0 | null | |
| 61869 | [왓챠] 아네트 (100원) | 왓챠 | 아네트 | high | game:annette:watcha | game:annette | etc | null | 100 | 왓챠 | 영화 콘텐츠 1편 |

---

## J. 통신 요금제 (canonical_key=null) — 5건

| id | title | brand | model | specificity | canonical_key | base_key | category | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|
| 117826 | [기타] sk번호이동 갤럭시A35 / 갤럭시퀀텀5 | SKT | 번호이동 갤럭시A35, 갤럭시퀀텀5 | low | null | null | telecom_plan | null | null | 통신 요금제 (대분류) |
| 153294 | 너겟 47요금제 라이브 예고 (월 47,000원) | 너겟 | 47요금제 | medium | null | null | telecom_plan | 0 | null | 알뜰폰 요금제 |
| 160208 | [기타] 휴대폰성지 / 수도권방문 / 성지의기준 폰슐랭 | null | 휴대폰성지 | low | null | null | telecom_plan | 0 | 폰슐랭 | 정보성 |
| 158063 | [기타] 휴대폰성지 / 수도권방문 / 성지의기준 폰슐랭 | null | 휴대폰성지 | low | null | null | telecom_plan | 0 | 폰슐랭 | 정보성 (중복) |
| 89467 | [애플TV스토어] SKT 휴대폰 결제 영화 40% 청구할인 이벤트 | SKT | 애플TV 스토어 영화 40% 청구할인 이벤트 | low | null | null | promotion | null | 애플TV스토어 | 결제수단 이벤트 |
| 138857 | [기타] (SKT전용) 삼성 갤럭시 버즈3 프로 +투명 케이스 | 삼성전자 | 갤럭시 버즈3 프로 + 투명 케이스 | high | samsung:galaxy-buds-3-pro+case | samsung:galaxy-buds-3-pro | earphone_headphone | null | null | SKT 통신사 한정이지만 상품 자체는 명확 |

---

## K. 비상품 — 적립/이벤트/쿠폰/체험단/정리성 (canonical_key=null) — 15건

| id | title | brand | model | specificity | canonical_key | base_key | category | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|
| 16817 | 네이버 10원 + 1원 | 네이버 | 포인트 | low | null | null | promotion | null | null | 적립 |
| 22236 | 11번가 우주패스 가입시 포인트증정 | 11번가 | 우주패스 포인트증정 | low | null | null | promotion | null | null | 가입 이벤트 |
| 48560 | [네이버페이] 적립 15원/12원 종합 차트 | 네이버페이 | 적립 종합 차트 | low | null | null | promotion | null | 네이버페이 | |
| 121402 | [네이버] 네이버페이 적립 10원 에디션 | 네이버페이 | 적립 10원 에디션 | low | null | null | promotion | null | 네이버 | |
| 120764 | [기타] [네이버페이] 적립 2원/YES24 원데이 특가 DVD/BLU-Ray/음반 | 네이버페이 | 적립 2원, YES24 원데이 특가 | low | null | null | promotion | null | null | |
| 86000 | [네이버페이] 클릭 10원 | 네이버페이 | 클릭 10원 | low | null | null | promotion | 10 | 네이버페이 | |
| 164146 | [네이버페이] 라방 10원 | 네이버페이 | 라방 10원 | low | null | null | promotion | 10 | 네이버페이 | |
| 1416 | [네이버페이] 클릭적립 2원 즉시적립 | 네이버페이 | 클릭적립 2원 | low | null | null | promotion | null | 네이버페이 | |
| 157983 | [네이버페이] 일일적립, 클릭적립 25원, 라이브예고 8원 | 네이버페이 | 일일적립, 클릭적립, 라이브예고 | low | null | null | promotion | 0 | 네이버페이 | |
| 129662 | [네이버] 네이버페이 적립 28원/보험 매일 100% 당첨보장/12원 | 네이버페이 | 적립 종합 차트 | low | null | null | promotion | null | 네이버 | |
| 111568 | [알리] 쿠폰 추가 | 알리 | 쿠폰 추가 | low | null | null | promotion | null | 알리 | |
| 112828 | [알리] 3월 초이스데이 봄날 세일 코드 정리 | 알리 | 3월 초이스데이 세일 코드 정리 | low | null | null | promotion | null | 알리 | 정리성 |
| 11940 | [DROP] Hallowheel 2023 할로윈 룰렛 쿠폰 코드 | DROP | Hallowheel 2023 할로윈 룰렛 쿠폰 | low | null | null | promotion | null | DROP | |
| 948 | [스토브인디] 스토브 한글 게임 30% 할인 쿠폰 | 스토브 | 한글 게임 30% 할인 쿠폰 | low | null | null | promotion | null | 스토브인디 | |
| 140058 | [네이버] 삼성특별쿠폰 4K UHD 모니터 + 비즈니스 TV | 삼성전자 | 4K UHD 모니터, 비즈니스 TV (쿠폰 행사) | low | null | null | promotion | null | 네이버 | 쿠폰 행사 정리 |
| 140010 | [KT멤버십] 10월 달달 초이스 (10/15~31) | KT | 멤버십 10월 달달 초이스 | low | null | null | promotion | null | KT멤버십 | 멤버십 이벤트 |

---

## L. 의류 시즌/스타일 묶음 (canonical_key=null) — 8건

| id | title | brand | model | specificity | canonical_key | base_key | category | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|
| 151053 | 에디션 사방스판 테이퍼드 슬랙스 | 에디션 | 사방스판 테이퍼드 슬랙스 | medium | null | null | men_bottom | 21120 | null | 시즌 스타일 |
| 129560 | [지마켓] 8420원~ 모노시크/티셔츠/나시/블라우스/남방/니트 | 모노시크 | 티셔츠, 나시, 블라우스, 남방, 니트 | low | null | null | men_top | 8420 | G마켓 | 다중 카테고리 |
| 141845 | [롯데온] 지제프 세미 오버핏 남녀공용 후드티 | 지제프 | 세미 오버핏 남녀공용 후드티 | medium | null | null | men_top | 55200 | 롯데온 | 시즌 스타일 |
| 164821 | 라이트 테리 반바지 | null | 라이트 테리 반바지 | low | null | null | men_bottom | 14800 | null | 노브랜드 |
| 74091 | [티몬] 봄신상 블라우스/셔츠/맨투맨 등 할인 | null | 봄신상 블라우스, 셔츠, 맨투맨 | low | null | null | women_top | 3900 | 티몬 | 다중 카테고리 정리성 |
| 29235 | [옥션] 언더아머 덕다운 패딩 남성 UA Storm 아머 다운푸퍼 | 언더아머 | 덕다운 패딩 남성 UA 스톰 아머 다운푸퍼 | medium | null | null | men_outer | 121200 | 옥션 | 시즌 |
| 47974 | [지마켓] 나이키키즈 조던 나이키 후드 윈드자켓 | 나이키 | 조던 키즈 후드 윈드자켓 | medium | null | null | men_outer | 38240 | G마켓 | |
| 53140 | [롯데온] 타미진스 맨투맨 | 타미진스 | 맨투맨 | low | null | null | men_top | 30760 | 롯데온 | 모델명 모호 |
| 73339 | [하프클럽] 행텐 맨투맨/데님 외 50종 | 행텐 | 맨투맨, 데님 외 50종 | low | null | null | men_top | 15020 | 하프클럽 | 외 50종 |
| 147669 | 내셔널지오그래픽 키즈 공용 맨투맨 세트 | 내셔널지오그래픽 | 키즈 공용 맨투맨 세트 | low | null | null | men_top | 49710 | null | 시즌 세트 |
| 42337 | [옥션] 에디션 하이넥 퀄팅 덕다운 자켓 | 에디션 | 하이넥 퀄팅 덕다운 자켓 | low | null | null | men_outer | 56250 | 옥션 | 시즌 |

---

## M. 다중 SKU 게시글 (대표만 저장) — 8건

| id | title | brand | model | specificity | canonical_key | base_key | category | unit_type | unit_label | unit_qty | pack | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 158009 | [지마켓] 펩시제로 라임 210ml 30캔+355ml 24캔 | 펩시 | 제로 라임 210ml 30캔, 355ml 24캔 | high | pepsi:zero-lime | pepsi:zero-lime | juice_soda | ml | ml | 210 | 30 | 0 | G마켓 | 같은 SKU 다른 패키지 |
| 111529 | [알리] 미숫가루 500g x4 / 2kg | null | 미숫가루 500g 4개, 2kg | low | null | null | etc | g | g | 500 | 4 | 5582 | 알리 | 노브랜드, 다중 패키지 |
| 109115 | [네이버] 립톤 제로 아이스티 복숭아 355ml 24캔 + 2025 데일리 다이어리 | 립톤 | 제로 아이스티 복숭아 355ml 24캔 + 다이어리 | high | lipton:zero-iced-tea-peach+diary | lipton:zero-iced-tea-peach | tea | ml | ml | 355 | 24 | null | 네이버 | 이종 번들 |
| 169535 | [네이버] 컴퓨터선정리책상 제로라인데스크 47% 할인 특가 | null | 제로라인데스크 | medium | null | null | desk | null | null | null | null | 72470 | 네이버 | 노브랜드 |
| 116207 | [알리] 갤럭시 지포스 RTX5070 Ti 16G (블랙/화이트) | 갤럭시 | 지포스 RTX5070 Ti 16G | high | galaxy:rtx-5070-ti-16gb | galaxy:rtx-5070-ti | gpu | null | null | null | null | null | 알리 | 색상은 표준 통합 |
| 64705 | [11번가] 보노스프 12각 머그컵 골라담기 총36봉입 | 보노스프 | 12각 머그컵 36봉입 | low | null | null | etc | null | null | null | null | null | 11번가 | 골라담기 다중 |
| 124899 | [알리] 레노버 샤오신패드 프로 12.7인치 2025 8+128GB 외 5종 | 레노버 | 샤오신패드 프로 12.7인치 2025 8+128GB 외 5종 | low | null | null | tablet | null | null | null | null | null | 알리 | 외 5종 |
| 112108 | [알리] 마이크론 P3 PLUS 1TB 벌크 외 1종 | 마이크론 | P3 PLUS 1TB 벌크 | low | null | null | ssd | null | null | null | null | null | 알리 | 외 1종 |

---

## N. 멀티 카테고리 묶음/노브랜드 묶음 (canonical_key=null) — 6건

| id | title | brand | model | specificity | canonical_key | base_key | category | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|
| 48260 | [롯데온] 오뚜기 컵밥 5종 12개 골라담기 | 오뚜기 | 컵밥 5종 12개 골라담기 | low | null | null | instant_food | 19760 | 롯데온 | 5종 골라담기 |
| 82386 | [티몬] 오뚜기 냉동피자 3판 11,600원(불고기, 콤비, 마르중 택3) | 오뚜기 | 냉동피자 3판 (불고기, 콤비, 마르게리타) | low | null | null | frozen_food | 11600 | 티몬 | 택3 |
| 172163 | [네이버] 프링글스110g 외 10종 3+3+3 골라담기 | 프링글스 | 110g 외 10종 골라담기 | low | null | null | snack | 13450 | 네이버 | 외 10종 |
| 123912 | [기타] 오뚜기몰 오뚜기밥 3입 3개 골라담기 | 오뚜기 | 오뚜기밥 3입 3개 골라담기 | low | null | null | instant_food | null | 오뚜기몰 | 골라담기 |
| 77069 | [쓰리알샵] T700 외 82건 특가 | 3RSYS | T700 외 82건 | low | null | null | etc | null | 쓰리알샵 | 외 82건 (대분류) |
| 161518 | [컴퓨존] [MSI] 공랭 쿨러 신년 할인 - MSI MAG 코어프로저 AA13 ARGB | MSI | MAG 코어프로 AA13 ARGB 쿨러 | high | msi:mag-coreprozer-aa13-argb | msi:mag-coreprozer-aa13 | cpu_cooler | 0 | 컴퓨존 | |

---

## O. 여행/숙박/기타 (canonical_key=null) — 4건

| id | title | brand | model | specificity | canonical_key | base_key | category | price | platform | note |
|---|---|---|---|---|---|---|---|---|---|---|
| 136851 | [기타] 모두투어 인기여행지 특가 코타키나발루 준특급 3박5일 | 모두투어 | 코타키나발루 준특급 3박5일 | low | null | null | travel | null | 모두투어 | 패키지 여행 (대분류) |
| 73020 | 누워서 책보기 거치대 ●▅▇█▇▆▅▄▇ | null | 누워서 책보기 거치대 | low | null | null | etc | null | null | 노브랜드 광고성 |
| 11185 | [펑] | null | 펑 | low | null | null | etc | null | null | 의미 불명 |
| 99986 | 간편한 판매로 매일 수익 쌓기! | null | 게임 판매 | low | null | null | promotion | null | null | 광고성 |
| 87304 | [11번가] 비비고 만두 | 비비고 | 만두 | low | null | null | frozen_food | null | 11번가 | 너무 광범위, 모델명 부재 |
| 163993 | [리디] 메가마크다운 전자책 할인 | 리디 | 메가마크다운 전자책 할인 | low | null | null | book | 0 | 리디 | 다중 도서 정리성 |

---

## 라벨링 통계 요약

| 분류 | 건수 |
|---|---|
| 일반 상품 (사양 명확) | ~60 |
| 패키지 용량 (음료/식품) | 20 |
| 식품 (g/ea) | 15 |
| 생활용품/위생/영양제 | 10 |
| 한정판/에디션 | 5 |
| 게임 SW | 10 |
| 모바일 앱 일시무료 | 4 |
| 상품권/기프티콘 | 10 |
| 디지털 구독 | 6 |
| 통신 요금제 | 5 |
| 비상품 (적립/이벤트) | 15 |
| 의류 시즌/스타일 | 8 |
| 다중 SKU 게시글 | 8 |
| 멀티 카테고리/노브랜드 | 6 |
| 여행/광고성/기타 | 4 |
| **합계** | **~186** |

| canonical_key 부여 비율 | 건수 |
|---|---|
| 부여됨 (NOT NULL) | ~110 (59%) |
| null | ~76 (41%) |

null 비율 41%는 적정 범위 (50~60% 목표 대비 보수적). 운영 데이터에서는 더 높을 수 있음.

---

## 검토 요청 사항

1. **canonical_key 형식 일관성**
   - 영문 소문자 + 하이픈 표기로 통일했는데, 실제로는 영문/한글 매핑이 미해결 (예: "갤럭시 S25" → `samsung:galaxy-s25`)
   - LLM이 한글 그대로 키 만드는 게 더 자연스러울지 검토 필요

2. **base_key 통합 단위 적절성**
   - 갤럭시 S23 울트라의 base는 `samsung:galaxy-s23-ultra` (사양만 빠짐)
   - PS5 30주년 한정판의 base는 `sony:ps5` (한정판이 본체로 흡수)
   - 게임 디럭스 에디션의 base는 타이틀까지만 (`game:dying-light-2`)
   - 일관성 검토 필요

3. **모호 게시글 (model_specificity=low) 판정**
   - 모델번호 없으면 거의 다 low/null 처리했는데, 너무 보수적인지 검토
   - 예: "삼성전자 75인치 4K UHD TV" — 시리즈명 없어도 사양으로 어느 정도 식별 가능?

4. **Promotion vs 진짜 상품 경계**
   - 결제수단/체험단/이벤트 게시글의 경계 모호
   - 예: "SKT 결제 시 40% 청구할인" → promotion으로 처리

5. **노브랜드 농산물/식자재**
   - "스위트 골드키위 1kg" 같은 노브랜드 농산물 → null로 처리했는데, brand="노브랜드"+keyword 매칭으로 가격비교 가능할지

6. **게임 디지털/패키지 구분**
   - 닌텐도 SW의 디지털/패키지 구분이 가격 차이를 만드는지 검증 필요

7. **음향/PC부품 모델명 표기**
   - 영문 모델명 그대로 vs 한글 변환 (예: "ZOTAC GAMING" vs "조탁 게이밍") — 일관 정책 필요

8. **위 표가 검토하기 불편하면 JSON 또는 CSV 형식으로 변환 가능**

검토 후 수정사항 알려주시면 반영하겠습니다.
