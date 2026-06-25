# 🏠 구해즈 홈줘

## 🧰 기술 스택

| 영역 | 기술 |
| --- | --- |
| Frontend | Vue 3, Vue Router 4, Vite 7, Axios |
| Backend | Java 21, Spring Boot 3.5.x, Spring MVC REST API |
| Security | Spring Security, JWT Access/Refresh Token, JJWT |
| Database | MySQL 8.x, HikariCP, MyBatis XML Mapper |
| Batch | Spring Batch, Spring Scheduling |
| AI/RAG | Spring AI OpenAI, GPT API, OpenAI Embedding, RAG 문서 검색 |
| Cache | Redis, Caffeine Cache |
| Map/Geocoding | Kakao Map JavaScript SDK, Kakao Local API |
| API 문서 | springdoc-openapi, Swagger UI |
| Data Source | 공공데이터포털 실거래 API, 소상공인시장진흥공단 상권 API, 서울시 버스 정류장 API, 파일 기반 학교·공원·지하철 데이터 |

<br>

## 🧭 개발 구조

### 🏗️ 전체 구조

```text
사용자
→ Vue SPA
→ Axios REST API 호출
→ Spring Boot API
→ Service / Score Calculator / Batch / RAG
→ MyBatis Mapper
→ MySQL
```

프론트엔드는 Vue SPA로 화면 전환을 담당하고, 백엔드는 `/api/**` REST API로 JSON 데이터를 제공합니다.
주요 기능은 실거래 검색, 조건 기반 동네 추천, 관심지역, 게시판, 공지사항, AI 챗봇/동네 요약으로 나뉩니다.

### ☕ 백엔드 패키지 구조

```text
backend/src/main/java/com/ssafy/prj
├─ controller/api        # REST API 컨트롤러
├─ model
│  ├─ dto                # API 응답/요청 DTO
│  ├─ mapper             # MyBatis Mapper 인터페이스
│  └─ service            # 도메인 서비스
│     └─ neighborhood    # 동네 추천 서비스와 점수 계산기
├─ batch                 # 실거래·상권·버스 데이터 수집 배치
├─ chat                  # Spring AI 챗봇, RAG 검색, 추천 설명 함수
├─ security              # Spring Security, JWT 인증/인가
├─ filter                # 공통 필터
├─ config                # Cache, CORS, Transaction 등 설정
└─ util                  # 공통 유틸
```

### 🎨 프론트엔드 구조

```text
frontend/src
├─ api          # Axios 기반 API 호출 함수
├─ views        # 페이지 단위 Vue 컴포넌트
├─ components   # 공통 UI와 동네 추천 단계별 컴포넌트
├─ router       # Vue Router 설정
├─ assets       # 이미지·정적 리소스
└─ utils        # 포맷팅 등 공통 함수
```

### 🏘️ 동네 추천 처리 흐름

```text
기준 위치 입력
→ 반경·거래 유형·예산 선택
→ 생활 우선순위 3개 선택
→ 후보 법정동 조회
→ 카테고리별 점수 계산
→ 상위 3개 추천
→ 상세 화면에서 매물 목록 + AI/RAG 요약 확인
```

점수 계산에는 학교, 공원, 상권, 의료, 반려, 교통 지표가 사용됩니다.
상권·버스·실거래 데이터는 배치로 갱신하고, 학교·공원·지하철 데이터는 파일 기반으로 적재합니다.

<br>

## ✨ 주요 기능

### 🔎 주택 실거래가 검색

구현 내용:

- 기존 JSP 주택 검색 화면을 Vue SPA 화면으로 이전
- 시도/시군구/읍면동 선택 목록을 Vue에서 동적으로 조회
- 선택한 지역 코드, 동명, 아파트명을 조건으로 주택 실거래가 검색
- 검색 결과를 Vue 화면에서 목록과 지도에 함께 표시
- 백엔드는 `GET /api/houses` REST API로 JSON 데이터 제공

관련 화면 및 코드:

- `frontend/src/views/HomeView.vue`
- `frontend/src/views/HouseListView.vue`
- `frontend/src/api/houses.js`
- `frontend/src/api/regions.js`

