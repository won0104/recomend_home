package com.ssafy.prj.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.prj.model.dto.RealEstateTransaction;
import com.ssafy.prj.model.dto.SearchCondition;
import com.ssafy.prj.model.dto.TransactionSummary;

@Mapper
public interface RealEstateTransactionMapper {

    List<RealEstateTransaction> search(SearchCondition condition);

    /** 목록 조회의 1,000건 제한과 별개로, 조건에 맞는 전체 거래 통계를 DB에서 집계한다. */
    TransactionSummary summarize(SearchCondition condition);

    /** 필터 없이 DB 전체 거래 건수·평균 매매가를 집계한다. 홈 화면 초기 통계 카드용. */
    TransactionSummary summarizeAll();
}
