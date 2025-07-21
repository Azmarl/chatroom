package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.entity.ChatroomActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing ChatroomActivityLog entities.
 *
 * This provides methods to access the monthly activity statistics for conversations,
 * which can be used by the recommendation engine to identify popular or active groups.
 */
@Repository
public interface ChatroomActivityLogRepository extends JpaRepository<ChatroomActivityLog, Long> {

    /**
     * Finds a specific activity log for a given chatroom and month.
     * The month should be in 'YYYY-MM' format.
     *
     * @param chatroomId The ID of the conversation (chatroom).
     * @param logMonth The month of the log, e.g., "2025-07".
     * @return An Optional containing the activity log if found.
     */
    Optional<ChatroomActivityLog> findByChatroomIdAndLogMonth(Long chatroomId, String logMonth);

}