<br>

### ⭐ 관심 지역 관리

구현 내용:

- 기존 JSP 마이페이지의 관심지역 영역을 Vue 화면으로 이전
- 로그인한 사용자의 관심지역 목록 조회
- 관심지역 삭제 기능을 REST API로 연결
- JWT 로그인 상태를 기준으로 사용자별 관심지역만 표시

관련 화면 및 코드:

- `frontend/src/views/MyPageView.vue`
- `frontend/src/api/favorites.js`
- `backend/src/main/java/com/ssafy/prj/controller/api/FavoriteApiController.java`

<br>

### 📦 동네 추천용 생활 인프라 데이터 수집

구현 내용:

- 동네 추천 점수 계산에 필요한 생활 인프라 데이터를 법정동 단위로 집계
- 상권 원본 점포 전체를 저장하지 않고 `dong_commercial_metrics`에 카테고리별 업소 수만 저장
- 카페·외식, 운동·문화, 장보기·생활편의, 의료, 반려용품, 동물병원 데이터를 공공데이터 API 배치로 수집
- 버스 정류장은 서울시 API와 좌표→법정동 변환 캐시를 활용해 법정동별 정류장 수로 집계
- 학교·공원·지하철은 정적 시설 데이터로 보고 파일 기반으로 적재

관련 파일:

- `backend/src/main/java/com/ssafy/prj/batch/commercial/CommercialMetricsCollectTasklet.java`
- `backend/src/main/java/com/ssafy/prj/batch/bus/BusStopMetricsCollectTasklet.java`
- `sql/dong_commercial_metrics.sql`
- `sql/dong_bus_stop_metrics.sql`

<br>

### 🧩 조건 기반 동네 추천

구현 내용:

- 기준 위치와 최대 반경으로 후보 법정동을 찾음
- 거래 유형과 예산 조건으로 실제 매물이 있는 동네를 필터링
- 사용자가 선택한 생활 우선순위 3개를 기준으로 학교·공원·상권·의료·반려·교통 점수를 계산
- 추천 결과에서 서울시 기준 상위 비율과 AI/RAG 기반 동네 요약을 제공
- 추천 동네 상세 화면에서 조건에 맞는 실제 매물 목록을 함께 확인

관련 화면 및 코드:

- `frontend/src/views/NeighborhoodRecommendView.vue`
- `backend/src/main/java/com/ssafy/prj/controller/api/neighborhood/NeighborhoodRecommendApiController.java`
- `backend/src/main/java/com/ssafy/prj/model/service/neighborhood/NeighborhoodRecommendServiceImpl.java`

<br>

### 📢 공지사항 관리

구현 내용:

- 기존 JSP 공지사항 목록/상세/등록/수정 화면을 Vue로 이전
- 공지사항 목록 조회와 키워드 검색 제공
- 공지사항 상세 조회 제공
- 관리자 권한으로 공지사항 등록, 수정, 삭제 가능
- 기존 form submit 방식에서 Axios 기반 REST 요청 방식으로 전환

관련 화면 및 코드:

- `frontend/src/views/NoticeListView.vue`
- `frontend/src/views/NoticeDetailView.vue`
- `frontend/src/views/NoticeFormView.vue`
- `frontend/src/api/notices.js`

<br>

### 🐣 회원가입

구현 내용:

- 기존 JSP 회원가입 화면을 Vue 화면으로 이전
- 회원가입 폼 입력값을 JSON으로 전송
- `POST /api/members` REST API로 회원 등록
- 회원가입 완료 후 로그인 화면으로 이동

관련 화면 및 코드:

- `frontend/src/views/RegisterView.vue`
- `frontend/src/api/auth.js`
- `backend/src/main/java/com/ssafy/prj/controller/api/MemberApiController.java`

<br>

### 👤 회원 정보 조회

구현 내용:

- 기존 JSP 마이페이지와 관리자 회원 목록 화면을 Vue로 이전
- 로그인한 사용자의 회원 정보 조회
- 관리자 권한으로 전체 회원 목록 조회
- Access Token과 Refresh Token 기반으로 현재 사용자 정보를 유지

