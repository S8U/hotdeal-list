# Admin Operations API

운영자가 수동으로 핫딜 파이프라인을 트리거하는 내부 API. 크롤러 장애 복구, 재가공 (프롬프트 변경 시), 검색엔진 재색인 (매핑 변경 시), 썸네일 재다운로드 같은 비정기 운영 작업을 지원한다.

## 단일 엔드포인트 + 단계 선택 구조

```
POST /admin/ops/run
```

5단계 파이프라인(`RAW → THUMBNAIL → PROCESS → HOTDEAL → ES`)을 사용자가 원하는 단계만 선택해 실행한다. 단계 순서는 서버가 항상 고정 순서로 실행한다 (사용자는 Set으로 선택만).

## 보안

**임시 정책: localhost 접근만 허용**

- 컨트롤러에서 `request.remoteAddr`가 `127.0.0.1` 또는 `0:0:0:0:0:0:0:1` (`::1`)이 아니면 **404 응답** (엔드포인트 존재 자체를 숨김)
- 운영 환경에서는 nginx에서 `/admin/*` 외부 차단 + Tailscale로 SSH 접속 후 `curl localhost:8080/admin/...`
- Swagger 문서에 노출하지 않음 (`@Hidden`)
- 추후 admin 인증/UI 추가 시 이 게이트는 `@PreAuthorize` 어노테이션으로 교체

## 요청 본문

```json
{
  "stages": ["RAW", "THUMBNAIL", "PROCESS", "HOTDEAL", "ES"],

  "target": {
    "rawId": null,
    "hotdealId": null,
    "platform": "RULIWEB_HOTDEAL",
    "minPage": null,
    "maxPage": null,
    "wroteAtFrom": "2026-05-02T00:00:00",
    "wroteAtTo": "2026-05-02T23:59:59",
    "limit": null,
    "maxPagesToScan": 50
  },

  "raw":       { "delayMs": 1500 },
  "thumbnail": { "force": false },
  "process":   { "force": false, "promptVersion": null },

  "dryRun": false
}
```

### `stages` (필수)

`Set<Stage>`. 가능한 값: `RAW`, `THUMBNAIL`, `PROCESS`, `HOTDEAL`, `ES`.

- 비어있으면 안 됨
- 순서는 무관 — 서버가 항상 `RAW → THUMBNAIL → PROCESS → HOTDEAL → ES` 순으로 실행

### `target` (필수)

대상을 정하는 방법.

| 필드 | 의미 | 우선순위 |
|---|---|---|
| `rawId` | 단건 raw 지정 | 1 (가장 강함) |
| `hotdealId` | 단건 hotdeal 지정 | 2 |
| `platform` | 플랫폼 | — |
| `minPage` + `maxPage` | 페이지 범위 (RAW 단계용, 1-based) | — |
| `wroteAtFrom`, `wroteAtTo` | 작성 시각 범위 | — |
| `limit` | 최대 처리 건수 (max 10000) | — |
| `maxPagesToScan` | wroteAt/limit 모드 페이지 순회 안전상한 (max 1000, 기본 50) | — |

**검증**:
- 최소 하나 non-null
- stages에 `RAW` 포함 시 `(minPage+maxPage)` 또는 `wroteAtFrom` 또는 `limit` 중 하나 필수
- `wroteAtFrom <= wroteAtTo` (둘 다 지정 시)
- `limit in 1..10000`
- `maxPagesToScan in 1..1000`

### 단계별 옵션

#### `raw` (RAW 단계)
- `delayMs: Long = 1000` — 페이지 간 대기 (외부 사이트 부하 고려)

#### `thumbnail` (THUMBNAIL 단계)
- `force: Boolean = false` — true면 `thumbnail_path` 있어도 강제 재다운로드. false면 기존 path 있는 raw는 skip.

#### `process` (PROCESS 단계)
- `force: Boolean = false` — true면 기존 process 있어도 AI 재호출. false면 process 없는 raw만 호출.
- `promptVersion: String? = null` — Phase B에서 v2 분기에 사용. Phase A에서는 받기만 하고 무시 (errors에 안내).

#### HOTDEAL/ES 단계는 옵션 없음

- HOTDEAL: 항상 "현재 raw + 최신 process로 hotdeal upsert"
- ES: 항상 "hotdeal → ES 색인"

