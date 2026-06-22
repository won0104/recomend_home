package com.ssafy.prj.model.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.prj.model.dto.Commercial;
import com.ssafy.prj.model.dto.CommercialSearchCondition;
import com.ssafy.prj.model.mapper.CommercialMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommercialServiceImpl implements CommercialService {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 1000;

    private final CommercialMapper commercialMapper;

    @Override
    @Transactional(readOnly = true)
    public List<Commercial> search(CommercialSearchCondition condition) throws Exception {
        if (condition == null || (
                isBlank(condition.getSigunguCode())
                && isBlank(condition.getBeopjeongName())
                && isBlank(condition.getCategoryLCode()))) {
            throw new IllegalArgumentException(
                    "시군구코드, 법정동, 대분류 중 하나 이상을 지정해야 합니다. 상호명만으로는 검색할 수 없습니다.");
        }
        condition.setLimit(resolveLimit(condition.getLimit()));
        return commercialMapper.search(condition);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> findCategoryLargeList() throws Exception {
        List<Map<String, String>> categories = commercialMapper.findCategoryLargeList();
        if (!categories.isEmpty()) {
            return categories;
        }
        return commercialMapper.findCategoryLargeFallbackList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> findCategoryMediumList(String categoryLCode) throws Exception {
        return commercialMapper.findCategoryMediumList(categoryLCode);
    }

    private int resolveLimit(Integer requested) {
        if (requested == null || requested <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requested, MAX_LIMIT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
