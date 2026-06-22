USE ssafy_home;

CREATE TABLE IF NOT EXISTS batch_estate_market_reports (
    report_id VARCHAR(40) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    lawd_cd VARCHAR(10) NOT NULL,
    deal_ymd VARCHAR(6) NOT NULL,
    collected_at VARCHAR(30) NOT NULL,
    item_count INT NOT NULL DEFAULT 0,
    avg_deal_amount INT NOT NULL DEFAULT 0,
    max_deal_amount INT NOT NULL DEFAULT 0,
    min_deal_amount INT NOT NULL DEFAULT 0,
    ai_summary TEXT,
    ai_status VARCHAR(30),
    pdf_path VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS batch_estate_market_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id VARCHAR(40) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    lawd_cd VARCHAR(10) NOT NULL,
    deal_ymd VARCHAR(6) NOT NULL,
    apt_name VARCHAR(200) NOT NULL,
    dong VARCHAR(100),
    jibun VARCHAR(100),
    deal_amount INT NOT NULL DEFAULT 0,
    area DECIMAL(10, 2) NOT NULL DEFAULT 0,
    floor INT NOT NULL DEFAULT 0,
    build_year INT NOT NULL DEFAULT 0,
    deal_date VARCHAR(10),
    raw_address VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_batch_estate_report_id (report_id),
    INDEX idx_batch_estate_lawd_deal (lawd_cd, deal_ymd)
);
