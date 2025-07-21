package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.entity.Conversation;
import com.chatroom.chatroombackend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    // 获取指定聊天的最后一条消息
    Optional<Message> findTopByConversationOrderByCreatedAtDesc(Conversation conversation);
}