### `dryRun`

- 외부 HTTP/AI 호출 안 함, DB/ES 쓰기 안 함
- target resolve 결과만 응답
- **PROCESS 단계만 비용 추정 추가**: 토큰 수 × 모델 단가 → USD

## 단계별 동작

### 1. RAW
- `target`에서 (platform, page 범위 또는 wroteAt 또는 limit)으로 모드 결정
- 모드:
  - **PAGE**: `minPage..maxPage` 순회
  - **WROTE_AT**: 1페이지부터 순회, 페이지의 모든 raw가 `wroteAtFrom` 이전이면 종료. `maxPagesToScan` 안전상한
  - **LIMIT**: 1페이지부터 순회, 누적 raw가 `limit` 도달 시 종료. `maxPagesToScan` 안전상한
- raw는 `(platform_type, platform_post_id)` UNIQUE 기준 upsert
- 다음 단계로 처리한 raw_id 리스트 전달

### 2. THUMBNAIL
- 입력: 직전 RAW 단계 결과 또는 `target`에서 resolve한 raw_id 리스트
- 각 raw에 대해:
  - `thumbnail_path` 없거나 `thumbnail.force = true`면 다운로드 + 저장 + `thumbnail_path` 갱신
  - 그 외 skip
- 다음 단계로 같은 raw_id 리스트 전달

### 3. PROCESS
- 입력: 직전 단계 결과 또는 `target`에서 resolve한 raw_id 리스트
- 각 raw에 대해:
  - `process.force = false` (기본): `hotdeal_processes`에 해당 raw 레코드 있으면 skip
  - `process.force = true`: 항상 AI 호출, 새 process row INSERT (덮어쓰기 아님 — 기존 row 유지, 최신 row만 사용)
- 다음 단계로 같은 raw_id 리스트 전달

### 4. HOTDEAL
- 입력: 직전 단계 결과 또는 `target`에서 resolve한 raw_id 리스트
- 각 raw에 대해 신규 메서드 `upsertHotdealFromRawAndProcess(rawId)` 호출:

#### upsert 로직
```
hotdeal = findByHotdealRawId(rawId)
hotdealProcess = 최신 process

if hotdeal == null:
    # 신규 생성
    hotdeal = new Hotdeal(...)  # raw 메타 + process 본문
    hotdeal.hotdealProcessId = hotdealProcess?.id
    save
    if hotdealProcess: 카테고리 생성 (부모 체인 포함)
    ES 색인  # ES 단계가 stages에 있다면 그 단계가 처리. 여기선 색인 안 함
else:
    # 메타 (view/comment/like/ended)는 raw에서 항상 반영
    hotdeal.viewCount = raw.viewCount
    ...

    # 본문 + 카테고리는 process_id 변경 시만 반영
    if hotdealProcess != null && hotdeal.hotdealProcessId != hotdealProcess.id:
        hotdeal.titleEn = process.titleEn
        hotdeal.productName = process.productName
        ...
        hotdeal.hotdealProcessId = hotdealProcess.id
        # 카테고리 재구성: 기존 삭제 + 부모 체인 재생성
        deleteByHotdealId
        saveAll(부모체인)

    save(hotdeal)
```

**ES 색인은 ES 단계가 처리** (HOTDEAL 단계에서는 hotdeal/카테고리만 갱신).

- 다음 단계로 hotdeal_id 리스트 전달

### 5. ES
- 입력: 직전 단계 결과 또는 `target`에서 resolve한 hotdeal_id 리스트
- bulk index (`indexAllToIndex`, 1000건 청크)

## 단계 의존성과 데이터 흐름

각 단계는 **다음 단계가 쓸 ID 리스트를 반환**.

- 첫 단계는 `target`에서 ID 추출 (`rawId` 단건이거나 조건 검색)
- 이후 단계는 직전 단계 출력을 입력으로 사용
- 단, 직전 단계가 stages에 없으면 → 자기 단계에서 `target`으로 다시 resolve

예시:
- `["PROCESS", "HOTDEAL", "ES"]`: PROCESS가 첫 단계 → target으로 raw_id 검색 → 다음 단계로 전달
- `["ES"]`: ES만 → target으로 hotdeal_id 검색
- `["RAW", "ES"]`: RAW가 raw_id 반환 → ES는 그 raw들에 해당하는 hotdeal_id를 자기가 조회 (단, hotdeal이 있어야 함)

