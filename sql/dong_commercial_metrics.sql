-- 동네 추천용 상권 집계 테이블
-- 원본 점포가 아니라 업종별·법정동별 건수만 보관합니다.

USE `ssafy_home`;

CREATE TABLE IF NOT EXISTS `dong_commercial_metrics` (
  `dong_code` VARCHAR(10) NOT NULL,
  `category` VARCHAR(30) NOT NULL,
  `business_count` INT NOT NULL DEFAULT 0,
  `source_date` DATE NOT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`dong_code`, `category`),
  CONSTRAINT `fk_commercial_metrics_dong`
    FOREIGN KEY (`dong_code`) REFERENCES `dongcodes` (`dong_code`)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
