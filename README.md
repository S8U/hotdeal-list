# 핫딜 애그리게이터

여러 커뮤니티의 핫딜 게시글을 크롤링하여 한 곳에서 볼 수 있는 애그리게이터 사이트입니다.

---

## 데이터 수집 대상 커뮤니티

| 커뮤니티 | 게시판 | URL |
|------|-------|-----|
| 쿨엔조이 | 지름 | https://coolenjoy.net/bbs/jirum |
| 퀘이사존 | 핫딜 | https://quasarzone.com/bbs/qb_saleinfo |
| 퀘이사존 | 타세요 | https://quasarzone.com/bbs/qb_tsy |
| 클리앙 | 알뜰구매 | https://www.clien.net/service/board/jirum |
| 루리웹 | 핫딜 | https://bbs.ruliweb.com/market/board/1020 |
| 뽐뿌 | 뽐뿌게시판 | https://www.ppomppu.co.kr/zboard/zboard.php?id=ppomppu |

---

## 주요 기능

- **핫딜 크롤링** - 6개 커뮤니티에서 3분 주기로 신규 게시글 수집
- **AI 데이터 가공** - OpenAI를 통해 카테고리 추출, 상품명 정제, 영문 번역 자동화
- **Elasticsearch 검색** - search_after 커서 페이지네이션
- **썸네일 처리** - 원본 이미지 다운로드 후 WebP 변환, 로컬 또는 Cloudflare R2에 저장
- **핫딜 조회 API** - 카테고리, 가격 범위, 플랫폼, 키워드 필터 및 검색
- **가격 히스토리** - 상품명 유사도 기반 동일 상품의 가격 변동 이력 조회

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| **Backend** | Kotlin, Spring Boot 4.0.1, JDK 21 |
| **Backend 라이브러리** | Spring Data JPA, Spring Data Elasticsearch, Spring AI (OpenAI), Jsoup, Scrimage, AWS S3 SDK |
| **Frontend** | Next.js 16.1.1, React 19, TypeScript, Tailwind CSS 4 |
| **DB** | MySQL / MariaDB, Elasticsearch |
| **스토리지** | Cloudflare R2 (S3 호환) / 로컬 파일 |
| **인프라** | Docker, Docker Compose, Nginx |

---

## 시작하기

### 사전 요구사항

- Docker, Docker Compose
- MySQL / MariaDB
- Elasticsearch
- OpenAI API 키
- Cloudflare R2 버킷 (또는 로컬 스토리지 사용 시 불필요)

### 환경 변수 설정

**`backend/.env`**

```env
# DB
SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/hotdeal
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=password

# Elasticsearch
SPRING_ELASTICSEARCH_URIS=http://host:9200

# OpenAI
SPRING_AI_OPENAI_API_KEY=sk-...

# Storage (LOCAL 또는 S3)
FILE_STORE_TYPE=LOCAL
# S3 사용 시 추가 설정
AWS_S3_ENDPOINT=https://xxx.r2.cloudflarestorage.com
AWS_S3_ACCESS_KEY=...
AWS_S3_SECRET_KEY=...
AWS_S3_BUCKET=hotdeal-thumbnails
AWS_S3_REGION=auto
```

**`frontend/.env`**

```env
NEXT_PUBLIC_API_URL=http://localhost/api
```

### Docker Compose로 실행

```bash
# 저장소 클론
git clone <repository-url>
cd hotdeal-list

# 환경 변수 파일 생성
cp backend/.env.example backend/.env   # 위 환경 변수 참고하여 수정
cp frontend/.env.example frontend/.env

# 전체 스택 빌드 및 실행
docker compose up -d

# 로그 확인
docker compose logs -f
```

실행 후 접속:
- 서비스: http://localhost
- Swagger UI: http://localhost/swagger-ui/

---

## 개발 환경

### Backend (Spring Boot + Kotlin)

```bash
cd backend

# Gradle로 빌드
./gradlew build

# 개발 서버 실행 (application.yml 또는 환경 변수 필요)
./gradlew bootRun
```

- 실행 포트: `8080`
- Swagger UI: http://localhost:8080/swagger-ui/

### Frontend (Next.js)

```bash
cd frontend

# 의존성 설치
pnpm install

# 개발 서버 실행
pnpm dev
```

- 실행 포트: `3000`
- 접속: http://localhost:3000