관련 화면 및 코드:

- `frontend/src/views/MyPageView.vue`
- `frontend/src/views/AdminUsersView.vue`
- `frontend/src/api/auth.js`
- `frontend/src/api/admin.js`

<br>

### ✏️ 회원 정보 수정

구현 내용:

- 마이페이지에서 회원 정보 수정 기능을 Vue 화면으로 이전
- 로그인한 사용자의 이름, 이메일, 전화번호 등 회원 정보 수정
- Axios를 통해 회원 수정 REST API 호출
- 수정 후 현재 사용자 정보를 다시 조회하여 화면 상태 갱신

관련 화면 및 코드:

- `frontend/src/views/MyPageView.vue`
- `frontend/src/api/auth.js`
- `backend/src/main/java/com/ssafy/prj/controller/api/MemberApiController.java`

<br>

### 🗑️ 회원 삭제

구현 내용:

- 마이페이지에서 회원 탈퇴 기능을 Vue 화면으로 이전
- 관리자 화면에서 회원 삭제 기능 제공
- 삭제 전 확인 모달을 표시해 실수 삭제 방지
- 삭제 후 로그인 상태와 화면 목록 갱신

관련 화면 및 코드:

- `frontend/src/views/MyPageView.vue`
- `frontend/src/views/AdminUsersView.vue`
- `frontend/src/components/ConfirmModal.vue`
- `frontend/src/api/admin.js`

<br>

### 🔐 로그인과 인증

구현 내용:

- 기존 JSP 로그인/로그아웃 화면과 처리를 Vue + REST API 방식으로 이전
- `POST /api/auth/login`으로 JSON 로그인 요청
- Spring Security 인증 성공 시 JWT Access Token과 Refresh Token 발급
- 프론트엔드는 Access Token을 `Authorization: Bearer ...` 헤더에 담아 보호 API 호출
- Refresh Token은 HttpOnly 쿠키로 내려 로그인 연장 요청(`/api/auth/refresh`)에 사용
- `GET /api/auth/me`로 현재 로그인 사용자 정보를 조회
- `POST /api/auth/logout`으로 보안 컨텍스트를 비우고 Refresh Token 쿠키 만료
- 인증 실패와 권한 없음 응답은 HTML redirect가 아닌 JSON으로 반환

관련 화면 및 코드:

- `frontend/src/views/LoginView.vue`
- `frontend/src/api/auth.js`
- `backend/src/main/java/com/ssafy/prj/controller/api/AuthApiController.java`
- `backend/src/main/java/com/ssafy/prj/security/config/CustomSecurityConfig.java`
- `backend/src/main/java/com/ssafy/prj/security/jwt/JwtTokenProvider.java`
- `backend/src/main/java/com/ssafy/prj/security/jwt/JwtAuthenticationFilter.java`

<br>

### 🏢 주택 정보 관리

구현 내용:

- 기존 JSP 주택 정보 목록/상세/수정 화면을 Vue로 이전
- 주택 기본 정보와 최근 거래 내역을 상세 화면에서 제공
- Kakao 지도에 주택 위치 표시
- 관리자 권한으로 주택명, 건축연도, 위도, 경도 수정
- 백엔드는 주택 정보 관리 API를 JSON으로 제공

관련 화면 및 코드:

- `frontend/src/views/HouseListView.vue`
- `frontend/src/views/HouseDetailView.vue`
- `frontend/src/views/HouseFormView.vue`
- `frontend/src/components/KakaoMap.vue`
- `frontend/src/api/houses.js`

<br>

### 💬 공유게시판

구현 내용:

- 기존 JSP 공유게시판 목록/상세/등록/수정 화면을 Vue로 이전
- 게시글 목록 조회와 키워드 검색 제공
- 게시글 상세 조회 시 조회수 증가
- 로그인 사용자는 게시글 등록 가능
- 작성자 또는 관리자는 게시글 수정/삭제 가능
- 기존 form submit 방식에서 Axios 기반 REST 요청 방식으로 전환

