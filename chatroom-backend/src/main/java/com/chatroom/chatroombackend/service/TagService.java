package com.chatroom.chatroombackend.service;

import com.chatroom.chatroombackend.entity.Tag;
import com.chatroom.chatroombackend.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    /**
     * 获取所有标签的名称列表。
     * @return 一个包含所有标签名称的字符串列表。
     */
    public List<String> getAllTagNames() {
        return tagRepository.findAll().stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
    }
}