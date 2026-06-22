package com.ssafy.prj.model.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.prj.model.dto.Commercial;
import com.ssafy.prj.model.dto.CommercialSearchCondition;

@Mapper
public interface CommercialMapper {

    List<Commercial> search(CommercialSearchCondition condition);

    List<Map<String, String>> findCategoryLargeList();

    List<Map<String, String>> findCategoryLargeFallbackList();

    List<Map<String, String>> findCategoryMediumList(String categoryLCode);
}
