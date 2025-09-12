package com.chatroom.chatroombackend.service;

import com.chatroom.chatroombackend.dto.UserProfileUpdateDto;
import com.chatroom.chatroombackend.dto.UserSearchResultDto;
import com.chatroom.chatroombackend.entity.Conversation;
import com.chatroom.chatroombackend.entity.ConversationParticipant;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.repository.ConversationParticipantRepository;
import com.chatroom.chatroombackend.repository.ConversationRepository;
import com.chatroom.chatroombackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final StorageService storageService;
  private final SimpMessagingTemplate messagingTemplate;
  private final ConversationRepository conversationRepository;
  private final ConversationParticipantRepository participantRepository;

  @Transactional
  public User updateNickname(User currentUser, String newNickname) {
    if (newNickname == null || newNickname.trim().isEmpty()) {
      throw new IllegalArgumentException("Nickname cannot be empty.");
    }
    newNickname = newNickname.replaceAll("^\"|\"$", "").trim();
    currentUser.setNickname(newNickname.trim());
    broadcastProfileUpdate(currentUser);
    return userRepository.save(currentUser);
  }

  @Transactional
  public User updateAvatar(User currentUser, MultipartFile file) {
    String avatarUrl = storageService.store(file);
    currentUser.setAvatarUrl(avatarUrl);
    broadcastProfileUpdate(currentUser);
    return userRepository.save(currentUser);
  }

  private void broadcastProfileUpdate(User updatedUser) {
    // 1. 创建通知 DTO
    UserProfileUpdateDto updateDto = UserProfileUpdateDto.fromUser(updatedUser);

    // 2. 查找该用户参与的所有会话
    List<Conversation> conversations =
        conversationRepository.findConversationsByParticipantAndType(
            updatedUser.getId(), null); // 查找所有类型的会话

    // 3. 向每个会话的所有其他参与者发送通知
    conversations.forEach(
        convo ->
            conversationRepository
                .findAllUserByConversationId(convo.getId())
                .forEach(
                    participant -> {
                      // 不给自己发送通知
                      if (!participant.getUser().getId().equals(updatedUser.getId())) {
                        String destination =
                            "/topic/user/" + participant.getUser().getId() + "/profile";
                        messagingTemplate.convertAndSend(destination, updateDto);
                      }
                    }));
  }

  public UserSearchResultDto getPartnerSummaryInPrivateChat(
      User currentUser, String conversationUuid) {
    // 1. 根据UUID查找会话
    Conversation conversation =
        conversationRepository
            .findByUuid(conversationUuid)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Conversation not found with UUID: " + conversationUuid));

    // 2. 验证会话类型是否为私聊
    if (conversation.getType() != com.chatroom.chatroombackend.enums.ConversationType.PRIVATE) {
      throw new IllegalStateException("This operation is only valid for private chats.");
    }

    // 3. 从参与者中找出对方用户
    User partner =
        participantRepository.findAllByConversation(conversation).stream()
            .map(ConversationParticipant::getUser)
            .filter(user -> !user.getId().equals(currentUser.getId()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("Could not find a partner in this private chat."));

    UserSearchResultDto dto = new UserSearchResultDto();
    dto.setId(partner.getId());
    dto.setNickname(partner.getNickname());
    dto.setUsername(partner.getUsername());
    dto.setAvatarUrl(partner.getAvatarUrl());
    return dto;
  }
}