관련 화면 및 코드:

- `frontend/src/views/BoardListView.vue`
- `frontend/src/views/BoardDetailView.vue`
- `frontend/src/views/BoardFormView.vue`
- `frontend/src/components/ArticleList.vue`
- `frontend/src/components/ArticleForm.vue`
- `frontend/src/api/boards.js`

<br>

## 🚀 실행 방법

### ✅ 사전 요구사항

| 항목 | 버전 | 비고 |
|------|------|------|
| Java | 21 | |
| Maven | 3.9+ | `./mvnw` 래퍼 포함 |
| Node.js | 18+ | |
| pnpm | 8+ | `npm install -g pnpm` |
| MySQL | 8.x | |
| Redis | 7+ | 챗봇 세션 저장용. Docker 사용 가능 |

Redis 빠른 실행 (Docker):

```bash
docker run -d --name redis -p 6379:6379 redis:7
```

<br>

### 1. 🔑 환경 변수 설정

`backend/application-secret.properties` 파일을 생성하고 아래 값을 채웁니다.

```properties
# Kakao Developers > 내 애플리케이션 > 앱 키 > REST API 키
KAKAO_REST_API_KEY=발급받은_키

# SSAFY GMS 또는 OpenAI API 키 (AI 챗봇, 동네 요약에 사용)
AI_API_KEY=발급받은_키

# 공공데이터포털 아파트 실거래가 API 키 (배치 수집용, 선택)
ESTATE_API_KEY=발급받은_키

# JWT 서명 비밀키 (임의의 긴 문자열)
JWT_SECRET=여기에_64자_이상의_랜덤_문자열_입력
```

> `AI_API_KEY`와 `KAKAO_REST_API_KEY`가 없으면 AI 챗봇과 동네 요약 기능이 동작하지 않습니다.

<br>

### 2. 🗄️ DB 스키마 및 기본 데이터 적재

MySQL에 `ssafy_home` 데이터베이스를 먼저 생성합니다.

```sql
CREATE DATABASE ssafy_home CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'ssafy_home'@'localhost' IDENTIFIED BY 'ssafy_home';
GRANT ALL PRIVILEGES ON ssafy_home.* TO 'ssafy_home'@'localhost';
```

아래 SQL 파일을 **순서대로** 실행합니다.

```powershell
# ① 전체 테이블 스키마 생성
mysql -u ssafy_home -p --default-character-set=utf8mb4 ssafy_home < sql\SSAFY_HOME_Schema.sql

# ② 법정동 코드
# load_dongcodes.sql 또는 dongcodes_from_dump.sql 중 하나만 실행합니다.
mysql -u ssafy_home -p --default-character-set=utf8mb4 ssafy_home < sql\load_dongcodes.sql

# ③ 전월세 실거래 테이블
mysql -u ssafy_home -p --default-character-set=utf8mb4 ssafy_home < sql\houserents.sql

# ④ 회원 · 관심지역 테이블
mysql -u ssafy_home -p --default-character-set=utf8mb4 ssafy_home < sql\users_and_favorites.sql

# ⑤ 공지사항 테이블
mysql -u ssafy_home -p --default-character-set=utf8mb4 ssafy_home < sql\notices.sql

# ⑥ 자유게시판 테이블
mysql -u ssafy_home -p --default-character-set=utf8mb4 ssafy_home < sql\boards.sql

# ⑦ 동네 추천용 - 공원 테이블
mysql -u ssafy_home -p --default-character-set=utf8mb4 ssafy_home < sql\parks.sql

# ⑧ 동네 추천용 - 학교 테이블
mysql -u ssafy_home -p --default-character-set=utf8mb4 ssafy_home < sql\schools.sql

# ⑨ 동네 추천용 - 지하철역 테이블
mysql -u ssafy_home -p --default-character-set=utf8mb4 ssafy_home < sql\subway_stations.sql

# ⑩ 동네 추천용 - 상권 집계 테이블
mysql -u ssafy_home -p --default-character-set=utf8mb4 ssafy_home < sql\dong_commercial_metrics.sql

# ⑪ 동네 추천용 - 버스 정류소 집계 테이블
mysql -u ssafy_home -p --default-character-set=utf8mb4 ssafy_home < sql\dong_bus_stop_metrics.sql

# ⑫ 매매 실거래 배치 리포트 테이블
mysql -u ssafy_home -p --default-character-set=utf8mb4 ssafy_home < sql\batch_estate_market.sql
```

