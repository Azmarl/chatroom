package com.chatroom.chatroombackend.controller;

import com.chatroom.chatroombackend.dto.CreateConversationRequest;
import com.chatroom.chatroombackend.dto.ConversationSummaryDto;
import com.chatroom.chatroombackend.dto.SearchResultDto;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.repository.UserRepository;
import com.chatroom.chatroombackend.service.ConversationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    @Autowired private ConversationService conversationService;
    @Autowired private UserRepository userRepository; // 用于从 UserDetails 获取 User 实体

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
    }

    @PostMapping("/conversations")
    public ResponseEntity<?> createConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateConversationRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = getCurrentUser(userDetails);
            return ResponseEntity.ok(conversationService.createConversation(currentUser, request, httpRequest));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage()); // 403 Forbidden
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("创建失败: " + e.getMessage());
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummaryDto>> getAllConversations(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getCurrentUser(userDetails);
        List<ConversationSummaryDto> conversations = conversationService.getAllConversations(currentUser);
        return ResponseEntity.ok(conversations);
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<String> deleteConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long conversationId) {
        try {
            User currentUser = getCurrentUser(userDetails);
            conversationService.deleteConversation(currentUser, conversationId);
            return ResponseEntity.ok("聊天已删除");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage()); // 404 Not Found
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<SearchResultDto>> search(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String query) {
        User currentUser = getCurrentUser(userDetails);
        List<SearchResultDto> results = conversationService.searchFriendOrConversation(currentUser, query);
        return ResponseEntity.ok(results);
    }
}