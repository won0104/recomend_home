package com.ssafy.prj.model.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        List<RealEstateTransaction> transactions = search(condition);

        int dealCount = 0;
        long sumDealAmount = 0L;
        for (RealEstateTransaction tx : transactions) {
            if ("deal".equals(tx.getContractType())) {
                dealCount++;
                sumDealAmount += tx.getAmount();
            }
        }
        int avgDealAmount = dealCount == 0 ? 0 : (int) Math.round((double) sumDealAmount / dealCount);
        int rentCount = transactions.size() - dealCount;
        return new TransactionSummary(transactions.size(), dealCount, rentCount, avgDealAmount);
    }
}
