package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.entity.UserBlock;
import com.chatroom.chatroombackend.entity.UserBlockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, UserBlockId> {

    // New method to get IDs of users blocked by a specific user
    @Query("SELECT ub.blocked.id FROM UserBlock ub WHERE ub.blocker.id = :blockerId")
    List<Long> findBlockedIdsByBlockerId(Long blockerId);
}