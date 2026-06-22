package com.ssafy.prj.model.service;

import java.util.List;
import java.util.Map;

public interface DongCodeService {

    List<Map<String, String>> findSidoList() throws Exception;

    List<Map<String, String>> findSigunguList(String sidoCode) throws Exception;

    List<Map<String, String>> findEmdList(String sigunguCode) throws Exception;
}
