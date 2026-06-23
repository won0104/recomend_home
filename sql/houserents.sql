-- ============================================================
-- 아파트 전월세 실거래가 저장 테이블
--
-- 매매(housedeals)와 전월세를 한 테이블에 섞지 않는다.
-- 금액의 의미가 다르므로, 전월세는 보증금(deposit)과 월세(monthly_rent)를
-- 독립적으로 보관한다. monthly_rent가 0이면 전세로 판단할 수 있다.
-- ============================================================

CREATE TABLE IF NOT EXISTS houserents (
    rent_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '전월세 거래 식별자',
    apt_seq VARCHAR(20) NOT NULL COMMENT 'houseinfos 아파트 코드',
    floor VARCHAR(3) NULL COMMENT '층',
    deal_year INT NOT NULL COMMENT '계약 연도',
    deal_month INT NOT NULL COMMENT '계약 월',
    deal_day INT NOT NULL COMMENT '계약 일',
    exclu_use_ar DECIMAL(7, 2) NULL COMMENT '전용면적(㎡)',
    deposit INT NOT NULL DEFAULT 0 COMMENT '보증금(만원)',
    monthly_rent INT NOT NULL DEFAULT 0 COMMENT '월세(만원), 0이면 전세',
    contract_term VARCHAR(50) NULL COMMENT '계약기간 원문',
    contract_type VARCHAR(30) NULL COMMENT '계약구분 원문(신규·갱신 등)',
    use_rr_right VARCHAR(30) NULL COMMENT '갱신요구권 사용 여부 원문',
    pre_deposit INT NULL COMMENT '종전 보증금(만원)',
    pre_monthly_rent INT NULL COMMENT '종전 월세(만원)',
    source_hash CHAR(64) NOT NULL COMMENT '원천 응답 기반 중복 방지 해시',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (rent_id),
    UNIQUE KEY uk_houserents_source_hash (source_hash),
    KEY idx_houserents_apt_date (apt_seq, deal_year, deal_month, deal_day),
    CONSTRAINT fk_houserents_houseinfos
        FOREIGN KEY (apt_seq) REFERENCES houseinfos (apt_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
