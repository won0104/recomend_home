-- =====================================================
-- parks_for_import.csv 적재 스크립트
-- 실행 전: sql/parks.sql을 먼저 실행한다.
-- =====================================================

USE `ssafy_home`;

LOAD DATA LOCAL INFILE '/Users/chaewon/Downloads/pjt13_buk03_15_08/data/parks_for_import.csv'
INTO TABLE `parks`
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(
  @park_name,
  @road_address,
  @latitude,
  @longitude,
  @park_area,
  @dong_code
)
SET
  park_name = NULLIF(@park_name, ''),
  road_address = NULLIF(@road_address, ''),
  latitude = NULLIF(@latitude, ''),
  longitude = NULLIF(@longitude, ''),
  park_area = NULLIF(@park_area, ''),
  dong_code = NULLIF(@dong_code, '');

-- 적재 결과 확인
SELECT
  COUNT(*) AS total_parks,
  SUM(dong_code IS NOT NULL) AS parks_with_dong_code,
  SUM(dong_code IS NULL) AS parks_without_dong_code
FROM `parks`;
