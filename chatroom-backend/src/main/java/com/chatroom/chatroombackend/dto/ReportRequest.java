package com.chatroom.chatroombackend.dto;

import lombok.Data;

@Data
public class ReportRequest {
    private String reason;
    private String evidenceUrl; // Optional
}