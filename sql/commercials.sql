-- -----------------------------------------------------
-- SSAFY HOME : 상가(상권) 정보 스키마 (F604, F605)
-- 출처 : 소상공인시장진흥공단 상가(상권)정보 (UTF-8 CSV, 17개 시도)
-- 기준 DB : `ssafy_home`
-- -----------------------------------------------------

USE `ssafy_home`;

DROP TABLE IF EXISTS `ssafy_home`.`commercials`;

CREATE TABLE IF NOT EXISTS `ssafy_home`.`commercials` (
  `shop_no`           VARCHAR(20)    NOT NULL                 COMMENT '상가업소번호',
  `shop_name`         VARCHAR(200)   NOT NULL                 COMMENT '상호명',
  `branch_name`       VARCHAR(200)   NULL                     COMMENT '지점명',
  `category_l_code`   VARCHAR(10)    NULL                     COMMENT '상권업종 대분류 코드',
  `category_l_name`   VARCHAR(50)    NULL                     COMMENT '상권업종 대분류명',
  `category_m_code`   VARCHAR(10)    NULL                     COMMENT '상권업종 중분류 코드',
  `category_m_name`   VARCHAR(100)   NULL                     COMMENT '상권업종 중분류명',
  `category_s_code`   VARCHAR(20)    NULL                     COMMENT '상권업종 소분류 코드',
  `category_s_name`   VARCHAR(100)   NULL                     COMMENT '상권업종 소분류명',
  `std_industry_code` VARCHAR(20)    NULL                     COMMENT '표준산업분류코드',
  `std_industry_name` VARCHAR(100)   NULL                     COMMENT '표준산업분류명',
  `sido_code`         VARCHAR(10)    NULL                     COMMENT '시도코드 (2자리)',
  `sido_name`         VARCHAR(30)    NULL                     COMMENT '시도명',
  `sigungu_code`      VARCHAR(10)    NULL                     COMMENT '시군구코드 (5자리, LAWD_CD 매칭)',
  `sigungu_name`      VARCHAR(30)    NULL                     COMMENT '시군구명',
  `haengjeong_code`   VARCHAR(20)    NULL                     COMMENT '행정동코드',
  `haengjeong_name`   VARCHAR(50)    NULL                     COMMENT '행정동명',
  `beopjeong_code`    VARCHAR(20)    NULL                     COMMENT '법정동코드 (10자리)',
  `beopjeong_name`    VARCHAR(50)    NULL                     COMMENT '법정동명',
  `jibun_address`     VARCHAR(255)   NULL                     COMMENT '지번주소',
  `road_address`      VARCHAR(255)   NULL                     COMMENT '도로명주소',
  `building_name`     VARCHAR(100)   NULL                     COMMENT '건물명',
  `longitude`         DECIMAL(13,10) NULL                     COMMENT '경도',
  `latitude`          DECIMAL(13,10) NULL                     COMMENT '위도',
  PRIMARY KEY (`shop_no`),
  INDEX `idx_commercials_sigungu`        (`sigungu_code` ASC) VISIBLE,
  INDEX `idx_commercials_beopjeong_code` (`beopjeong_code` ASC) VISIBLE,
  INDEX `idx_commercials_beopjeong_name` (`beopjeong_name` ASC) VISIBLE,
  INDEX `idx_commercials_category_l`     (`category_l_code` ASC) VISIBLE,
  INDEX `idx_commercials_sigungu_cat`    (`sigungu_code`, `category_l_code`) VISIBLE,
  INDEX `idx_commercials_shop_name`      (`shop_name` ASC) VISIBLE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '소상공인 상가(상권)정보';

-- -----------------------------------------------------
-- (선택) 대분류 카테고리 마스터 — UI 콤보박스 등에 활용
-- -----------------------------------------------------
DROP TABLE IF EXISTS `ssafy_home`.`commercial_categories`;

CREATE TABLE IF NOT EXISTS `ssafy_home`.`commercial_categories` (
  `category_code` VARCHAR(10) NOT NULL COMMENT '대분류 코드 (예: L1, F1)',
  `category_name` VARCHAR(50) NOT NULL COMMENT '대분류명',
  PRIMARY KEY (`category_code`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '상권업종 대분류 마스터';
