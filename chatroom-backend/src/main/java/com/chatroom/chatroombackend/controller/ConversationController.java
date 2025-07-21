package com.chatroom.chatroombackend.controller;

import com.chatroom.chatroombackend.dto.AdminActionRequest;
import com.chatroom.chatroombackend.dto.MessageRequest;
import com.chatroom.chatroombackend.dto.ReportRequest;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.repository.UserRepository;
import com.chatroom.chatroombackend.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations") // A more RESTful base path
public class ConversationController {

    @Autowired private ConversationService conversationService;
    @Autowired private UserRepository userRepository;

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
    }

    // Invite friend to a group
    @PostMapping("/{conversationId}/invite")
    public ResponseEntity<?> inviteFriend(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId, @RequestBody AdminActionRequest request) {
        conversationService.inviteFriend(getCurrentUser(userDetails), conversationId, request.getTargetUserId());
        return ResponseEntity.ok("User invited successfully.");
    }

    // Send a message (handles both normal and reply)
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<?> sendMessage(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId, @RequestBody MessageRequest request) {
        return ResponseEntity.ok(conversationService.sendMessage(getCurrentUser(userDetails), conversationId, request));
    }

    // Withdraw a message
    @DeleteMapping("/{conversationId}/messages/{messageId}")
    public ResponseEntity<?> withdrawMessage(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId, @PathVariable Long messageId) {
        conversationService.withdrawMessage(getCurrentUser(userDetails), conversationId, messageId);
        return ResponseEntity.ok("Message withdrawn.");
    }

    // Report a message
    @PostMapping("/messages/{messageId}/report")
    public ResponseEntity<?> reportMessage(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long messageId, @RequestBody ReportRequest request) {
        conversationService.reportMessage(getCurrentUser(userDetails), messageId, request);
        return ResponseEntity.ok("Message reported successfully.");
    }

    // Report a conversation
    @PostMapping("/{conversationId}/report")
    public ResponseEntity<?> reportConversation(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId, @RequestBody ReportRequest request) {
        conversationService.reportConversation(getCurrentUser(userDetails), conversationId, request);
        return ResponseEntity.ok("Conversation reported successfully.");
    }

    // Leave a conversation
    @PostMapping("/{conversationId}/leave")
    public ResponseEntity<?> leaveConversation(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId) {
        conversationService.leaveConversation(getCurrentUser(userDetails), conversationId);
        return ResponseEntity.ok("You have left the conversation.");
    }

    // Silence a user in a group
    @PostMapping("/{conversationId}/participants/silence")
    public ResponseEntity<?> silenceUser(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId, @RequestBody AdminActionRequest request) {
        conversationService.silenceUser(getCurrentUser(userDetails), conversationId, request.getTargetUserId(), request.getSilenceUntil());
        return ResponseEntity.ok("User has been silenced.");
    }

    // Kick a user from a group
    @DeleteMapping("/{conversationId}/participants/{targetUserId}")
    public ResponseEntity<?> kickUser(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId, @PathVariable Long targetUserId) {
        conversationService.kickUser(getCurrentUser(userDetails), conversationId, targetUserId);
        return ResponseEntity.ok("User has been kicked from the group.");
    }

    // Block a user from a group
    @PostMapping("/{conversationId}/block")
    public ResponseEntity<?> blockUser(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId, @RequestBody AdminActionRequest request) {
        conversationService.blockUser(getCurrentUser(userDetails), conversationId, request.getTargetUserId(), request.getReason());
        return ResponseEntity.ok("User has been blocked and removed from the group.");
    }

    // Pin/unpin a conversation
    @PostMapping("/{conversationId}/pin")
    public ResponseEntity<?> togglePin(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId) {
        conversationService.toggleConversationPin(getCurrentUser(userDetails), conversationId);
        return ResponseEntity.ok("Conversation pin status updated.");
    }

    // Mute/unmute conversation notifications
    @PostMapping("/{conversationId}/mute")
    public ResponseEntity<?> toggleMute(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long conversationId) {
        conversationService.toggleConversationMute(getCurrentUser(userDetails), conversationId);
        return ResponseEntity.ok("Conversation notification status updated.");
    }

    @PostMapping("/{conversationId}/join")
    public ResponseEntity<?> requestToJoinGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long conversationId) {
        try {
            User currentUser = getCurrentUser(userDetails);
            conversationService.requestToJoinGroup(currentUser, conversationId);
            return ResponseEntity.ok("Your request to join the group has been sent.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
}