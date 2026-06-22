package com.ssafy.prj.model.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DongCodeMapper {

    List<Map<String, String>> findSidoList();

    List<Map<String, String>> findSigunguList(String sidoCode);

    List<Map<String, String>> findEmdList(String sigunguCode);
}
