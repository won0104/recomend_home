package com.ssafy.prj.batch.bus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** 외부 API를 호출하지 않고 XML 필드명과 XCRD/YCRD 좌표 순서를 검증한다. */
class BusStopApiResponseParserTest {

    private final BusStopApiResponseParser parser = new BusStopApiResponseParser();

    @Test
    void 정상_응답에서_정류소번호와_WGS84_좌표를_읽는다() throws Exception {
        BusStopApiPage page = parser.parse("""
            <busStopLocationXyInfo>
              <list_total_count>2</list_total_count>
              <RESULT><CODE>INFO-000</CODE><MESSAGE>정상 처리되었습니다</MESSAGE></RESULT>
              <row><STOPS_NO>100000001</STOPS_NO><XCRD>126.9781234</XCRD><YCRD>37.5665678</YCRD></row>
              <row><STOPS_NO>100000002A</STOPS_NO><XCRD>127.0123456</XCRD><YCRD>37.5123456</YCRD></row>
            </busStopLocationXyInfo>
            """);

        assertThat(page.totalCount()).isEqualTo(2);
        assertThat(page.stops()).hasSize(2);
        assertThat(page.stops().get(0).stopsNo()).isEqualTo("100000001");
        assertThat(page.stops().get(0).longitude().toPlainString()).isEqualTo("126.9781234");
        assertThat(page.stops().get(0).latitude().toPlainString()).isEqualTo("37.5665678");
    }

    @Test
    void 서울_Open_API_오류_응답은_집계를_교체하기_전에_실패시킨다() {
        assertThatThrownBy(() -> parser.parse("""
            <busStopLocationXyInfo>
              <RESULT><CODE>ERROR-300</CODE><MESSAGE>인증키가 유효하지 않습니다</MESSAGE></RESULT>
            </busStopLocationXyInfo>
            """))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ERROR-300");
    }

    @Test
    void WGS84가_아닌_좌표는_법정동_오매핑을_막기_위해_실패시킨다() {
        assertThatThrownBy(() -> parser.parse("""
            <busStopLocationXyInfo>
              <list_total_count>1</list_total_count>
              <RESULT><CODE>INFO-000</CODE><MESSAGE>정상</MESSAGE></RESULT>
              <row><STOPS_NO>100000001</STOPS_NO><XCRD>953210.1</XCRD><YCRD>1950210.2</YCRD></row>
            </busStopLocationXyInfo>
            """))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not WGS84");
    }
}
