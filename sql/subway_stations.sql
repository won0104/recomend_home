-- =====================================================
-- 동네 추천용 서울 도시·광역철도 역 테이블
-- 실행 전: dongcodes 테이블이 먼저 생성·적재되어 있어야 한다.
-- 주의: 아래 DROP TABLE은 기존 subway_stations 데이터를 삭제한다.
-- =====================================================

USE `ssafy_home`;

DROP TABLE IF EXISTS `subway_stations`;

CREATE TABLE `subway_stations` (
  `station_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '역 내부 식별자',
  `line_name` VARCHAR(100) NOT NULL COMMENT '운영노선명',
  `station_no` VARCHAR(20) NOT NULL COMMENT '노선별 역번호',
  `station_name` VARCHAR(150) NOT NULL COMMENT '역명(한글)',
  `latitude` DECIMAL(10, 8) NOT NULL COMMENT '위도(WGS84)',
  `longitude` DECIMAL(11, 8) NOT NULL COMMENT '경도(WGS84)',
  `dong_code` VARCHAR(10) NOT NULL COMMENT '법정동코드',
  PRIMARY KEY (`station_id`),
  UNIQUE KEY `uk_subway_stations_line_station_no` (`line_name`, `station_no`),
  KEY `idx_subway_stations_dong_code` (`dong_code`),
  KEY `idx_subway_stations_location` (`latitude`, `longitude`),
  CONSTRAINT `fk_subway_stations_dong_code`
    FOREIGN KEY (`dong_code`) REFERENCES `dongcodes` (`dong_code`)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '동네 추천 교통 점수용 서울 도시·광역철도 역 정보';

-- 같은 환승역이라도 운영노선과 역번호가 다르면 각각 보존한다.
-- 이후 동별 교통 점수에서는 필요에 따라 역 수 또는 노선 수 기준으로 집계할 수 있다.
