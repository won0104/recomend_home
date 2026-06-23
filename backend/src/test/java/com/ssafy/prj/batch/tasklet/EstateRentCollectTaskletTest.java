package com.ssafy.prj.batch.tasklet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ssafy.prj.batch.domain.EstateRentItem;

class EstateRentCollectTaskletTest {

    @Test
    void 전월세_API_XML을_보증금_월세_계약정보로_변환한다() throws Exception {
        EstateRentCollectTasklet tasklet = new EstateRentCollectTasklet(null);

        List<EstateRentItem> items = tasklet.parseItems("""
                <response><body><items>
                  <item>
                    <umdNm>내수동</umdNm><aptNm>테스트아파트</aptNm><jibun>71</jibun>
                    <excluUseAr>84.92</excluUseAr><dealYear>2025</dealYear>
                    <dealMonth>01</dealMonth><dealDay>03</dealDay><deposit>50,000</deposit>
                    <monthlyRent>0</monthlyRent><floor>12</floor><buildYear>2004</buildYear>
                    <contractTerm>25.02~27.01</contractTerm><contractType>신규</contractType>
                    <useRRRight>사용</useRRRight><preDeposit>45,000</preDeposit>
                    <preMonthlyRent>0</preMonthlyRent>
                  </item>
                </items></body></response>
                """, "11110", "202501");

        assertThat(items).hasSize(1);
        EstateRentItem item = items.getFirst();
        assertThat(item.getLawdCd()).isEqualTo("11110");
        assertThat(item.getDealYmd()).isEqualTo("202501");
        assertThat(item.getAptName()).isEqualTo("테스트아파트");
        assertThat(item.getDeposit()).isEqualTo(50_000);
        assertThat(item.getMonthlyRent()).isZero();
        assertThat(item.getDealDate()).isEqualTo("2025-01-03");
        assertThat(item.getContractTerm()).isEqualTo("25.02~27.01");
        assertThat(item.getContractType()).isEqualTo("신규");
        assertThat(item.getUseRrRight()).isEqualTo("사용");
        assertThat(item.getPreDeposit()).isEqualTo(45_000);
    }

    @Test
    void 월세가_0보다_크면_월세_데이터로_그대로_보관한다() throws Exception {
        EstateRentCollectTasklet tasklet = new EstateRentCollectTasklet(null);

        List<EstateRentItem> items = tasklet.parseItems("""
                <response><body><items><item>
                  <umdNm>필운동</umdNm><aptNm>월세테스트</aptNm><jibun>10</jibun>
                  <dealYear>2025</dealYear><dealMonth>02</dealMonth><dealDay>01</dealDay>
                  <deposit>1,000</deposit><monthlyRent>85</monthlyRent>
                </item></items></body></response>
                """, "11110", "202502");

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.getDeposit()).isEqualTo(1_000);
            assertThat(item.getMonthlyRent()).isEqualTo(85);
            assertThat(item.getContractType()).isEmpty();
            assertThat(item.getPreDeposit()).isNull();
        });
    }
}
