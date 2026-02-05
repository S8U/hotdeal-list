# 핫딜 애그리게이터 백엔드 구현 계획

## 현재 상태

| 기능 | 상태 | 비고 |
|------|------|------|
| 크롤링 (6개 커뮤니티) | ✅ 완료 | Spring Scheduler, 3분 주기 |
| AI 가공 (카테고리/상품명/번역) | ✅ 완료 | Spring AI + OpenAI |
| Elasticsearch 연동 | ✅ 완료 | Phase 1 |
| 썸네일 다운로드 + 스토리지 | ✅ 완료 | Phase 1.5 |
| 핫딜 조회 API | ✅ 완료 | Phase 2 |
| 필터 + 검색 | ✅ 완료 | Phase 3 |
| 가격 히스토리 | ❌ 미구현 | Phase 4 |
| JWT 인증 | ❌ 미구현 | Phase 5 |
| 소셜 로그인 | ❌ 미구현 | Phase 6 |
| 웹 푸시 알림 | ❌ 미구현 | Phase 7 |

---

## Phase 1: Elasticsearch 도입 + 핫딜 인덱싱

**목표**: ES 인프라 구축 및 핫딜 데이터 자동 동기화

### 태스크

- [x] 1-1. ES 의존성 추가 (Spring Data Elasticsearch)
- [x] 1-2. Docker Compose에 ES 컨테이너 추가
- [x] 1-3. HotdealDocument 정의 + 인덱스 매핑
- [x] 1-4. HotdealElasticsearchRepository 생성
- [x] 1-5. 핫딜 생성/업데이트 시 ES 동기화 로직
- [x] 1-6. 기존 DB 데이터 → ES 마이그레이션 배치

### 테스트 기준
- [x] ES 컨테이너 정상 실행 (`docker-compose up elasticsearch`)
- [x] 앱 시작 시 기존 DB 데이터가 ES로 마이그레이션
- [x] 새 핫딜 크롤링 시 ES에 자동 인덱싱

### 관련 파일
- `backend/build.gradle.kts` - ES 의존성
- `docker-compose.yml` - ES 컨테이너
- `backend/src/main/kotlin/.../document/HotdealDocument.kt` - ES 문서 정의
- `backend/src/main/kotlin/.../repository/HotdealElasticsearchRepository.kt` - ES 레포지토리
- `backend/src/main/kotlin/.../service/HotdealSearchService.kt` - ES 동기화 서비스

---

## Phase 1.5: 썸네일 다운로드 + 스토리지

**목표**: 핫딜 썸네일 이미지 다운로드 및 저장

### 태스크

- [x] 1.5-1. FileStore 인터페이스 정의 (범용 스토리지)
- [x] 1.5-2. LocalFileStore 구현
- [x] 1.5-3. S3FileStore 구현 (Cloudflare R2 호환)
- [x] 1.5-4. application.yml 스토리지 타입 설정 (local/s3)
- [x] 1.5-5. ThumbnailService 구현 (우선순위: 썸네일 > 본문 첫 이미지, WebP 변환)
- [x] 1.5-6. HotdealService에서 ThumbnailService 연동
- [x] 1.5-7. Hotdeal 엔티티에 thumbnailPath 필드 추가
- [x] 1.5-8. HotdealDocument에 thumbnailPath 필드 추가 + ES 동기화

### 테스트 기준
- [x] 환경변수로 local/s3 스토리지 전환 가능
- [x] 핫딜 생성 시 썸네일 자동 다운로드
- [x] 썸네일 Path가 ES에 인덱싱
- [x] 썸네일 없는 경우 graceful 처리 (null 허용)

### 관련 파일
- `backend/src/main/kotlin/.../store/FileStore.kt` - 스토리지 인터페이스
- `backend/src/main/kotlin/.../store/LocalFileStore.kt` - 로컬 구현
- `backend/src/main/kotlin/.../store/S3FileStore.kt` - S3 구현
- `backend/src/main/kotlin/.../config/FileStoreConfig.kt` - 스토리지 설정
- `backend/src/main/kotlin/.../service/ThumbnailService.kt` - 썸네일 다운로드 서비스

---

## Phase 2: 핫딜 조회 API (ES 기반)

**목표**: ES를 데이터 소스로 하는 핫딜 조회 API

### 태스크

- [x] 2-1. 핫딜 리스트 조회 API (`GET /api/v1/hotdeals`)
- [x] 2-2. search_after 기반 커서 페이지네이션
- [x] 2-3. 핫딜 상세 조회 API (`GET /api/v1/hotdeals/{id}`)
- [x] 2-4. 카테고리 목록 API (`GET /api/v1/categories`)

### 테스트 기준
- [x] `/api/v1/hotdeals` 호출 시 ES에서 데이터 반환
- [x] search_after로 다음 페이지 조회 동작
- [x] Swagger UI에서 API 테스트 가능

### 관련 파일
- `backend/src/main/kotlin/.../controller/HotdealController.kt` - 핫딜 API
- `backend/src/main/kotlin/.../controller/CategoryController.kt` - 카테고리 API
- `backend/src/main/kotlin/.../dto/request/HotdealSearchRequest.kt` - 검색 요청 DTO
- `backend/src/main/kotlin/.../dto/response/HotdealResponse.kt` - 핫딜 응답 DTO
- `backend/src/main/kotlin/.../dto/response/HotdealListResponse.kt` - 핫딜 목록 응답 DTO
- `backend/src/main/kotlin/.../dto/response/CategoryResponse.kt` - 카테고리 응답 DTO
- `backend/src/main/kotlin/.../enums/HotdealSortType.kt` - 정렬 타입