> `SSAFY_HOME_Dump_2604.sql`은 원본 대용량 덤프입니다. 현재 프로젝트 실행 절차에서는 직접 실행하지 않고,
> 필요한 법정동 데이터만 분리한 `load_dongcodes.sql` 또는 `dongcodes_from_dump.sql`를 사용합니다.

각 테이블이 정상 생성되었는지 확인합니다.

```sql
USE ssafy_home;
SHOW TABLES;
```

<br>

### 3. 🏙️ 동네 추천 인프라 데이터 적재

동네 추천 점수 계산에 필요한 데이터는 **파일 적재 데이터**와 **배치 수집 데이터**로 나뉩니다.

#### 데이터 수집 방식 요약

| 데이터 | 적재 방식 | 사용 테이블 |
| --- | --- | --- |
| 학교 | 파일 기반 CSV 변환 후 적재 | `schools` |
| 공원 | 파일 기반 CSV 변환 후 적재 | `parks` |
| 지하철 | 파일 기반 CSV 변환 후 적재 | `subway_stations` |
| 상권·생활시설 | 공공데이터 API 배치 수집 | `dong_commercial_metrics` |
| 버스 정류장 | 서울시 API 배치 수집 + 좌표→법정동 캐시 | `dong_bus_stop_metrics`, `bus_stop_dong_cache` |
| 매매 실거래 | 공공데이터 API 배치 수집 | `houseinfos`, `housedeals`, 배치 리포트 테이블 |
| 전월세 실거래 | 공공데이터 API 배치 수집 | `houserents` |

파일이 필요한 데이터는 학교·공원·지하철 3종입니다. 나머지는 Spring Batch가 API를 호출해 수집합니다.

#### 학교 데이터

전국 학교 위치 표준데이터 CSV를 `schools` 적재용 CSV로 변환합니다.

```powershell
# 1차 주소 매칭
powershell -ExecutionPolicy Bypass -File scripts\convert_national_schools_for_import.ps1

# 주소만으로 법정동 코드가 확정되지 않은 학교를 카카오 역지오코딩으로 보정할 때만 실행
powershell -ExecutionPolicy Bypass -File scripts\recover_national_school_dong_codes_with_kakao.ps1
```

변환된 CSV는 `sql/schools.sql`의 `LOAD DATA LOCAL INFILE` 경로와 맞춰 적재합니다.

```powershell
mysql -u ssafy_home -p --local-infile=1 --default-character-set=utf8mb4 ssafy_home < sql\schools.sql
```

#### 공원 데이터

전국 도시공원 표준데이터 CSV를 `parks` 적재용 CSV로 변환합니다.

```powershell
python scripts\prepare_parks.py
mysql -u ssafy_home -p --local-infile=1 --default-character-set=utf8mb4 ssafy_home < sql\load_parks.sql
```

#### 지하철역 데이터

전국 도시·광역철도 역사 파일을 서울 지하철 추천 점수용 CSV로 변환합니다.

```powershell
python scripts\prepare_subway_stations.py
```

`prepare_subway_stations.py`는 변환 후 `subway_stations` 테이블에 데이터를 적재합니다.

#### 적재 확인

```sql
SELECT COUNT(*) FROM dongcodes;
SELECT COUNT(*) FROM schools;
SELECT COUNT(*) FROM parks;
SELECT COUNT(*) FROM subway_stations;
```

<br>

### 4. 🔁 배치 데이터 수집

상권·버스·실거래처럼 변동성이 큰 데이터는 파일을 직접 넣지 않고 Spring Batch로 수집합니다.

#### API 키 설정

