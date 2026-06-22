-- =====================================================
-- 추천 점수 계산용 도시공원 테이블
-- 실행 전: dongcodes 테이블이 먼저 생성·적재되어 있어야 한다.
-- 주의: 아래 DROP TABLE은 기존 parks 데이터를 삭제한다.
-- =====================================================

USE `ssafy_home`;

DROP TABLE IF EXISTS `parks`;

CREATE TABLE `parks` (
  `park_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '공원 내부 식별자',
  `park_name` VARCHAR(200) NOT NULL COMMENT '공원명',
  `road_address` VARCHAR(500) NULL COMMENT '소재지 도로명주소',
  `latitude` DECIMAL(10, 8) NOT NULL COMMENT '위도',
  `longitude` DECIMAL(11, 8) NOT NULL COMMENT '경도',
  `park_area` DECIMAL(18, 2) NULL COMMENT '공원면적(㎡)',
  `dong_code` VARCHAR(10) NULL COMMENT '법정동코드',
  PRIMARY KEY (`park_id`),
  KEY `idx_parks_dong_code` (`dong_code`),
  KEY `idx_parks_location` (`latitude`, `longitude`),
  CONSTRAINT `fk_parks_dong_code`
    FOREIGN KEY (`dong_code`) REFERENCES `dongcodes` (`dong_code`)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '추천 점수 계산용 도시공원 정보';

-- 시도·구군·동 이름은 dongcodes에 있으므로 parks에 중복 저장하지 않는다.
