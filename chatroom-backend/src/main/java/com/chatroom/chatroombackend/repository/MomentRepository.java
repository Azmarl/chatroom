package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.entity.Moment;
import com.chatroom.chatroombackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface MomentRepository extends JpaRepository<Moment, Long> {

    long countByPosterAndCreatedAtAfter(User poster, LocalDateTime timestamp);

    // New method for fetching moments, excluding those from blocked users
    @Query("SELECT m FROM Moment m WHERE m.isDeleted = false AND m.poster.id NOT IN :blockedUserIds")
    Page<Moment> findMoments(List<Long> blockedUserIds, Pageable pageable);
}
