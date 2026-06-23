package com.ssafy.prj.batch.commercial;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommercialMetricsBatchDao {

    List<String> findExistingDongCodes(@Param("dongCodes") List<String> dongCodes);

    int deleteByCategory(@Param("category") String category);

    int insertMetrics(@Param("metrics") List<DongCommercialMetric> metrics);
}
