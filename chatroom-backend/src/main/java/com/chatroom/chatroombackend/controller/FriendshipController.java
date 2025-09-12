package com.chatroom.chatroombackend.controller;

import com.chatroom.chatroombackend.dto.GroupDetailsDto;
import com.chatroom.chatroombackend.dto.UserSearchResultDto;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.repository.ConversationRepository;
import com.chatroom.chatroombackend.repository.UserRepository;
import com.chatroom.chatroombackend.service.ConversationService;
import com.chatroom.chatroombackend.service.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RestController
@RequestMapping("/api/friendships")
public class FriendshipController {

  @Autowired private FriendshipService friendshipService;
  @Autowired private UserRepository userRepository;
  @Autowired private ConversationService conversationService;
  @Autowired private SimpMessagingTemplate messagingTemplate;
  @Autowired private ConversationRepository conversationRepo;

  private User getCurrentUser(UserDetails userDetails) {
    return userRepository
        .findByUsername(userDetails.getUsername())
        .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
  }

  @GetMapping("/{targetId}/delete")
  public ResponseEntity<Long> getConversationIdToDelete(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long targetId) {
    User currentUser = getCurrentUser(userDetails);
    return conversationRepo.getConversationIdByUserAndTargetId(currentUser.getId(), targetId);
  }

  @PostMapping("/request")
  public ResponseEntity<?> sendFriendRequest(
      @AuthenticationPrincipal UserDetails userDetails, @RequestBody String targetUsername) {
    try {
      User currentUser = getCurrentUser(userDetails);
      friendshipService.sendFriendRequest(currentUser, targetUsername);
      return ResponseEntity.ok("Friend request sent successfully.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @GetMapping("/search")
  public ResponseEntity<List<UserSearchResultDto>> searchUsers(
      @AuthenticationPrincipal UserDetails userDetails, @RequestParam("query") String query) {

    if (query == null || query.trim().isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    User currentUser = getCurrentUser(userDetails);
    List<UserSearchResultDto> results = friendshipService.searchUsersByUsername(currentUser, query);

    return ResponseEntity.ok(results);
  }

  /**
   * (核心新增) 获取当前登录用户的好友列表。
   *
   * @param userDetails 由Spring Security提供的当前用户信息
   * @return 包含好友列表的ResponseEntity
   */
  @GetMapping("/friends")
  public ResponseEntity<List<UserSearchResultDto>> getFriendList(
      @AuthenticationPrincipal UserDetails userDetails) {
    // 1. 获取当前用户实体
    User currentUser = getCurrentUser(userDetails); // 假设您有这个辅助方法

    // 2. 调用服务层获取好友列表
    List<UserSearchResultDto> friends = friendshipService.getFriends(currentUser);

    // 3. 返回成功响应
    return ResponseEntity.ok(friends);
  }

  @GetMapping("/groups")
  public ResponseEntity<List<GroupDetailsDto>> getJoinedGroups(
      @AuthenticationPrincipal UserDetails userDetails) {
    // 1. 获取当前用户实体
    User currentUser = getCurrentUser(userDetails); // 假设您有这个辅助方法

    // 2. 调用服务层获取群聊列表
    List<GroupDetailsDto> groups = conversationService.getJoinedGroups(currentUser);

    // 3. 返回成功响应
    return ResponseEntity.ok(groups);
  }

  @DeleteMapping("/{friendId}")
  public ResponseEntity<?> deleteFriend(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long friendId) {
    try {
      User currentUser = getCurrentUser(userDetails);

      // 1. 删除好友关系
      friendshipService.deleteFriendship(currentUser, friendId);

      // 2. 查找并删除私聊会话
      conversationService.deletePrivateConversation(currentUser, friendId);

      // 3. 通过WebSocket通知对方
      messagingTemplate.convertAndSendToUser(
          friendId.toString(),
          "/queue/friend-removed",
          Map.of("removerId", currentUser.getId(), "removerName", currentUser.getUsername()));

      return ResponseEntity.ok("Friend and conversation deleted successfully.");
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to delete friend: " + e.getMessage());
    }
  }
}
