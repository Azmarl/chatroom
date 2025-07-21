package com.chatroom.chatroombackend.dto;

import lombok.Data;

@Data
public class ConversationRecommendationDto {
    private Long conversationId;
    private String name;
    private String avatarUrl;
    private String description;
    private String reason; // e.g., "Matches your interests", "Popular in your city"
    private double score; // The internal relevance score
}