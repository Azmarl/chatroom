package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing Comment entities.
 *
 * Inherits all standard CRUD operations from JpaRepository. Custom queries
 * can be added here if more specific comment-fetching logic is required in the future.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
}