package com.chatroom.chatroombackend.dto;

import com.chatroom.chatroombackend.enums.RequestType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PendingRequestDto {
    // The type of request, so the frontend knows how to handle it
    private RequestType requestType;

    // The timestamp when the request was made, for sorting
    private LocalDateTime timestamp;

    // --- Information about the person who sent the request ---
    private Long requesterId;
    private String requesterNickname;
    private String requesterAvatarUrl;

    // --- Friend Request Specific ---
    // The ID needed to accept/reject this friend request
    private Long friendshipId;

    // --- Group Join Request Specific ---
    // The ID of the group being requested
    private Long conversationId;
    // The name of the group, for display
    private String conversationName;
}