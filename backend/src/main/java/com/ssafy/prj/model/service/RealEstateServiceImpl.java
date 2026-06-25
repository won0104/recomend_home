package com.ssafy.prj.model.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.prj.config.CacheConfig;

import com.ssafy.prj.model.dto.RealEstateTransaction;
import com.ssafy.prj.model.dto.SearchCondition;
import com.ssafy.prj.model.dto.TransactionSummary;
import com.ssafy.prj.model.mapper.RealEstateTransactionMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RealEstateServiceImpl implements RealEstateService {

    private final RealEstateTransactionMapper transactionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RealEstateTransaction> search(SearchCondition condition) throws Exception {
        return transactionMapper.search(condition);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionSummary summarize(SearchCondition condition) throws Exception {
        // search()는 화면 목록 보호를 위해 최대 1,000건만 반환한다.
        // 첫 화면 통계는 모든 거래를 대상으로 해야 하므로 별도 집계 SQL을 사용한다.
        return transactionMapper.summarize(condition);
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.STATS_TOTAL, key = "'all'")
    @Transactional(readOnly = true)
    public TransactionSummary summarizeAll() throws Exception {
        return transactionMapper.summarizeAll();
    }
}
