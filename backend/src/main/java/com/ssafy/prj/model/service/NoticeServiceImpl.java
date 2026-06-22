package com.ssafy.prj.model.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.prj.model.dto.Notice;
import com.ssafy.prj.model.mapper.NoticeMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<Notice> findAll(String keyword) throws Exception {
        return noticeMapper.findAll(keyword);
    }

    @Override
    @Transactional
    public Notice findByIdAndIncreaseView(int id) throws Exception {
        noticeMapper.incrementViewCount(id);
        return noticeMapper.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Notice findById(int id) throws Exception {
        return noticeMapper.findById(id);
    }

    @Override
    @Transactional
    public Notice register(String title, String content, String authorUserId) throws Exception {
        if (isBlank(title) || isBlank(content)) {
            throw new IllegalArgumentException("제목과 내용은 필수입니다.");
        }
        if (isBlank(authorUserId)) {
            throw new IllegalArgumentException("작성자 정보가 없습니다.");
        }

        Notice notice = new Notice();
        notice.setTitle(title.trim());
        notice.setContent(content);
        notice.setAuthorUserId(authorUserId);
        notice.setAuthorName(authorUserId);
        noticeMapper.insert(notice);
        return notice;
    }

    @Override
    @Transactional
    public Notice update(int id, String title, String content) throws Exception {
        if (isBlank(title) || isBlank(content)) {
            throw new IllegalArgumentException("제목과 내용은 필수입니다.");
        }

        Notice notice = noticeMapper.findById(id);
        if (notice == null) {
            throw new IllegalArgumentException("공지사항을 찾을 수 없습니다.");
        }
        notice.setTitle(title.trim());
        notice.setContent(content);
        noticeMapper.update(notice);
        return noticeMapper.findById(id);
    }

    @Override
    @Transactional
    public boolean delete(int id) throws Exception {
        return noticeMapper.delete(id) == 1;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
