package com.chatroom.chatroombackend.service;

import com.chatroom.chatroombackend.dto.ConversationRecommendationDto;
import com.chatroom.chatroombackend.dto.LocationDto;
import com.chatroom.chatroombackend.entity.Conversation;
import com.chatroom.chatroombackend.entity.Tag;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    @Autowired private ConversationRepository conversationRepo;
    @Autowired private ConversationParticipantRepository participantRepo;
    @Autowired private FriendshipRepository friendshipRepo;
    @Autowired private ChatroomActivityLogRepository activityLogRepo;
    @Autowired private GeoIpService geoIpService;

    // Decide if a user is "new" based on number of conversations joined
    private static final int NEW_USER_THRESHOLD = 2;
    private static final int RECOMMENDATION_COUNT = 5;

    public List<ConversationRecommendationDto> recommendConversations(User user, HttpServletRequest request) {
        long conversationsJoined = participantRepo.countByUser(user);

        List<Conversation> candidates;
        if (conversationsJoined <= NEW_USER_THRESHOLD) {
            candidates = recommendForNewUser(user, request);
        } else {
            candidates = recommendForOldUser(user);
        }

        // Convert to DTOs and limit the results
        return candidates.stream()
                .limit(RECOMMENDATION_COUNT)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private List<Conversation> recommendForNewUser(User user, HttpServletRequest request) {
        LocationDto userLocation = geoIpService.getLocationFromRequest(request);
        Set<Tag> userInterests = user.getInterestedTags();

        // Get a pool of all public groups the user is NOT in
        List<Conversation> allGroups = conversationRepo.findAllPublicGroupsNotIn(user.getId());

        Map<Conversation, Double> scoredGroups = new HashMap<>();
        for (Conversation group : allGroups) {
            double score = 0;
            String reason = "";

            // Score based on matching tags
            long matchingTags = group.getTags().stream().filter(userInterests::contains).count();
            if (matchingTags > 0) {
                score += matchingTags * 50;
                reason += matchingTags + " matching interests. ";
            }

            // Score based on geolocation
            if (group.getCity() != null && group.getCity().equals(userLocation.getCity())) {
                score += 100;
                reason += "In your city. ";
            }

            // Score based on activity (simplified)
            // In a real app, you'd fetch from chatroom_activity_logs
            score += participantRepo.countByConversation(group); // Simple activity metric: member count

            if (score > 0) {
                scoredGroups.put(group, score);
            }
        }
        return sortAndExtract(scoredGroups);
    }

    private List<Conversation> recommendForOldUser(User user) {
        // Get tags from groups user is already in
        Set<Tag> existingTags = participantRepo.findAllByUser(user).stream()
                .flatMap(p -> p.getConversation().getTags().stream())
                .collect(Collectors.toSet());

        // Get user's friends
        List<User> friends = friendshipRepo.findAllFriends(user);

        List<Conversation> allGroups = conversationRepo.findAllPublicGroupsNotIn(user.getId());

        Map<Conversation, Double> scoredGroups = new HashMap<>();
        for (Conversation group : allGroups) {
            double score = 0;
            String reason = "";

            // Score based on similar tags
            long matchingTags = group.getTags().stream().filter(existingTags::contains).count();
            if (matchingTags > 0) {
                score += matchingTags * 30;
                reason += matchingTags + " similar interests. ";
            }

            // Score based on friends being in the group
            long friendsInGroup = friends.stream()
                    .filter(friend -> participantRepo.findByConversationIdAndUserId(group.getId(), friend.getId()).isPresent())
                    .count();
            if (friendsInGroup > 0) {
                score += friendsInGroup * 70;
                reason += friendsInGroup + " friends are in this group. ";
            }

            if (score > 0) {
                scoredGroups.put(group, score);
            }
        }
        return sortAndExtract(scoredGroups);
    }

    // Helper to sort the map by score and return a list of conversations
    private List<Conversation> sortAndExtract(Map<Conversation, Double> scoredMap) {
        return scoredMap.entrySet().stream()
                .sorted(Map.Entry.<Conversation, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Helper to map entity to DTO
    private ConversationRecommendationDto mapToDto(Conversation conversation) {
        ConversationRecommendationDto dto = new ConversationRecommendationDto();
        dto.setConversationId(conversation.getId());
        dto.setName(conversation.getName());
        dto.setAvatarUrl(conversation.getAvatarUrl());
        dto.setDescription(conversation.getDescription());
        // In a real app, the 'reason' would be calculated and stored alongside the score
        dto.setReason("Highly relevant to you");
        return dto;
    }
}
// You would need to add the new repository methods in ConversationRepository and ConversationParticipantRepository