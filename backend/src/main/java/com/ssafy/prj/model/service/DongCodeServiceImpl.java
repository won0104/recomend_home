package com.ssafy.prj.model.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.prj.model.mapper.DongCodeMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DongCodeServiceImpl implements DongCodeService {

    private final DongCodeMapper dongCodeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> findSidoList() throws Exception {
        return dongCodeMapper.findSidoList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> findSigunguList(String sidoCode) throws Exception {
        if (isBlank(sidoCode)) {
            return Collections.emptyList();
        }
        return dongCodeMapper.findSigunguList(sidoCode.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> findEmdList(String sigunguCode) throws Exception {
        if (isBlank(sigunguCode)) {
            return Collections.emptyList();
        }
        return dongCodeMapper.findEmdList(sigunguCode.trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
