package com.ssafy.prj.batch.config;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 홈의 매매·전월세 통합 검색에서 사용하는 조회 인덱스를 안전하게 보장한다.
 *
 * <p>CREATE TABLE IF NOT EXISTS는 이미 만들어진 테이블에 새 INDEX를 추가하지 않는다.
 * 따라서 개발자가 전월세 기능을 추가하기 전에 만든 로컬 DB도, 서버를 다시 실행하는 것만으로
 * 필요한 인덱스를 받을 수 있도록 시작 시 메타데이터를 확인한다.</p>
 *
 * <p>이 컴포넌트는 데이터를 수정하거나 테이블을 삭제하지 않는다. 이름이 고정된 인덱스가 없을 때만
 * CREATE INDEX를 한 번 실행하며, 이후 서버 재시작에서는 아무 작업도 하지 않는다.</p>
 */
@Component
public class RealEstateQueryIndexInitializer {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public RealEstateQueryIndexInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    // 서비스 테이블을 만드는 ApplicationStartedEvent 리스너가 끝난 뒤에 인덱스를 확인한다.
    @EventListener(ApplicationReadyEvent.class)
    public void ensureSearchIndexes() {
        // 법정동 코드로 houseinfos를 먼저 줄인 뒤 거래 테이블과 조인하기 위한 인덱스다.
        createIndexIfMissing("houseinfos", "idx_houseinfos_sgg_apt", "sgg_cd, apt_seq");

        // 기존 housedeals에는 apt_seq 단일 인덱스만 있을 수 있다.
        // 아파트별 거래 연월 필터까지 함께 처리할 수 있도록 복합 인덱스를 보장한다.
        createIndexIfMissing("housedeals", "idx_housedeals_apt_date", "apt_seq, deal_year, deal_month");

        // houserents는 배치 DDL에서 같은 복합 인덱스를 만들지만, 과거 수동 생성 DB도 대비한다.
        createIndexIfMissing("houserents", "idx_houserents_apt_date", "apt_seq, deal_year, deal_month");
    }

    /**
     * 테이블 또는 인덱스가 이미 있으면 조용히 끝낸다.
     *
     * <p>DDL 문자열은 외부 요청값이 아니라 이 클래스에 고정한 테이블·인덱스 이름만 사용한다.
     * 그래서 사용자 입력이 DDL에 섞이지 않는다.</p>
     */
    private void createIndexIfMissing(String tableName, String indexName, String columns) {
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            if (!tableExists(metadata, connection.getCatalog(), tableName)) {
                // 매매 배치를 아직 한 번도 실행하지 않아 housedeals가 없는 경우가 여기에 해당한다.
                return;
            }
            if (indexExists(metadata, connection.getCatalog(), tableName, indexName)) {
                return;
            }
            jdbcTemplate.execute("CREATE INDEX " + indexName + " ON " + tableName + " (" + columns + ")");
            System.out.println("[RealEstateQueryIndex] created " + indexName + " on " + tableName);
        } catch (SQLException exception) {
            throw new IllegalStateException("부동산 검색 인덱스를 확인하지 못했습니다. table=" + tableName, exception);
        }
    }

    private boolean tableExists(DatabaseMetaData metadata, String catalog, String tableName) throws SQLException {
        try (ResultSet tables = metadata.getTables(catalog, null, tableName, new String[] {"TABLE"})) {
            return tables.next();
        }
    }

    private boolean indexExists(DatabaseMetaData metadata, String catalog, String tableName, String indexName)
            throws SQLException {
        try (ResultSet indexes = metadata.getIndexInfo(catalog, null, tableName, false, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
