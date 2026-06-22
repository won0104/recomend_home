package com.ssafy.prj.model.service;

import java.util.List;

import com.ssafy.prj.model.dto.Notice;

public interface NoticeService {

    List<Notice> findAll(String keyword) throws Exception;

    Notice findByIdAndIncreaseView(int id) throws Exception;

    Notice findById(int id) throws Exception;

    Notice register(String title, String content, String authorUserId) throws Exception;

    Notice update(int id, String title, String content) throws Exception;

    boolean delete(int id) throws Exception;
}
