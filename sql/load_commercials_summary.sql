-- -----------------------------------------------------
-- 상가(상권) 경량(샘플링) 데이터 적재
-- 원본 :   data/소상공인시장진흥공단_상가(상권)정보_20260331/*.csv  (200만+ 행)
-- 경량 :   data/summary/commercials_<region>.csv  (전부 영문 파일명)
--          → scripts/sample_commercials.py 로 미리 생성
--
-- 인덱스 6개는 그대로 유지 (commercials.sql 의 CREATE TABLE 정의 그대로)
-- 행 수가 2만대라 인덱스 켜진 채로도 1~3분이면 끝남.
--
-- 사용 :
--   1) python scripts\sample_commercials.py
--   2) mysql -u ssafy -p --local-infile=1 ssafy_home < sql\load_commercials_summary.sql
-- -----------------------------------------------------

USE `ssafy_home`;
SET NAMES utf8mb4;
SET GLOBAL local_infile = 1;

TRUNCATE TABLE `commercials`;

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_seoul.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_gyeonggi.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_incheon.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_busan.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_daegu.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_daejeon.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_gwangju.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_ulsan.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_sejong.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_gangwon.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_chungbuk.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_chungnam.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_jeonbuk.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_jeonnam.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_gyeongbuk.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_gyeongnam.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

LOAD DATA LOCAL INFILE 'C:/Users/hhy/ssafy/pjt11_buk03_15_09/data/summary/commercials_jeju.csv'
INTO TABLE commercials CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n' IGNORE 1 LINES
( shop_no, shop_name, @branch_name, category_l_code, category_l_name, category_m_code, category_m_name, category_s_code, category_s_name,
  std_industry_code, std_industry_name, sido_code, sido_name, sigungu_code, sigungu_name, haengjeong_code, haengjeong_name, beopjeong_code, beopjeong_name,
  @v20, @v21, @v22, @v23, @v24, jibun_address, @v26, @v27, @v28, @v29, @v30, @building_name, road_address,
  @v33, @v34, @v35, @v36, @v37, @lng, @lat )
SET branch_name = NULLIF(@branch_name, ''), building_name = NULLIF(@building_name, ''),
    longitude = NULLIF(@lng, ''), latitude = NULLIF(@lat, '');

-- 카테고리 마스터 자동 생성
INSERT IGNORE INTO `commercial_categories` (`category_code`, `category_name`)
SELECT DISTINCT category_l_code, category_l_name
FROM commercials
WHERE category_l_code IS NOT NULL AND category_l_name IS NOT NULL;

SELECT COUNT(*) AS total_rows FROM commercials;
