# 애널리틱스 / GTM 이벤트

GTM 컨테이너로 dataLayer에 발행하는 커스텀 이벤트 명세.

- 발행 헬퍼: `frontend/src/lib/gtm.ts` 의 `gtmEvent(event, params)`
- 환경변수: `NEXT_PUBLIC_GTM_ID` (비워두면 GTM 비활성, no-op)
- GA4 측정 ID는 GTM 콘솔 안의 "Google 태그"에서 등록 (사이트 코드는 GTM ID만 알면 됨)

## 자동 이벤트
| 이벤트 | 설명 |
|---|---|
| `page_view` | GTM/GA4 기본 제공. 페이지 로드 시 자동 |

## 커스텀 이벤트
| 구분 | 파일 | 이벤트 | params | 발생 시점 |
|---|---|---|---|---|
| 검색 | `site-header.tsx` | `search` | `keyword` | 검색창에서 Enter 또는 검색 실행 |
| 검색 | `site-header.tsx` | `search_suggestion_click` | `keyword`, `query`(원래 입력값) | 자동완성 항목 클릭 |
| 검색 | `site-header.tsx` | `recent_search_click` | `keyword` | 최근 검색어 클릭 |
| 필터/스크롤 | `app/page.tsx` | `filter_apply` | `categoryCode`, `platforms`, `priceMin`, `priceMax` | 필터 변경 250ms debounce 또는 모바일 sheet "적용" |
| 필터/스크롤 | `app/page.tsx` | `filter_chip_open` | `chip`(`category`/`price`/`community`) | 모바일 필터 칩 열기 |
| 필터/스크롤 | `app/page.tsx` | `filter_reset` | - | 필터 초기화 |
| 필터/스크롤 | `app/page.tsx` | `infinite_scroll_load` | `pageIndex` | IntersectionObserver로 다음 페이지 로드 |
| 핫딜 카드 | `deal-card.tsx` | `deal_click` | `hotdealId`, `platformType`, `categoryCode`, `price` | 카드 썸네일/제목 클릭 |
| 핫딜 카드 | `deal-card.tsx` | `deal_category_click` | `categoryCode` | 카드 내 카테고리 태그 클릭 |
| 핫딜 카드 | `deal-card.tsx` | `deal_community_click` | `platformType` | 카드 내 커뮤니티 태그 클릭 |
| 가격 추이 | `price-history-dialog.tsx` | `price_history_open` | `hotdealId`, `productName` | 가격추이 다이얼로그 열기 |
| 가격 추이 | `price-history-dialog.tsx` | `price_history_deal_click` | `hotdealId`, `sourceHotdealId`, `date` | 다이얼로그 내 핫딜 항목 클릭 |
