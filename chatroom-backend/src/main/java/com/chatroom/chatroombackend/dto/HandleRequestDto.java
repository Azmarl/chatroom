package com.chatroom.chatroombackend.dto;

import com.chatroom.chatroombackend.enums.RequestType;
import lombok.Data;

@Data
public class HandleRequestDto {
    // Defines the type of request being handled.
    private RequestType requestType;

    // The action to perform: "ACCEPT" or "REJECT".
    private String action;

    // --- Fields for FRIEND_REQUEST ---
    // The unique ID of the pending entry in the 'friendships' table.
    private Long friendshipId;

    // --- Fields for GROUP_JOIN_REQUEST ---
    // The ID of the group the user wants to join.
    private Long conversationId;
    // The ID of the user who sent the join request.
    private Long requesterId;
}