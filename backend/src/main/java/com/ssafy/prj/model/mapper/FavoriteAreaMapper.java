package com.ssafy.prj.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.prj.model.dto.FavoriteArea;

@Mapper
public interface FavoriteAreaMapper {

    List<FavoriteArea> findByUserId(String userId);

    int insert(FavoriteArea favoriteArea);

    int delete(@Param("userId") String userId, @Param("key") String key);

    int deleteByUserId(String userId);
}
