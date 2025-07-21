package com.chatroom.chatroombackend.controller;

import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.repository.UserRepository;
import com.chatroom.chatroombackend.service.MomentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/moments") // Standard API prefix
public class MomentController {

    @Autowired private MomentService momentService;
    @Autowired private UserRepository userRepository;

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> postMoment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        try {
            User currentUser = getCurrentUser(userDetails);
            return ResponseEntity.ok(momentService.postMoment(currentUser, content, images));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{momentId}")
    public ResponseEntity<?> deleteMoment(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long momentId) {
        momentService.deleteMoment(getCurrentUser(userDetails), momentId);
        return ResponseEntity.ok("Moment deleted successfully.");
    }

    @PostMapping("/{momentId}/like")
    public ResponseEntity<?> likeMoment(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long momentId) {
        int newLikeCount = momentService.toggleLikeMoment(getCurrentUser(userDetails), momentId);
        return ResponseEntity.ok(java.util.Map.of("newLikeCount", newLikeCount));
    }

    @PostMapping("/{momentId}/comments")
    public ResponseEntity<?> commentMoment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long momentId,
            @RequestBody java.util.Map<String, String> payload) {
        String content = payload.get("content");
        Long parentCommentId = payload.get("parentCommentId") != null ? Long.parseLong(payload.get("parentCommentId")) : null;
        return ResponseEntity.ok(momentService.commentMoment(getCurrentUser(userDetails), momentId, content, parentCommentId));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long commentId) {
        momentService.deleteComment(getCurrentUser(userDetails), commentId);
        return ResponseEntity.ok("Comment deleted successfully.");
    }

    @GetMapping
    public ResponseEntity<?> getMoments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sortBy) {

        Sort sort;
        switch (sortBy.toLowerCase()) {
            case "likes":
                sort = Sort.by("likesCount").descending();
                break;
            case "comments":
                sort = Sort.by("commentsCount").descending();
                break;
//          case "tag":
//              // Tag-based sorting would require a more complex query joining with tag tables
//              sort = Sort.by("createdAt").descending();
//              break;
            default: // "latest"
                sort = Sort.by("createdAt").descending();
                break;
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(momentService.getMoments(getCurrentUser(userDetails), pageable));
    }

    @PostMapping("/{momentId}/report")
    public ResponseEntity<?> reportMoment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long momentId,
            @RequestBody java.util.Map<String, String> payload) {
        momentService.reportMoment(getCurrentUser(userDetails), momentId, payload.get("reason"));
        return ResponseEntity.ok("Moment reported successfully.");
    }

    // Exception handlers for this controller
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, IOException.class})
    public ResponseEntity<String> handleBadRequest(Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleForbidden(Exception e) {
        return ResponseEntity.status(403).body(e.getMessage());
    }
}