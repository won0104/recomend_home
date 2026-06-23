package com.ssafy.prj.batch.commercial;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 상권 배치가 DB에 접근할 때 사용하는 MyBatis Mapper다.
 *
 * <p>배치는 원본 상가 데이터를 저장하지 않고 동별 집계값만 다룬다. 저장 전 법정동 코드 존재 여부를
 * 확인해 FK 오류를 피하고, 카테고리 단위로 이전 스냅샷을 교체한다.</p>
 */
@Mapper
public interface CommercialMetricsBatchDao {

    /** 외부 API가 준 코드 중 우리 dongcodes 테이블에 실제로 존재하는 코드만 반환한다. */
    List<String> findExistingDongCodes(@Param("dongCodes") List<String> dongCodes);

    /** 한 원천 상권 지표의 이전 집계 스냅샷을 지운다. 새 데이터 수집 성공 후에만 호출한다. */
    int deleteByCategory(@Param("category") String category);

    /** 한 카테고리의 동별 집계 스냅샷을 한 번의 다중 INSERT로 저장한다. */
    int insertMetrics(@Param("metrics") List<DongCommercialMetric> metrics);
}
