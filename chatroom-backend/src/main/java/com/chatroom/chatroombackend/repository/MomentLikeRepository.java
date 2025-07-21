package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.entity.Moment;
import com.chatroom.chatroombackend.entity.MomentLike;
import com.chatroom.chatroombackend.entity.MomentLikeId;
import com.chatroom.chatroombackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MomentLikeRepository extends JpaRepository<MomentLike, MomentLikeId> {
    // New method to find a specific like
    Optional<MomentLike> findByMomentAndUser(Moment moment, User user);
}