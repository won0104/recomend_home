-- =====================================================
-- schools 테이블 생성 및 데이터 적재 스크립트
-- 전국 초중등학교 위치 표준 데이터
-- =====================================================

-- 1. 기존 테이블 삭제 (필요시)
DROP TABLE IF EXISTS `ssafy_home`.`schools` ;

-- 2. schools 테이블 생성
CREATE TABLE IF NOT EXISTS `ssafy_home`.`schools` (
  `school_id` VARCHAR(20) NOT NULL COMMENT '학교 고유 ID',
  `school_name` VARCHAR(100) NOT NULL COMMENT '학교명',
  `sido` VARCHAR(30) NOT NULL COMMENT '시도명',
  `sigungu` VARCHAR(30) NOT NULL COMMENT '시군구명',
  `lawd_cd` BIGINT NOT NULL COMMENT '법정동코드',
  `dong_name` VARCHAR(50) NULL COMMENT '법정동명',
  `school_type` VARCHAR(20) NOT NULL COMMENT '학교급구분 (초등학교, 중학교, 고등학교)',
  `establishment` VARCHAR(20) NOT NULL COMMENT '설립유형 (공립, 사립, 국립)',
  `facility_type` VARCHAR(20) NOT NULL COMMENT '시설구분 (본교, 분교)',
  `operation_status` VARCHAR(20) NOT NULL COMMENT '운영상태 (운영, 폐교, 휴교)',
  `latitude` DOUBLE NOT NULL COMMENT '위도',
  `longitude` DOUBLE NOT NULL COMMENT '경도',
  `created_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
  PRIMARY KEY (`school_id`),
  KEY `idx_lawd_cd` (`lawd_cd`),
  KEY `idx_sido_sigungu` (`sido`, `sigungu`),
  KEY `idx_school_type` (`school_type`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
COMMENT = '전국 초중등학교 위치 데이터';

-- 3. 인덱스 추가 (조회 성능 최적화)
CREATE INDEX idx_schools_lawd_cd ON `ssafy_home`.`schools`(`lawd_cd`);
CREATE INDEX idx_schools_sido_sigungu ON `ssafy_home`.`schools`(`sido`, `sigungu`);

-- 4. 데이터 적재 (CSV 파일에서)
-- 실행 방법:
-- LOAD DATA LOCAL INFILE '/경로/schools.csv'
-- INTO TABLE ssafy_home.schools
-- FIELDS TERMINATED BY ','
-- IGNORE 1 ROWS
-- (school_id, school_name, sido, sigungu, lawd_cd, dong_name, school_type, establishment, 
--  facility_type, operation_status, latitude, longitude);

-- 또는 테스트용 샘플 데이터 (선택사항)
-- INSERT INTO `ssafy_home`.`schools` VALUES
-- ('B000002843', '대구옥산초등학교', '대구광역시', '북구', 2723000000, '', '초등학교', '공립', '본교', '운영', 35.8808955, 128.5978756),
-- ('B000008555', '부곡여자중학교', '부산광역시', '금정구', 2641000000, '', '중학교', '공립', '본교', '운영', 35.22332989, 129.0953419);
