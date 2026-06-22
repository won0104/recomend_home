# SSAFY HOME - Spring Batch 부동산 실거래 데이터 자동화

공공데이터포털의 아파트 매매 실거래 데이터를 Spring Batch로 주기적으로 수집하고, 수집 데이터를 서비스 DB에 저장한 뒤 생성형 AI 요약과 PDF 리포트를 자동 생성하는 프로젝트입니다.

이번 구현의 핵심은 기존 정적 덤프 기반 조회 서비스에 *배치 수집/가공/저장/AI 요약/PDF 생성/스케줄 실행* 흐름을 추가한 것입니다.

<br>

## 프로젝트 구조

```text
pjt10_buk03_15_09
├─ README.md
├─ backend/
│  ├─ pom.xml
│  └─ src/main/
│     ├─ java/com/ssafy/prj/
│     │  ├─ PjtApplication.java
│     │  ├─ batch/
│     │  │  ├─ config/EstateMarketBatchConfig.java
│     │  │  ├─ scheduler/EstateMarketBatchScheduler.java
│     │  │  ├─ tasklet/
│     │  │  │  ├─ EstateMarketCollectTasklet.java
│     │  │  │  ├─ EstateMarketSaveTasklet.java
│     │  │  │  ├─ EstateMarketAiTasklet.java
│     │  │  │  └─ EstateMarketPdfTasklet.java
│     │  │  ├─ ai/EstateMarketAiService.java
│     │  │  ├─ pdf/EstateMarketPdfService.java
│     │  │  ├─ dao/EstateMarketBatchDao.java
│     │  │  └─ domain/
│     │  │     ├─ EstateMarketItem.java
│     │  │     └─ EstateMarketReport.java
│     │  ├─ controller/api/
│     │  ├─ model/
│     │  └─ security/
│     └─ resources/
│        ├─ application.properties
│        └─ mappers/EstateMarketBatchMapper.xml
├─ sql/
│  ├─ SSAFY_HOME_Schema.sql
│  ├─ SSAFY_HOME_Dump_2604.sql
│  └─ batch_estate_market.sql
└─ images/
```

<br>

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.x |
| Batch | Spring Batch, Tasklet Step, JobLauncher |
| Scheduler | Spring `@Scheduled`, `@EnableScheduling` |
| Database | MySQL 8.x, MyBatis XML Mapper |
| AI | OpenAI 호환 Chat Completions API |
| PDF | openhtmltopdf, Malgun Gothic 폰트 임베딩 |
| API 문서 | springdoc-openapi / Swagger UI |

<br>

## 배치 처리 흐름

```text
앱 시작 또는 스케줄 시간 도달
→ estateMarketBatchJob 실행
→ F901 데이터 수집
→ F902 데이터 가공 및 DB 저장
→ F903 AI 요약 및 DB 업데이트
→ F904 PDF 리포트 생성
→ F905 다음 스케줄에 자동 반복 실행
```

전체 Job 구성:

```text
estateMarketCollectStep
→ estateMarketSaveStep
→ estateMarketAiStep
→ estateMarketPdfStep
```

관련 코드:

- `backend/src/main/java/com/ssafy/prj/batch/config/EstateMarketBatchConfig.java`
- `backend/src/main/java/com/ssafy/prj/batch/tasklet/*.java`

<br>

## 구현 기능

### F901. Batch를 이용한 데이터 수집

공공데이터포털 아파트 매매 실거래 API에서 부동산 거래 데이터를 수집합니다. 데이터 형식은 XML이며, `LAWD_CD`, `DEAL_YMD`, `pageNo`, `numOfRows` 조건으로 조회합니다.

주요 구현:

- Spring Batch `Job` 정의
- 외부 API 호출
- 지역코드 반복 지원
- 거래월 반복 지원
- 페이지 번호 반복 지원
- API 실패 시 로컬 DB fallback 처리

관련 코드:

- `EstateMarketBatchConfig.java`
- `EstateMarketCollectTasklet.java`

현재 테스트 설정:

```properties
batch.estate.lawd-codes=11110
batch.estate.deal-ymd=
batch.estate.months=1
batch.estate.page-size=100
batch.estate.max-pages=2
batch.estate.max-items=200
batch.estate.force-api=true
```

설정 의미:

- `lawd-codes=11110`: 서울 종로구만 테스트 수집
- `deal-ymd=`: 비워두면 최근 거래월 자동 계산
- `months=1`: 최근 1개월
- `page-size=100`: API 1회 호출당 100건 요청
- `max-pages=2`: 최대 2페이지까지 조회
- `max-items=200`: 최대 200건까지만 수집


### F902. 수집 데이터 가공 및 DB 저장

외부 API 응답을 그대로 저장하지 않고 서비스에서 사용할 수 있는 구조로 정제합니다.

가공 내용:

- 불필요한 XML 필드 제거
- 아파트명, 법정동, 지번, 거래금액, 전용면적, 층, 건축년도, 거래일만 추출
- 거래월과 지역코드 보정
- 서비스 조회 테이블에 저장 가능한 구조로 변환
- 중복 거래는 `housedeals`에 재삽입하지 않도록 체크

저장 테이블:

