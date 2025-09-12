package com.chatroom.chatroombackend.controller;

import com.chatroom.chatroombackend.dto.*;
import com.chatroom.chatroombackend.entity.ConversationBlock;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.enums.ConversationStatus;
import com.chatroom.chatroombackend.repository.ConversationRepository;
import com.chatroom.chatroombackend.repository.UserRepository;
import com.chatroom.chatroombackend.service.ConversationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

  @Autowired private ConversationService conversationService;
  @Autowired private UserRepository userRepository;
  @Autowired private ConversationRepository conversationRepo;

  private User getCurrentUser(UserDetails userDetails) {
    return userRepository
        .findByUsername(userDetails.getUsername())
        .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
  }

  // Invite friend to a group
  @PostMapping("/{conversationId}/invite")
  public ResponseEntity<?> inviteFriend(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long conversationId,
      @RequestBody List<Long> targetIds) {
    for (Long targetId : targetIds) {
      conversationService.inviteFriend(getCurrentUser(userDetails), conversationId, targetId);
    }

    return ResponseEntity.ok("User invited successfully.");
  }

  /** (核心新增) 标记会话为已读的 API 端点 */
  @PostMapping("/{conversationId}/read")
  public ResponseEntity<?> markAsRead(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId) {
    User currentUser = getCurrentUser(userDetails);
    conversationService.markConversationAsRead(currentUser, conversationId);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{conversationId}/messages")
  public ResponseEntity<List<MessageDto>> getMessages(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId) {

    User currentUser = getCurrentUser(userDetails);
    List<MessageDto> messages =
        conversationService.getMessagesForConversation(conversationId, currentUser);
    return ResponseEntity.ok(messages);
  }

  // Send a message (handles both normal and reply)
  @PostMapping("/{conversationId}/messages")
  public ResponseEntity<?> sendMessage(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long conversationId,
      @RequestBody MessageRequest request) {
    return ResponseEntity.ok(
        conversationService.sendMessage(getCurrentUser(userDetails), conversationId, request));
  }

  // Withdraw a message
  @DeleteMapping("/{conversationId}/messages/{messageId}")
  public ResponseEntity<?> recallMessage(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long conversationId,
      @PathVariable Long messageId) {

    User currentUser = getCurrentUser(userDetails);
    conversationService.recallMessage(currentUser, conversationId, messageId);
    return ResponseEntity.ok().build();
  }

  // Report a message
  @PostMapping("/messages/{messageId}/report")
  public ResponseEntity<?> reportMessage(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long messageId,
      @RequestBody ReportRequest request) {
    conversationService.reportMessage(getCurrentUser(userDetails), messageId, request);
    return ResponseEntity.ok("Message reported successfully.");
  }

  // 转发消息
  @PostMapping("/forward")
  public ResponseEntity<?> forwardMessage(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody ForwardMessageRequest request) {
    User currentUser = getCurrentUser(userDetails); // 假设有此方法
    conversationService.forwardMessage(currentUser, request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{uuid}/admin")
  public ResponseEntity<?> manageAdmin(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable String uuid,
      @RequestBody AdminActionRequest request) {
    conversationService.manageAdmin(
        getCurrentUser(userDetails), uuid, request.getTargetUserId(), request.getAction());
    return ResponseEntity.ok("Admin status updated successfully.");
  }

  @GetMapping("/{uuid}/requests")
  public ResponseEntity<List<GroupJoinRequestDto>> getJoinRequests(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable String uuid) {
    // 您需要在 ConversationService 中实现 getJoinRequests 的逻辑
    List<GroupJoinRequestDto> requests =
        conversationService.getJoinRequests(getCurrentUser(userDetails), uuid);
    return ResponseEntity.ok(requests);
  }

  @PostMapping("/{uuid}/handle-request")
  public ResponseEntity<?> handleJoinRequest(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable String uuid,
      @RequestBody HandleJoinRequest request) {
    conversationService.handleGroupJoinRequest(
        getCurrentUser(userDetails),
        conversationRepo.findByUuid(uuid).get().getId(),
        request.getRequesterId(),
        request.getAction());
    return ResponseEntity.ok("Request handled successfully.");
  }

  // Report a conversation
  @PostMapping("/{conversationId}/report")
  public ResponseEntity<?> reportConversation(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long conversationId,
      @RequestBody ReportRequest request) {
    conversationService.reportConversation(getCurrentUser(userDetails), conversationId, request);
    return ResponseEntity.ok("Conversation reported successfully.");
  }

  // Leave a conversation
  @PostMapping("/{conversationId}/leave")
  public ResponseEntity<?> leaveConversation(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId) {
    conversationService.leaveConversation(getCurrentUser(userDetails), conversationId);
    return ResponseEntity.ok("You have left the conversation.");
  }

  // Silence a user in a group
  @PostMapping("/{conversationId}/participants/silence")
  public ResponseEntity<?> silenceUser(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long conversationId,
      @RequestBody AdminActionRequest request) {
    conversationService.silenceUser(
        getCurrentUser(userDetails),
        conversationId,
        request.getTargetUserId(),
        request.getSilenceUntil());
    return ResponseEntity.ok("User has been silenced.");
  }

  // Kick a user from a group
  @DeleteMapping("/{conversationId}/participants/{targetUserId}")
  public ResponseEntity<?> kickUser(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long conversationId,
      @PathVariable Long targetUserId) {
    conversationService.kickUser(getCurrentUser(userDetails), conversationId, targetUserId);
    return ResponseEntity.ok("User has been kicked from the group.");
  }

  // Block a user from a group
  @PostMapping("/{conversationId}/block")
  public ResponseEntity<?> blockUser(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long conversationId,
      @RequestBody AdminActionRequest request) {
    conversationService.blockUser(
        getCurrentUser(userDetails),
        conversationId,
        request.getTargetUserId(),
        request.getReason());
    return ResponseEntity.ok("User has been blocked and removed from the group.");
  }

  @DeleteMapping("/{conversationId}/unblock/{targetId}")
  public ResponseEntity<?> unBlockUser(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long conversationId,
      @PathVariable Long targetId) {
    conversationService.unBlockUser(getCurrentUser(userDetails), conversationId, targetId);
    return ResponseEntity.ok("User has been unBlocked from the group.");
  }

  @GetMapping("/{conversationId}/block")
  public ResponseEntity<List<ConversationBlock>> getBlackList(@PathVariable Long conversationId) {
    List<ConversationBlock> results = conversationService.getBlackList(conversationId);
    return ResponseEntity.ok(results);
  }

  // Pin/unpin a conversation
  @PostMapping("/{conversationId}/pin")
  public ResponseEntity<?> togglePin(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId) {
    conversationService.toggleConversationPin(getCurrentUser(userDetails), conversationId);
    return ResponseEntity.ok("Conversation pin status updated.");
  }

  // Mute/unmute conversation notifications
  @PostMapping("/{conversationId}/mute")
  public ResponseEntity<?> toggleMute(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId) {
    conversationService.toggleConversationMute(getCurrentUser(userDetails), conversationId);
    return ResponseEntity.ok("Conversation notification status updated.");
  }

  @GetMapping("/{conversationId}/mutes")
  public ResponseEntity<List<MutedUserDto>> getMutedList(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId) {
    List<MutedUserDto> mutedList =
        conversationService.getMutedList(getCurrentUser(userDetails), conversationId);
    return ResponseEntity.ok(mutedList);
  }

  @PostMapping("/{conversationId}/join")
  public ResponseEntity<?> requestToJoinGroup(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId) {
    try {
      User currentUser = getCurrentUser(userDetails);
      conversationService.requestToJoinGroup(currentUser, conversationId);
      return ResponseEntity.ok("Your request to join the group has been sent.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @GetMapping("/{uuid}")
  public ResponseEntity<?> getGroupDetailsByUuid(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable String uuid) {
    try {
      GroupDetailsDto groupDetails =
          conversationService.findGroupDetailsByUuid(
              uuid, getCurrentUser(userDetails).getUsername());
      return ResponseEntity.ok(groupDetails);
    } catch (IllegalArgumentException e) {
      // 如果找不到群聊或ID不是群聊类型，返回404 Not Found
      return ResponseEntity.status(404).body(e.getMessage());
    }
  }

  @GetMapping("/{uuid}/members")
  public ResponseEntity<?> getGroupMembersByUuid(@PathVariable String uuid) {
    try {
      List<GroupMemberDto> groupMembers =
          conversationService.findMembersByUuid(conversationRepo.findByUuid(uuid).get().getId());
      return ResponseEntity.ok(groupMembers);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(404).body(e.getMessage());
    }
  }

  @GetMapping("/conversations/{conversationId}/status")
  public ResponseEntity<Map<String, String>> getConversationStatus(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId) {

    User currentUser = getCurrentUser(userDetails);
    ConversationStatus status =
        conversationService.checkConversationStatus(currentUser, conversationId);

    // 将枚举名作为字符串返回给前端
    Map<String, String> response = new HashMap<>();
    response.put("status", status.name());

    return ResponseEntity.ok(response);
  }

  // A global exception handler for this controller is recommended for cleaner code
  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  public ResponseEntity<String> handleBadRequest(Exception e) {
    return ResponseEntity.badRequest().body(e.getMessage());
  }

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<String> handleForbidden(Exception e) {
    return ResponseEntity.status(403).body(e.getMessage());
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<String> handleNotFound(EntityNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
  }
}