---

## Phase 3: 필터 + 검색 기능

**목표**: 다양한 조건의 필터링 및 검색

### 태스크

- [x] 3-1. 카테고리 필터 쿼리
- [x] 3-2. 가격 범위 필터 쿼리 (KRW)
- [x] 3-3. 커뮤니티(플랫폼) 필터 쿼리
- [x] 3-4. 키워드 검색 쿼리 (제목, 상품명)
- [x] 3-5. 복합 필터 조합 로직

### 테스트 기준
- [x] `?categories=electronics` → 해당 카테고리만 반환
- [x] `?minPrice=10000&maxPrice=50000` → 가격 범위 필터
- [x] `?platforms=COOLENJOY_JIRUM` → 해당 플랫폼 핫딜만 반환
- [x] `?keyword=아이폰` → 키워드 검색 동작

---

## Phase 4: 가격 히스토리

**목표**: 상품명 기반 유사 상품 가격 추이

### 태스크

- [ ] 4-1. 상품명 유사도 검색 ES 쿼리 (match_phrase, more_like_this)
- [ ] 4-2. 가격 히스토리 API (`GET /api/v1/hotdeals/{id}/price-history`)
- [ ] 4-3. 가격 집계 로직 (날짜별 그룹핑)

### 테스트 기준
- [ ] 특정 핫딜의 유사 상품 목록 반환
- [ ] 날짜별 가격 데이터 반환

---

## Phase 5: JWT 인증 시스템

**목표**: 이메일 기반 회원가입/로그인 + JWT

### 태스크

- [ ] 5-1. Spring Security + JWT 의존성 추가
- [ ] 5-2. User, RefreshToken 엔티티 생성
- [ ] 5-3. JWT 토큰 서비스 (Access 15분, Refresh 30일)
- [ ] 5-4. 회원가입 API (`POST /auth/register`)
- [ ] 5-5. 로그인 API (`POST /auth/login`)
- [ ] 5-6. 토큰 갱신 API (`POST /auth/refresh`)
- [ ] 5-7. 로그아웃 API (`POST /auth/logout`) - Soft Delete
- [ ] 5-8. Security 필터 설정 (JWT 검증)

### 테스트 기준
- [ ] 회원가입 → 로그인 → JWT 발급
- [ ] Refresh Token으로 Access Token 갱신
- [ ] 로그아웃 시 Refresh Token 무효화
- [ ] 여러 기기 동시 로그인 지원

---

## Phase 6: 소셜 로그인

**목표**: 구글, 네이버, 카카오 OAuth

### 태스크

- [ ] 6-1. OAuth2 설정 + 공통 로직
- [ ] 6-2. 구글 OAuth 연동
- [ ] 6-3. 네이버 OAuth 연동
- [ ] 6-4. 카카오 OAuth 연동
- [ ] 6-5. 소셜-이메일 계정 연동 로직

### 테스트 기준
- [ ] 각 소셜 로그인 OAuth 플로우 완료
- [ ] 동일 이메일 소셜 계정 자동 연동

---

## Phase 7: 웹 푸시 알림

**목표**: 키워드 기반 핫딜 알림 발송

### 태스크

- [ ] 7-1. FCM 의존성 + 설정
- [ ] 7-2. NotificationKeyword 엔티티
- [ ] 7-3. DeviceToken 엔티티
- [ ] 7-4. 키워드 CRUD API
- [ ] 7-5. 디바이스 토큰 등록/삭제 API
- [ ] 7-6. 기기별 알림 ON/OFF API
- [ ] 7-7. 핫딜 생성 시 키워드 매칭 서비스
- [ ] 7-8. FCM 푸시 발송 서비스

### 테스트 기준
- [ ] 키워드 등록 → 매칭 핫딜 생성 → FCM 발송 확인
- [ ] 기기별 알림 ON/OFF 동작

---

## 진행 순서

```
Phase 1: ES 도입 + 핫딜 인덱싱 (데이터 인프라) ✅ 완료
    ↓
Phase 1.5: 썸네일 다운로드 + 스토리지 ✅ 완료
    ↓
Phase 2: 핫딜 조회 API (ES 기반) ✅ 완료
    ↓
Phase 3: 필터 + 검색 ✅ 완료
    ↓
Phase 4: 가격 히스토리 ← 현재
    ↓
Phase 5: JWT 인증
    ↓
Phase 6: 소셜 로그인
    ↓
Phase 7: 웹 푸시 알림
```

---

## 백로그 (순서 미정)

### ES 검색 품질 개선

**문제**: 현재 ngram_analyzer로 저장하고 standard로 검색하여 일부 검색어가 매칭 안 됨

**개선 방향**:
- 한국어: nori (형태소 분석기) 메인
- 영어: standard 메인
- 서브: edge_ngram (부분 매칭, 자동완성용)

**태스크**:
- [ ] nori 플러그인 설치 및 설정
- [ ] 필드별 multi-field 매핑 (main + edge_ngram 서브필드)
- [ ] multi_match 쿼리 최적화 (title, productName, titleEn, productNameEn)
- [ ] 검색 스코어링 튜닝 (boost 값 조정)
- [ ] 동의어 사전 적용 검토
