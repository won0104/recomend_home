-- -----------------------------------------------------
-- SSAFY HOME : 회원/관심지역 추가 스키마
-- 기준 DB : `ssafy_home` (SSAFY_HOME_Schema.sql 적용 후 실행)
-- -----------------------------------------------------

USE `ssafy_home`;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `ssafy_home`.`favorite_areas`;
DROP TABLE IF EXISTS `ssafy_home`.`users`;
SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------
-- Table `ssafy_home`.`users`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ssafy_home`.`users` (
  `user_id`       VARCHAR(50)  NOT NULL                                COMMENT '회원 아이디',
  `name`          VARCHAR(50)  NOT NULL                                COMMENT '회원 이름',
  `email`         VARCHAR(100) NOT NULL                                COMMENT '이메일',
  `phone`         VARCHAR(20)  NULL DEFAULT ''                         COMMENT '연락처',
  `password_hash` CHAR(64)     NOT NULL                                COMMENT 'SHA-256 해시 비밀번호',
  `role`          VARCHAR(10)  NOT NULL DEFAULT 'USER'                 COMMENT '권한 : USER / ADMIN',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '생성 시각',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '수정 시각',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_users_email` (`email`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '회원 정보 테이블';

-- -----------------------------------------------------
-- Table `ssafy_home`.`favorite_areas`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ssafy_home`.`favorite_areas` (
  `id`         INT          NOT NULL AUTO_INCREMENT                   COMMENT '관심지역 PK',
  `user_id`    VARCHAR(50)  NOT NULL                                  COMMENT '회원 아이디',
  `sido`       VARCHAR(30)  NULL                                      COMMENT '시도 이름',
  `sigungu`    VARCHAR(30)  NULL                                      COMMENT '시군구 이름',
  `emd`        VARCHAR(30)  NULL                                      COMMENT '읍면동 이름',
  `lawd_cd`    VARCHAR(10)  NOT NULL                                  COMMENT '법정동 시군구 코드(5자리)',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP        COMMENT '생성 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favorite_user_lawd` (`user_id`, `lawd_cd`),
  INDEX `idx_favorite_user` (`user_id` ASC) VISIBLE,
  CONSTRAINT `fk_favorite_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `ssafy_home`.`users` (`user_id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '회원 관심지역 테이블';

-- -----------------------------------------------------
-- 시드 데이터 : 기본 회원 (id=ssafy / pw=1234, SHA-256)
-- -----------------------------------------------------
INSERT INTO `ssafy_home`.`users`
  (`user_id`, `name`, `email`, `phone`, `password_hash`, `role`)
VALUES
  ('ssafy', 'SSAFY', 'ssafy@ssafy.com', '010-1234-5678',
   '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'ADMIN')
ON DUPLICATE KEY UPDATE `role` = 'ADMIN', `updated_at` = CURRENT_TIMESTAMP;
