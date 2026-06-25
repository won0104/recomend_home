package com.ssafy.prj.model.service;

import java.util.List;

import com.ssafy.prj.model.dto.RealEstateTransaction;
import com.ssafy.prj.model.dto.SearchCondition;
import com.ssafy.prj.model.dto.TransactionSummary;

public interface RealEstateService {

    List<RealEstateTransaction> search(SearchCondition condition) throws Exception;

    TransactionSummary summarize(SearchCondition condition) throws Exception;

    TransactionSummary summarizeAll() throws Exception;
}
