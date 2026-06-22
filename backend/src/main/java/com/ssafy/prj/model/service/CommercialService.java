package com.ssafy.prj.model.service;

import java.util.List;
import java.util.Map;

import com.ssafy.prj.model.dto.Commercial;
import com.ssafy.prj.model.dto.CommercialSearchCondition;

public interface CommercialService {

    List<Commercial> search(CommercialSearchCondition condition) throws Exception;

    List<Map<String, String>> findCategoryLargeList() throws Exception;

    List<Map<String, String>> findCategoryMediumList(String categoryLCode) throws Exception;
}