## 단건 실패 정책

- 각 단계 내에서 단건 실패는 `errors` 배열에 누적, 다음 항목 계속 진행
- 단계 자체 치명적 오류 (DB 연결 실패 등)만 즉시 500
- 단계 의존성 검증 실패 시 errors 누적 후 진행

## 응답

```json
{
  "elapsedMs": 142391,
  "dryRun": false,
  "stages": ["RAW","THUMBNAIL","PROCESS","HOTDEAL","ES"],
  "totals": {
    "targeted": 100,
    "processed": 98,
    "skipped": 1,
    "failed": 1
  },
  "byStage": {
    "raw":       { "created": 30, "updated": 70, "failed": 0 },
    "thumbnail": { "downloaded": 30, "skipped": 70, "failed": 0 },
    "process":   { "called": 100, "skipped": 0, "failed": 1 },
    "hotdeal":   { "created": 30, "updated": 69, "categoryRebuilt": 25, "failed": 0 },
    "es":        { "indexed": 99, "failed": 0 }
  },
  "errors": [
    { "rawId": 12345, "stage": "process", "message": "..." }
  ]
}
```

dryRun일 때:
```json
{
  "elapsedMs": 12,
  "dryRun": true,
  "stages": ["PROCESS", "HOTDEAL", "ES"],
  "totals": { "targeted": 1000, "processed": 0, "skipped": 0, "failed": 0 },
  "byStage": {
    "process": {
      "wouldProcess": 1000,
      "estimatedTokensIn": 5400000,
      "estimatedTokensOut": 600000,
      "estimatedCostUsd": 0.84
    },
    "hotdeal": { "wouldUpsert": 1000 },
    "es":      { "wouldIndex": 1000 }
  },
  "errors": []
}
```

## 시나리오 예시

### 일반 크롤링 (정기 스케줄러와 같은 동작)
```json
{
  "stages": ["RAW", "THUMBNAIL", "PROCESS", "HOTDEAL", "ES"],
  "target": { "minPage": 1, "maxPage": 3 }
}
```

### raw만 있고 process부터 깨진 거 복구
```json
{
  "stages": ["PROCESS", "HOTDEAL", "ES"],
  "target": {
    "platform": "RULIWEB_HOTDEAL",
    "wroteAtFrom": "2026-05-02T00:00:00",
    "wroteAtTo": "2026-05-02T23:59:59"
  }
}
```

### 프롬프트 변경 후 v2 재가공 (Phase B)
```json
{
  "stages": ["PROCESS", "HOTDEAL", "ES"],
  "target": { "limit": 1000 },
  "process": { "force": true, "promptVersion": "v2-compact-r1" }
}
```

### ES 매핑 변경 후 재색인
```json
{
  "stages": ["ES"],
  "target": { "limit": 10000 }
}
```

### 썸네일 재다운로드
```json
{
  "stages": ["THUMBNAIL"],
  "target": { "limit": 100 },
  "thumbnail": { "force": true }
}
```

### 단건 디버깅
```json
{
  "stages": ["PROCESS", "HOTDEAL", "ES"],
  "target": { "rawId": 12345 },
  "process": { "force": true }
}
```

## 멱등성

- **RAW**: `(platform_type, platform_post_id)` UNIQUE → 같은 게시글은 update
- **THUMBNAIL**: `force=false`면 skip → 멱등. `force=true`면 매번 다운로드 (외부 자원 변경 가능)
- **PROCESS**: `force=false`면 skip → 멱등. `force=true`면 매번 INSERT (process row 누적, 단 hotdeal에는 최신만 적용)
- **HOTDEAL**: `hotdeal_process_id`로 변경 감지 → 같은 process_id면 본문/카테고리 안 건드림 → 멱등
- **ES**: `_id = hotdeal.id` → 덮어쓰기 → 멱등

## 비동기

1차 범위는 **동기 실행만**. 1000건 reprocess는 동기로 5~10분 (haiku ~2초/건).

## DB 스키마 변경

### `hotdeals` 테이블 — 엔티티에 컬럼 추가만 (Hibernate ddl-auto가 자동 ALTER)

`Hotdeal.kt`:
```kotlin
@Column(comment = "현재 핫딜에 적용된 가공 데이터 ID")
var hotdealProcessId: Long? = null
```

