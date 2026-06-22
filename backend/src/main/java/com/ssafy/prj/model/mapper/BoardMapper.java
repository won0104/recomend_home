package com.ssafy.prj.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.prj.model.dto.Board;

@Mapper
public interface BoardMapper {

    List<Board> findAll(String keyword);

    List<Board> findByAuthorUserId(String authorUserId);

    Board findById(int id);

    int insert(Board board);

    int update(Board board);

    int delete(int id);

    int incrementViewCount(int id);
}