- `batch_estate_market_reports`: 배치 실행 단위 리포트 메타 정보
- `batch_estate_market_items`: 수집된 거래 데이터 원장
- `houseinfos`: 사용자가 조회하는 주택 정보 테이블
- `housedeals`: 사용자가 조회하는 거래 정보 테이블

관련 코드:

- `EstateMarketCollectTasklet.java`
- `EstateMarketSaveTasklet.java`
- `EstateMarketBatchDao.java`
- `EstateMarketBatchMapper.xml`

### F903. 생성형 AI 요약 및 예외 처리

수집된 거래 데이터를 생성형 AI API에 전달하여 부동산 시장 요약 리포트를 생성합니다.

구현 내용:

- OpenAI 호환 Chat Completions API 호출
- 거래 건수, 평균 거래금액, 최고/최저 거래금액, 주요 지역 흐름 요약
- AI 호출 실패 시 fallback 요약문 생성
- 요약 결과와 상태를 DB에 업데이트

저장 컬럼:

- `batch_estate_market_reports.ai_summary`
- `batch_estate_market_reports.ai_status`

관련 코드:

- `EstateMarketAiTasklet.java`
- `EstateMarketAiService.java`
- `EstateMarketBatchMapper.xml`


### F904. PDF 자동 생성

AI 요약 결과를 PDF 문서로 자동 생성합니다.

PDF 구성:

- 리포트 제목 및 메타 정보
- 거래 건수/평균가/최고가/최저가 요약 카드
- 법정동별 거래량 비교 막대그래프
- AI 요약 본문
- 정제 거래 데이터 표

저장 위치:

```text
C:\Users\SSAFY\ssafy-home-reports
```

관련 코드:

- `EstateMarketPdfTasklet.java`
- `EstateMarketPdfService.java`

### F905. Scheduler를 활용한 주기적 실행

사용자 요청 없이 정해진 시간에 배치 Job이 자동 실행됩니다.

구현 내용:

- `@EnableScheduling`으로 스케줄러 활성화
- `@Scheduled`로 평일 오전 8시 자동 실행
- 테스트 편의를 위해 애플리케이션 시작 시 1회 즉시 실행

관련 코드:

- `PjtApplication.java`
- `EstateMarketBatchScheduler.java`

설정:

```properties
batch.estate.enabled=true
batch.estate.cron=0 0 8 * * MON-FRI
```

로그 예:

```text
[EstateBatch-Scheduler] estateMarketBatchJob start. trigger=startup
[EstateBatch-Scheduler] estateMarketBatchJob start. trigger=scheduler
```

<br>

## 실행 방법

### 1. DB 준비

MySQL에 `ssafy_home` 스키마와 기본 데이터를 적재합니다.

```powershell
mysql -u ssafy -p --default-character-set=utf8mb4 ssafy_home < sql\SSAFY_HOME_Schema.sql
mysql -u ssafy -p --default-character-set=utf8mb4 ssafy_home < sql\SSAFY_HOME_Dump_2604.sql
mysql -u ssafy -p --default-character-set=utf8mb4 ssafy_home < sql\users_and_favorites.sql
mysql -u ssafy -p --default-character-set=utf8mb4 ssafy_home < sql\notices.sql
mysql -u ssafy -p --default-character-set=utf8mb4 ssafy_home < sql\boards.sql
mysql -u ssafy -p --default-character-set=utf8mb4 ssafy_home < sql\commercials.sql
```

배치 리포트 테이블은 실행 시 자동 생성됩니다. 별도 SQL로 만들고 싶다면 다음 파일을 사용할 수 있습니다.

```powershell
mysql -u ssafy -p --default-character-set=utf8mb4 ssafy_home < sql\batch_estate_market.sql
```

### 2. API Key 설정

`backend/src/main/resources/application.properties`에 키를 설정합니다.

- 공공데이터포털 부동산 실거래 API
- AI API


### 3. 서버 실행

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

앱이 시작되면 테스트 편의를 위해 배치가 한 번 자동 실행됩니다.

정상 로그:

```text
[EstateBatch-Collect] reportId=..., items=...
[EstateBatch-Save] service tables updated. houseinfos=..., new housedeals=...
[EstateBatch-AI] summary updated. reportId=...
[EstateBatch-PDF] created: C:\Users\SSAFY\ssafy-home-reports\ESTATE-....pdf
Job ... status: [COMPLETED]
```

<br>

## Swagger UI

```text
http://localhost:8080/swagger-ui.html
```

<br>

## DB 확인 쿼리

최신 배치 리포트:

```sql
SELECT report_id, item_count, ai_status, LEFT(ai_summary, 300) AS summary_preview, pdf_path, created_at
FROM batch_estate_market_reports
ORDER BY created_at DESC
LIMIT 1;
```

수집 원장:

```sql
SELECT report_id, source_type, lawd_cd, deal_ymd, COUNT(*) AS cnt
FROM batch_estate_market_items
GROUP BY report_id, source_type, lawd_cd, deal_ymd
ORDER BY report_id DESC;
```

서비스 테이블 반영:

```sql
SELECT COUNT(*) FROM houseinfos WHERE apt_seq LIKE 'BT%';
SELECT COUNT(*) FROM housedeals WHERE apt_seq LIKE 'BT%';
```
