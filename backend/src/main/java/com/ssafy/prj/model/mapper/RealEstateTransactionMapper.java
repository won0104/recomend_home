package com.ssafy.prj.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.prj.model.dto.RealEstateTransaction;
import com.ssafy.prj.model.dto.SearchCondition;

@Mapper
public interface RealEstateTransactionMapper {

    List<RealEstateTransaction> search(SearchCondition condition);
}
