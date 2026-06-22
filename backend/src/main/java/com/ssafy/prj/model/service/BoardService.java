package com.ssafy.prj.model.service;

import java.util.List;

import com.ssafy.prj.model.dto.Board;

public interface BoardService {

    List<Board> findAll(String keyword) throws Exception;

    List<Board> findByAuthorUserId(String authorUserId) throws Exception;

    Board findByIdAndIncreaseView(int id) throws Exception;

    Board findById(int id) throws Exception;

    Board register(String title, String content, String authorUserId) throws Exception;

    Board update(int id, String title, String content, String actorUserId, boolean actorIsAdmin) throws Exception;

    boolean delete(int id, String actorUserId, boolean actorIsAdmin) throws Exception;
}
