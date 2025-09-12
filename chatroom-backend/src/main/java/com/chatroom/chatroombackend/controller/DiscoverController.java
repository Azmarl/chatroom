package com.chatroom.chatroombackend.controller;

import com.chatroom.chatroombackend.dto.ConversationRecommendationDto;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.repository.UserRepository;
import com.chatroom.chatroombackend.service.RecommendationService;
import com.chatroom.chatroombackend.service.TagService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RestController
@RequestMapping("/api/discover")
public class DiscoverController {

    @Autowired private RecommendationService recommendationService;
    @Autowired private UserRepository userRepository;
    @Autowired private TagService tagService;

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<ConversationRecommendationDto>> getRecommendations(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        User currentUser = getCurrentUser(userDetails);
        List<ConversationRecommendationDto> recommendations = recommendationService.recommendConversations(currentUser, request);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/tags")
    public ResponseEntity<List<String>> getAllTags() {
        List<String> tags = tagService.getAllTagNames();
        return ResponseEntity.ok(tags);
    }
}