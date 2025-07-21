package com.chatroom.chatroombackend.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UserMessageStatId implements Serializable {
    private Long userId;
    private String statMonth; // Corresponds to VARCHAR(7) 'YYYY-MM'
}