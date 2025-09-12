package com.chatroom.chatroombackend.dto;

import com.chatroom.chatroombackend.entity.Message;
import com.chatroom.chatroombackend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用于通过WebSocket广播或通过API返回消息的数据传输对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

    private Long id;
    private Long conversationId;
    private String content;
    private LocalDateTime timestamp;
    private SenderDto sender;

    // (核心修改) 将 Long 类型改为一个包含更多信息的对象
    private RepliedMessageInfo repliedMessage;
    private boolean isRecalled;

    /**
     * 一个静态工厂方法，用于从 Message 实体安全地创建 MessageDto。
     */
    public static MessageDto fromEntity(Message message) {
        if (message == null) {
            return null;
        }

        // (核心修改) 如果存在被回复的消息，则创建一个预览对象
        RepliedMessageInfo repliedInfo = null;
        if (message.getRepliedToMessage() != null) {
            repliedInfo = RepliedMessageInfo.fromMessage(message.getRepliedToMessage());
        }

        // (核心修改) 如果消息已被撤回，则将内容设置为空
        String content = message.isRecalled() ? null : message.getContent();

        return new MessageDto(
                message.getId(),
                message.getConversation().getId(),
                content, // 使用处理过的内容
                message.getCreatedAt(),
                SenderDto.fromUser(message.getSender()),
                repliedInfo,
                message.isRecalled()
        );
    }

    /**
     * (核心新增) 嵌套的静态DTO，用于表示被回复消息的预览信息。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepliedMessageInfo {
        private Long messageId;
        private String senderNickname;
        private String content;

        public static RepliedMessageInfo fromMessage(Message message) {
            if (message == null) {
                return null;
            }
            return new RepliedMessageInfo(
                    message.getId(),
                    message.getSender().getNickname(),
                    message.getContent()
            );
        }
    }

    /**
     * 嵌套的静态DTO，用于表示消息发送者的信息。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenderDto {
        private Long id;
        private String nickname;
        private String avatarUrl;

        public static SenderDto fromUser(User user) {
            if (user == null) {
                return null;
            }
            return new SenderDto(
                    user.getId(),
                    user.getNickname(),
                    user.getAvatarUrl()
            );
        }
    }
}