인덱스도 `@Table(indexes = ...)`에 추가:
```kotlin
Index(name = "idx_hotdeal_process_id", columnList = "hotdeal_process_id")
```

### 기존 데이터 백필
- `hotdealProcessId`는 NULL 시작
- 첫 admin ops `upsert` 호출 시 자동으로 채워짐 (NULL이면 "한 번도 적용 안 됨"으로 간주, 본문 갱신)
- 별도 마이그레이션 불필요

## 패키지 / 파일 배치

기존 프로젝트 컨벤션을 따른다. `admin/` 하위 패키지를 만들지 않고 평면 배치.

```
controller/
└── AdminOpsController.kt           ← @Hidden, localhost 게이트, POST /admin/ops/run

service/
├── AdminOpsService.kt              ← 단계 오케스트레이션
├── AdminOpsTargetResolver.kt       ← target → ID 리스트 (raw/hotdeal 양쪽)
└── (기존 Hotdeal*Service.kt 변경)

dto/request/
├── AdminOpsRequest.kt              ← 최상위
├── AdminOpsTarget.kt               ← target sub-object
├── AdminOpsRawOptions.kt           ← raw 단계 옵션
├── AdminOpsThumbnailOptions.kt     ← thumbnail 단계 옵션
└── AdminOpsProcessOptions.kt       ← process 단계 옵션

dto/response/
├── AdminOpsResponse.kt             ← 최상위
├── AdminOpsTotals.kt
└── AdminOpsError.kt

enums/
└── AdminOpsStage.kt                ← RAW, THUMBNAIL, PROCESS, HOTDEAL, ES
```

각 DTO는 파일 하나당 클래스 하나. 모든 Request DTO는 `@Schema` 어노테이션 적용.

## 코드 추가/변경

### 신규
- 위 파일 13개

### 변경
- `entity/Hotdeal.kt`: `hotdealProcessId: Long?` 컬럼 추가 (var). 이미 변경된 var 필드들 유지.
- `service/HotdealService.kt`:
  - 신규 `upsertHotdealFromRawAndProcess(rawId)` (process_id 변경 감지 + 본문/카테고리 갱신)
  - 카테고리 부모 체인 빌더 헬퍼 `buildAncestorCategoryChain(hotdealId, leafCategory)` 추출 (createHotdealFromRawAndProcess와 공유)
  - `createHotdealFromRawAndProcess`: hotdeal 생성 시 `hotdealProcessId` 채우도록 수정
  - 기존 `updateHotdealFromRaw`, `refreshHotdealAfterReprocess`는 유지 (정기 크롤러가 사용)
- `repository/HotdealCategoryRepository.kt`: `deleteByHotdealId` 유지 (이미 추가됨)
- `repository/HotdealRepository.kt`: `findIdsByCriteria` 유지

### 롤백 (기존 admin ops 코드 삭제)
- `controller/AdminOpsController.kt` (재작성)
- `service/AdminCrawlService.kt`, `AdminReprocessService.kt`, `AdminReindexService.kt` 삭제
- `service/AdminOpsTargetResolver.kt` 재작성
- `dto/request/AdminCrawlRequest.kt`, `AdminReprocessRequest.kt`, `AdminReindexRequest.kt` 삭제
- `dto/response/AdminCrawlResponse.kt`, `AdminReprocessResponse.kt`, `AdminReindexResponse.kt` 삭제
- `dto/response/AdminOpsTotals.kt`, `AdminOpsError.kt` 유지 (그대로 사용)

## 구현 범위

### Phase A (이 문서)
- 단일 엔드포인트 `POST /admin/ops/run`
- 5단계 + 단계별 옵션
- v1 가공만 동작 (`process.promptVersion` 받기만 함)
- localhost 게이트
- `hotdeals.hotdeal_process_id` 컬럼 추가

### Phase B (별도 PR)
- v2 스키마 마이그레이션 (Hotdeal/Process/Document 컬럼 추가)
- `HotdealProcessServiceV2` (압축 v2 프롬프트 + JSON 파싱)
- `process.promptVersion` 분기 적용

### Phase C (이후)
- 비동기 Job 큐
- admin Frontend UI
- Spring Security 기반 권한 게이트 (localhost 게이트 제거)
