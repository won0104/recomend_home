-- -----------------------------------------------------
-- SSAFY HOME : 공지사항 (F610)
-- 기준 DB : `ssafy_home` (users 테이블 적용 후 실행)
-- -----------------------------------------------------

USE `ssafy_home`;
SET NAMES utf8mb4;

DROP TABLE IF EXISTS `ssafy_home`.`notices`;

CREATE TABLE IF NOT EXISTS `ssafy_home`.`notices` (
  `id`              INT          NOT NULL AUTO_INCREMENT             COMMENT '공지 PK',
  `title`           VARCHAR(200) NOT NULL                            COMMENT '제목',
  `content`         TEXT         NOT NULL                            COMMENT '본문',
  `author_user_id`  VARCHAR(50)  NOT NULL                            COMMENT '작성자 user_id',
  `view_count`      INT          NOT NULL DEFAULT 0                  COMMENT '조회수',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '작성 시각',
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  INDEX `idx_notices_created_at` (`created_at` DESC) VISIBLE,
  INDEX `idx_notices_author`     (`author_user_id` ASC) VISIBLE,
  CONSTRAINT `fk_notice_author`
    FOREIGN KEY (`author_user_id`)
    REFERENCES `ssafy_home`.`users` (`user_id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '공지사항';

-- 샘플 공지 (관리자 ssafy 작성)
INSERT INTO `notices` (`title`, `content`, `author_user_id`)
VALUES
  ('SSAFY HOME 서비스 오픈 안내',
   '안녕하세요. SSAFY HOME 서비스가 정식 오픈되었습니다.\n부동산 실거래가 조회와 관심지역 등록을 자유롭게 이용해 주세요.',
   'ssafy'),
  ('상가(상권) 정보 검색 기능 추가',
   '주변상권 카드에서 시군구·법정동·업종 기준으로 상가 정보를 검색할 수 있습니다.',
   'ssafy');
