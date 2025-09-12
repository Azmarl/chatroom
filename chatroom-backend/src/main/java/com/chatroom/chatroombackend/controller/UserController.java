package com.chatroom.chatroombackend.controller;

import com.chatroom.chatroombackend.dto.HandleRequestDto;
import com.chatroom.chatroombackend.dto.LoginResponse;
import com.chatroom.chatroombackend.dto.PendingRequestDto;
import com.chatroom.chatroombackend.dto.UserSearchResultDto;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.repository.UserRepository;
import com.chatroom.chatroombackend.service.ConversationService;
import com.chatroom.chatroombackend.service.FriendshipService;
import com.chatroom.chatroombackend.service.PendingRequestService;
import com.chatroom.chatroombackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired private FriendshipService friendshipService;
    @Autowired private ConversationService conversationService;
    @Autowired private UserRepository userRepository;
    @Autowired private PendingRequestService pendingRequestService;
    @Autowired private UserService userService;

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    /**
     * A single endpoint to handle various incoming requests like friend requests
     * and group join requests.
     */
    @PostMapping("/handle-request")
    public ResponseEntity<?> handleRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody HandleRequestDto request) {

        try {
            User currentUser = getCurrentUser(userDetails);

            switch (request.getRequestType()) {
                case FRIEND_REQUEST:
                    if (request.getFriendshipId() == null) {
                        return ResponseEntity.badRequest().body("friendshipId is required for FRIEND_REQUEST.");
                    }
                    friendshipService.handleFriendRequest(currentUser, request.getFriendshipId(), request.getAction());
                    break;

                case GROUP_JOIN_REQUEST:
                    if (request.getConversationId() == null || request.getRequesterId() == null) {
                        return ResponseEntity.badRequest().body("conversationId and requesterId are required for GROUP_JOIN_REQUEST.");
                    }
                    conversationService.handleGroupJoinRequest(currentUser, request.getConversationId(), request.getRequesterId(), request.getAction());
                    break;

                default:
                    return ResponseEntity.badRequest().body("Invalid requestType.");
            }

            return ResponseEntity.ok("Request handled successfully: " + request.getAction().toLowerCase() + "ed.");

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage()); // 403 Forbidden
        }
    }

    /**
     * Fetches a combined list of all pending friend requests and group join requests
     * for the currently authenticated user.
     */
    @GetMapping("/pending-requests")
    public ResponseEntity<List<PendingRequestDto>> getPendingRequests(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getCurrentUser(userDetails);
        List<PendingRequestDto> requests = pendingRequestService.getAllPendingRequests(currentUser);
        return ResponseEntity.ok(requests);
    }

    @PutMapping("/profile")
    public ResponseEntity<LoginResponse.UserInfo> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody String nickname) {
        User currentUser = getCurrentUser(userDetails);
        User updatedUser = userService.updateNickname(currentUser, nickname);
        return ResponseEntity.ok(LoginResponse.UserInfo.fromUser(updatedUser));
    }

    @PostMapping("/avatar")
    public ResponseEntity<LoginResponse.UserInfo> uploadAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("avatar") MultipartFile file) {
        User currentUser = getCurrentUser(userDetails);
        User updatedUser = userService.updateAvatar(currentUser, file);
        return ResponseEntity.ok(LoginResponse.UserInfo.fromUser(updatedUser));
    }

    @GetMapping("/summary/{uuid}")
    public ResponseEntity<UserSearchResultDto> getUserSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("uuid") String conversationUuid) {

        User currentUser = getCurrentUser(userDetails);
        UserSearchResultDto partnerSummary = userService.getPartnerSummaryInPrivateChat(currentUser, conversationUuid);
        return ResponseEntity.ok(partnerSummary);
    }
}
