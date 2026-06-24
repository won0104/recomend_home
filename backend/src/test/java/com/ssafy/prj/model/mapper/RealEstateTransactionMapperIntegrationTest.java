package com.ssafy.prj.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.prj.model.dto.SearchCondition;
import com.ssafy.prj.model.dto.TransactionSummary;

/** 실제 MyBatis 매퍼가 매매·전월세 UNION 쿼리를 정상적으로 읽는지 확인한다. */
@SpringBootTest
@TestPropertySource(properties = {
        "batch.estate.enabled=false",
        "batch.estate-rent.enabled=false",
        "batch.estate-rent.run-on-startup=false",
        "batch.commercial.enabled=false"
})
class RealEstateTransactionMapperIntegrationTest {

    @Autowired
    private RealEstateTransactionMapper transactionMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 전체_요약은_매매와_전월세_건수의_합이다() {
        TransactionSummary summary = transactionMapper.summarize(new SearchCondition());

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalCount()).isEqualTo(summary.getDealCount() + summary.getRentCount());
        assertThat(transactionMapper.search(new SearchCondition())).isNotNull();
    }

    @Test
    @Transactional
    void 전월세_한건을_추가하면_검색과_요약에_전월세로_합산된다() {
        Map<String, Object> house = jdbcTemplate.queryForMap(
                "SELECT apt_seq, sgg_cd FROM houseinfos ORDER BY apt_seq LIMIT 1");
        String aptSeq = (String) house.get("apt_seq");
        String lawdCd = (String) house.get("sgg_cd");

        // @Transactional 테스트가 끝나면 자동 롤백된다. 로컬 데이터에는 테스트 행이 남지 않는다.
        jdbcTemplate.update("""
                INSERT INTO houserents
                    (apt_seq, floor, deal_year, deal_month, deal_day, exclu_use_ar,
                     deposit, monthly_rent, source_hash)
                VALUES (?, '10', 2099, 1, 1, 59.99, 10000, 50, ?)
                """, aptSeq, UUID.randomUUID().toString().replace("-", ""));
        jdbcTemplate.update("""
                INSERT INTO houserents
                    (apt_seq, floor, deal_year, deal_month, deal_day, exclu_use_ar,
                     deposit, monthly_rent, source_hash)
                VALUES (?, '11', 2099, 1, 2, 59.99, 20000, 0, ?)
                """, aptSeq, UUID.randomUUID().toString().replace("-", ""));

        SearchCondition condition = new SearchCondition();
        condition.setLawdCd(lawdCd);
        condition.setContractType("rent");
        condition.setDealYmd("209901");
        condition.setRentType("monthly");
        condition.setDepositMin(9_000);
        condition.setDepositMax(11_000);
        condition.setMonthlyRentMin(40);
        condition.setMonthlyRentMax(60);

        var transactions = transactionMapper.search(condition);
        TransactionSummary summary = transactionMapper.summarize(condition);

        assertThat(transactions).anySatisfy(transaction -> {
            assertThat(transaction.getContractType()).isEqualTo("rent");
            assertThat(transaction.getDeposit()).isEqualTo(10_000);
            assertThat(transaction.getMonthlyRent()).isEqualTo(50);
        });
        assertThat(summary.getTotalCount()).isEqualTo(1);
        assertThat(summary.getDealCount()).isZero();
        assertThat(summary.getRentCount()).isEqualTo(1);

        SearchCondition jeonseCondition = new SearchCondition();
        jeonseCondition.setLawdCd(lawdCd);
        jeonseCondition.setContractType("rent");
        jeonseCondition.setDealYmd("209901");
        jeonseCondition.setRentType("jeonse");
        jeonseCondition.setDepositMin(19_000);
        jeonseCondition.setDepositMax(21_000);

        var jeonseTransactions = transactionMapper.search(jeonseCondition);
        TransactionSummary jeonseSummary = transactionMapper.summarize(jeonseCondition);

        assertThat(jeonseTransactions).singleElement().satisfies(transaction -> {
            assertThat(transaction.getDeposit()).isEqualTo(20_000);
            assertThat(transaction.getMonthlyRent()).isZero();
        });
        assertThat(jeonseSummary.getTotalCount()).isEqualTo(1);
        assertThat(jeonseSummary.getRentCount()).isEqualTo(1);
    }
}
