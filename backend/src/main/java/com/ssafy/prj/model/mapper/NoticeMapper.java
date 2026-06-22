package com.ssafy.prj.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.prj.model.dto.Notice;

@Mapper
public interface NoticeMapper {

    List<Notice> findAll(String keyword);

    Notice findById(int id);

    int insert(Notice notice);

    int update(Notice notice);

    int delete(int id);

    int incrementViewCount(int id);
}
