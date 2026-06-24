-- 동네 추천용 서울 버스 정류소 집계 테이블
-- 배치가 처음 실행될 때도 자동 생성되지만, DB 구조를 미리 확인하거나 수동 생성할 때 사용할 수 있다.

USE `ssafy_home`;

CREATE TABLE IF NOT EXISTS `dong_bus_stop_metrics` (
  `dong_code` VARCHAR(10) NOT NULL COMMENT '법정동코드',
  `bus_stop_count` INT NOT NULL DEFAULT 0 COMMENT '법정동별 서울 버스 정류소 수',
  `source_date` DATE NOT NULL COMMENT '서울 Open API 수집 기준일',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '집계 갱신 시각',
  PRIMARY KEY (`dong_code`),
  CONSTRAINT `fk_bus_stop_metrics_dong`
    FOREIGN KEY (`dong_code`) REFERENCES `dongcodes` (`dong_code`)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='동네 추천용 법정동별 서울 버스 정류소 수';

-- 원본 정류소 전체를 저장하지 않고, 좌표→법정동 변환 결과만 재사용한다.
-- 같은 정류소가 다음 배치에도 같은 좌표로 오면 카카오 역지오코딩을 다시 호출하지 않는다.
CREATE TABLE IF NOT EXISTS `bus_stop_dong_cache` (
  `stops_no` VARCHAR(30) NOT NULL COMMENT '서울 Open API 정류소번호',
  `latitude` DECIMAL(10, 7) NOT NULL COMMENT '위도(WGS84)',
  `longitude` DECIMAL(11, 7) NOT NULL COMMENT '경도(WGS84)',
  `dong_code` VARCHAR(10) NULL COMMENT '좌표 역지오코딩으로 확인한 법정동코드',
  `last_seen_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '최근 API 확인 시각',
  PRIMARY KEY (`stops_no`),
  KEY `idx_bus_stop_dong_cache_dong_code` (`dong_code`),
  CONSTRAINT `fk_bus_stop_dong_cache_dong`
    FOREIGN KEY (`dong_code`) REFERENCES `dongcodes` (`dong_code`)
    ON UPDATE CASCADE
    ON DELETE SET NULL
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='서울 버스 정류소 법정동 변환 캐시';
