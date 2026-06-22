-- -----------------------------------------------------
-- SSAFY HOME : 공유게시판 (F620)
-- 기준 DB : `ssafy_home` (users 테이블 적용 후 실행)
--
-- 적용 :
--   mysql -u ssafy -p --default-character-set=utf8mb4 ssafy_home < sql/boards.sql
-- -----------------------------------------------------

USE `ssafy_home`;
SET NAMES utf8mb4;

DROP TABLE IF EXISTS `ssafy_home`.`boards`;

CREATE TABLE IF NOT EXISTS `ssafy_home`.`boards` (
  `id`              INT          NOT NULL AUTO_INCREMENT             COMMENT '게시글 PK',
  `title`           VARCHAR(200) NOT NULL                            COMMENT '제목',
  `content`         TEXT         NOT NULL                            COMMENT '본문',
  `author_user_id`  VARCHAR(50)  NOT NULL                            COMMENT '작성자 user_id',
  `view_count`      INT          NOT NULL DEFAULT 0                  COMMENT '조회수',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '작성 시각',
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  INDEX `idx_boards_created_at` (`created_at` DESC) VISIBLE,
  INDEX `idx_boards_author`     (`author_user_id` ASC) VISIBLE,
  CONSTRAINT `fk_board_author`
    FOREIGN KEY (`author_user_id`)
    REFERENCES `ssafy_home`.`users` (`user_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '공유게시판';

-- 시드 게시글 (관리자 ssafy 작성)
INSERT INTO `boards` (`title`, `content`, `author_user_id`)
VALUES
  ('공유게시판이 오픈되었습니다',
   '회원이라면 누구나 글을 작성할 수 있고, 본인 글과 관리자는 수정·삭제가 가능합니다.\n부동산 정보 공유, 이사 후기, 동네 추천 등 자유롭게 이야기 나눠 주세요.',
   'ssafy'),
  ('이용 안내',
   '광고·욕설성 글은 관리자가 즉시 삭제할 수 있습니다. 서로 존중하는 게시판 문화를 만들어 가요.',
   'ssafy');
