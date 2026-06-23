package com.ssafy.prj.batch.config;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ssafy.prj.batch.dao.EstateRentBatchDao;

/**
 * 전월세 수집을 아직 실행하지 않아도 /api/properties 조회가 실패하지 않게 서비스 테이블만 보장한다.
 *
 * <p>모두 CREATE TABLE IF NOT EXISTS라 기존 테이블·데이터를 수정하거나 비우지 않는다.
 * 배치 실행 기록 테이블은 실제 수집할 때만 만든다.</p>
 */
@Component
public class EstateRentSchemaInitializer {

    private final EstateRentBatchDao estateRentBatchDao;

    public EstateRentSchemaInitializer(EstateRentBatchDao estateRentBatchDao) {
        this.estateRentBatchDao = estateRentBatchDao;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void ensureServiceTables() {
        estateRentBatchDao.createHouseInfoTable();
        estateRentBatchDao.createHouseRentsTable();
    }
}
