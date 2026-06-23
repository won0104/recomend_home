package com.ssafy.prj.model.mapper.neighborhood;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.prj.model.dto.neighborhood.NeighborhoodSchoolCount;

/**
 * 학교 데이터 조회 Mapper
 */
@Mapper
public interface SchoolMapper {

    /**
     * 법정동코드별 학교 수 조회
     *
     * @param lawdCd 법정동코드
     * @return 해당 법정동의 학교 수
     */
    int countByDongCode(@Param("dongCode") String dongCode);

    /**
     * 후보 동별 운영 중인 학교 수와 행정구역 이름을 함께 조회한다.
     */
    List<NeighborhoodSchoolCount> countByDongCodes(
        @Param("dongCodes") List<String> dongCodes
    );

    /**
     * 시군구별 학교 수 조회 (첫 5자리 기준)
     *
     * @param sigunguCode 시군구코드 (첫 5자리)
     * @return 해당 시군구의 학교 수
     */
    int countBySimgungu(@Param("sigunguCode") String sigunguCode);

    /**
     * 전체 학교 수
     *
     * @return 전체 학교 수
     */
    int countAll();
}
