package com.ssafy.prj.model.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.prj.model.dto.Board;
import com.ssafy.prj.model.mapper.BoardMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardMapper boardMapper;

    @Override
    @Transactional(readOnly = true)
    public List<Board> findAll(String keyword) throws Exception {
        return boardMapper.findAll(keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Board> findByAuthorUserId(String authorUserId) throws Exception {
        if (isBlank(authorUserId)) {
            return Collections.emptyList();
        }
        return boardMapper.findByAuthorUserId(authorUserId);
    }

    @Override
    @Transactional
    public Board findByIdAndIncreaseView(int id) throws Exception {
        boardMapper.incrementViewCount(id);
        return boardMapper.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Board findById(int id) throws Exception {
        return boardMapper.findById(id);
    }

    @Override
    @Transactional
    public Board register(String title, String content, String authorUserId) throws Exception {
        if (isBlank(title) || isBlank(content)) {
            throw new IllegalArgumentException("제목과 내용은 필수입니다.");
        }
        if (isBlank(authorUserId)) {
            throw new IllegalArgumentException("작성자 정보가 없습니다.");
        }

        Board board = new Board();
        board.setTitle(title.trim());
        board.setContent(content);
        board.setAuthorUserId(authorUserId);
        boardMapper.insert(board);
        return board;
    }

    @Override
    @Transactional
    public Board update(int id, String title, String content, String actorUserId, boolean actorIsAdmin) throws Exception {
        if (isBlank(title) || isBlank(content)) {
            throw new IllegalArgumentException("제목과 내용은 필수입니다.");
        }

        Board board = boardMapper.findById(id);
        if (board == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        ensureCanModify(board, actorUserId, actorIsAdmin);

        board.setTitle(title.trim());
        board.setContent(content);
        boardMapper.update(board);
        return boardMapper.findById(id);
    }

    @Override
    @Transactional
    public boolean delete(int id, String actorUserId, boolean actorIsAdmin) throws Exception {
        Board board = boardMapper.findById(id);
        if (board == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        ensureCanModify(board, actorUserId, actorIsAdmin);

        return boardMapper.delete(id) == 1;
    }

    private void ensureCanModify(Board board, String actorUserId, boolean actorIsAdmin) {
        if (actorIsAdmin) {
            return;
        }
        if (actorUserId != null && actorUserId.equals(board.getAuthorUserId())) {
            return;
        }
        throw new IllegalArgumentException("본인 글이거나 관리자만 수정/삭제할 수 있습니다.");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