`backend/src/main/resources/application-secret.properties` 또는 환경변수에 API 키를 설정합니다.

```properties
# 공공데이터포털 실거래/상권 API
ESTATE_API_KEY=발급받은_공공데이터포털_키
COMMERCIAL_API_KEY=발급받은_상권_API_키

# 서울시 버스 정류장 API
SEOUL_OPEN_API_KEY=발급받은_서울시_OPEN_API_키

# 좌표를 법정동으로 변환할 때 사용
KAKAO_REST_API_KEY=발급받은_카카오_REST_API_키

# AI/RAG 요약용 GPT API
AI_API_KEY=발급받은_GPT_API_키
```

#### 상권·생활시설 배치

상권 원본 점포 전체를 저장하지 않고, 법정동별 통계만 `dong_commercial_metrics`에 저장합니다.

수집 대상:

- 카페·외식
- 운동·문화
- 장보기·생활편의
- 의료
- 반려용품
- 동물병원

#### 버스 정류장 배치

서울시 버스 정류장 API에서 정류장 좌표를 가져오고, 카카오 API로 좌표를 법정동 코드로 변환합니다.
한 번 변환한 좌표는 `bus_stop_dong_cache`에 저장해 다음 배치부터 재사용합니다.

#### 실거래 배치

매매와 전월세 실거래는 공공데이터 API를 통해 수집합니다. 서울시 전체 수집 시에는 배치 설정의 법정동 코드 범위를 비워두거나 서울시 시군구 코드 목록을 지정합니다.

#### 배치 실행 방법

배치는 `backend/src/main/resources/application.properties`의 설정값을 바꾼 뒤 백엔드를 실행하면 시작됩니다.
처음 데이터를 넣을 때는 아래 순서대로 진행합니다.

##### 1) 상권·생활시설 배치 첫 실행

```properties
# backend/src/main/resources/application.properties
batch.commercial.enabled=true
batch.commercial.run-on-startup=true
```

백엔드를 실행하면 소상공인시장진흥공단 API를 호출해 `dong_commercial_metrics`에 법정동별 통계를 저장합니다.

실행이 끝난 뒤에는 반복 실행을 막기 위해 아래처럼 되돌립니다.

```properties
batch.commercial.enabled=true
batch.commercial.run-on-startup=false
```

##### 2) 버스 정류장 배치 첫 실행

```properties
# backend/src/main/resources/application.properties
batch.bus.enabled=true
batch.bus.run-on-startup=true
```

백엔드를 실행하면 서울시 버스 정류장 API를 호출하고, 카카오 좌표→법정동 변환 결과를 캐시에 저장합니다.
최초 실행은 좌표 변환이 포함되어 시간이 오래 걸릴 수 있습니다.

실행이 끝난 뒤에는 아래처럼 되돌립니다.

```properties
batch.bus.enabled=true
batch.bus.run-on-startup=false
```

##### 3) 전월세 실거래 배치 첫 실행

```properties
# backend/src/main/resources/application.properties
batch.estate-rent.enabled=true
batch.estate-rent.run-on-startup=true
```

수집 범위는 아래 설정으로 조정합니다.

```properties
# 특정 구만 수집할 때
batch.estate-rent.lawd-codes=11110,11140

# 서울시 전체 수집 예시: lawd-codes를 주석 처리하고 sido-name을 사용합니다.
# batch.estate-rent.lawd-codes=
# batch.estate-rent.sido-name=서울특별시
```

실행이 끝난 뒤에는 아래처럼 되돌립니다.

```properties
batch.estate-rent.enabled=true
batch.estate-rent.run-on-startup=false
```

##### 4) 매매 실거래 배치

매매 실거래 배치는 `batch.estate.enabled`와 스케줄러를 사용합니다.

```properties
# backend/src/main/resources/application.properties
batch.estate.enabled=true
```

수집 범위는 아래 설정으로 조정합니다.

```properties
# 특정 구만 수집할 때
batch.estate.lawd-codes=11110,11140

# 서울시 전체 수집 예시: lawd-codes를 주석 처리하고 sido-name을 사용합니다.
# batch.estate.lawd-codes=
# batch.estate.sido-name=서울특별시
```

