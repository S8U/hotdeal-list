# 핫딜 애그리게이터 요구사항 정의서

## 1. 프로젝트 개요

### 1.1 프로젝트명
HotdealList - 핫딜 애그리게이터

### 1.2 목적
여러 커뮤니티의 핫딜 게시판을 자동으로 크롤링하여 통합된 핫딜 정보를 제공하는 웹 애플리케이션

### 1.3 크롤링 대상 커뮤니티

| 커뮤니티 | 게시판 | URL |
|---------|--------|-----|
| 쿨엔조이 | 지름 | https://coolenjoy.net/bbs/jirum |
| 퀘이사존 | 핫딜 | https://quasarzone.com/bbs/qb_saleinfo |
| 퀘이사존 | 타세요 | https://quasarzone.com/bbs/qb_tsy |
| 클리앙 | 알뜰구매 | https://www.clien.net/service/board/jirum |
| 루리웹 | 핫딜 | https://bbs.ruliweb.com/market/board/1020 |
| 뽐뿌 | 국내핫딜 | https://www.ppomppu.co.kr/zboard/zboard.php?id=ppomppu |
| 뽐뿌 | 해외뽐뿌 | https://www.ppomppu.co.kr/zboard/zboard.php?id=ppomppu4 |
| 조드 | 특가 | https://zod.kr/deal |

## 2. 주요 기능 요구사항

### 2.1 크롤링 시스템

#### 2.1.1 수집 데이터
**필수 항목:**
- 게시글 제목, 내용, URL
- 작성자, 작성일시
- 조회수, 추천수, 댓글 수
- 쇼핑몰 URL (게시글 내 링크)
- 썸네일 이미지

**메타데이터:**
- 크롤링 일시
- 커뮤니티별 고유 게시글 ID
- 삭제 여부

#### 2.1.2 크롤링 주기
- 기본: 5분 간격
- 커뮤니티별 설정 가능

#### 2.1.3 중복 방지
- 커뮤니티 타입 + 게시글 ID로 유니크 키 생성
- 기존 게시글은 조회수/추천수만 업데이트

### 2.2 이미지 관리

#### 2.2.1 로컬
- 로컬 파일 시스템에 저장
- 경로: `/uploads/thumbnails/{communityType}/{postId}/{filename}`

#### 2.2.2 CDN
- 설정을 통한 로컬/CDN 선택 가능
- AWS S3, Cloudflare R2 지원

#### 2.2.3 쇼핑몰 OG 이미지
- 쇼핑몰 URL에서 `og:image` 메타태그 추출
- 이미지 품질 검증

### 2.3 데이터 가공 및 노출

#### 2.3.1 핫딜 생성 프로세스
1. 커뮤니티 게시글 크롤링 → `hotdeal_community_raws` 저장
2. 쇼핑몰 링크 추출 → `hotdeal_source_raws` 저장 (1:N)
3. 데이터 가공 → `hotdeals` 생성 (1:1)
   - 제목 정제
   - 카테고리 자동 분류
   - 대표 쇼핑몰 링크 선택
   - 가격/할인율 파싱
   - 인기도 점수 계산

## 3. 시스템 플로우

### 3.1 기본 사용 시나리오
```
1. 크롤러가 5분마다 여러 커뮤니티의 핫딜 게시판 크롤링
2. 새 게시글 발견 시:
   - 커뮤니티 타입 + 게시글 ID로 중복 확인
   - 신규일 경우 게시글 정보 저장
   - 게시글 내 쇼핑몰 URL 추출 → 쇼핑몰 상품 정보 저장
   - 데이터 가공 → 노출용 데이터 생성
3. 기존 게시글일 경우:
   - 조회수, 추천수, 댓글수만 업데이트
4. 사용자가 핫딜 리스트 조회
   - hotdeals 테이블에서 ACTIVE 상태만 조회
   - 정렬/필터 적용하여 반환
```