##### 5) 백엔드 실행으로 배치 시작

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

콘솔에 배치 시작/완료 로그가 출력됩니다. `run-on-startup=true` 상태로 계속 두면 서버를 다시 켤 때마다 같은 배치가 반복 실행될 수 있으므로,
최초 적재나 재수집이 끝난 뒤에는 반드시 `false`로 되돌리는 것을 권장합니다.

#### 배치 결과 확인

```sql
SELECT category, COUNT(*), SUM(business_count)
FROM dong_commercial_metrics
GROUP BY category;

SELECT COUNT(*) FROM dong_bus_stop_metrics;
SELECT COUNT(*) FROM bus_stop_dong_cache;
SELECT COUNT(*) FROM housedeals;
SELECT COUNT(*) FROM houserents;
```

<br>

### 5. ☕ 백엔드 실행

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

| 항목 | 주소 |
|------|------|
| API 서버 | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

<br>

### 6. 🎨 프론트엔드 실행

```powershell
cd frontend
pnpm install
pnpm dev
```

| 항목 | 주소 |
|------|------|
| 웹 화면 | http://localhost:5173 |

<br>

### 📚 테이블별 역할 요약

| 테이블 | 설명 | 필수 여부 |
|--------|------|-----------|
| `dongcodes` | 법정동 코드 마스터 | ✅ 필수 |
| `houseinfos` | 아파트 기본 정보 | ✅ 필수 |
| `housedeals` | 매매 실거래가 | ✅ 필수 |
| `houserents` | 전월세 실거래가 | ✅ 필수 |
| `users` | 회원 정보 | ✅ 필수 |
| `favorite_areas` | 관심 지역 | ✅ 필수 |
| `boards` | 자유게시판 | ✅ 필수 |
| `notices` | 공지사항 | ✅ 필수 |
| `parks` | 공원 정보 | 동네 추천 사용 시 필수 |
| `schools` | 학교 위치 | 동네 추천 사용 시 필수 |
| `subway_stations` | 지하철역 위치 | 동네 추천 사용 시 필수 |
| `dong_commercial_metrics` | 법정동별 상권 집계 | 동네 추천 사용 시 필수 |
| `dong_bus_stop_metrics` | 법정동별 버스 정류소 수 | 동네 추천 사용 시 필수 |
| `bus_stop_dong_cache` | 버스 정류소 좌표 캐시 | 자동 생성 |
| `batch_estate_market_*` | 배치 리포트 | 배치 기능 사용 시 |

<br>

## 🧷 개발 시 주의사항

### 🔐 JWT 인증

Vue와 Spring Boot를 분리해서 실행하므로 프론트는 `localhost:5173`, 백엔드는 `localhost:8080`에서 동작합니다.
Access Token은 요청 헤더로 전달하고, Refresh Token은 HttpOnly 쿠키로 관리합니다.

- 백엔드 CORS에서 `http://localhost:5173` 허용
- Axios 요청에는 Access Token을 `Authorization: Bearer ...` 형태로 첨부
- Refresh Token 쿠키를 주고받는 인증 API는 `withCredentials: true` 사용
- 로그인 후 `/api/auth/me`로 현재 사용자 조회 확인

### 🛣️ API 경로

프론트에서 호출하는 백엔드 API는 `/api` prefix를 사용합니다.

```text
/api/auth
/api/boards
/api/notices
/api/houses
/api/neighborhood
```

이는 Vue 화면 라우트(`/boards`, `/login`, `/houses`)와 백엔드 데이터 API(`/api/boards`, `/api/auth/login`, `/api/houses`)를 구분하기 위한 구조입니다.

### 🗺️ Kakao Map

카카오 지도 SDK를 사용하려면 Kakao Developers의 JavaScript 키와 Web 플랫폼 도메인 등록이 필요합니다.

개발 중에는 아래 도메인을 등록합니다.

```text
http://localhost:5173
http://127.0.0.1:5173
```
