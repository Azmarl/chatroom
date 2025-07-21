package com.chatroom.chatroombackend.service;

import com.chatroom.chatroombackend.dto.PendingRequestDto;
import com.chatroom.chatroombackend.enums.ParticipantRole;
import com.chatroom.chatroombackend.enums.RequestType;
import com.chatroom.chatroombackend.entity.ConversationParticipant;
import com.chatroom.chatroombackend.entity.Friendship;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.enums.FriendshipStatus;
import com.chatroom.chatroombackend.enums.ParticipantStatus;
import com.chatroom.chatroombackend.repository.ConversationParticipantRepository;
import com.chatroom.chatroombackend.repository.FriendshipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class PendingRequestService {

    @Autowired private FriendshipRepository friendshipRepo;
    @Autowired private ConversationParticipantRepository participantRepo;

    public List<PendingRequestDto> getAllPendingRequests(User currentUser) {
        List<PendingRequestDto> allRequests = new ArrayList<>();

        // 1. Fetch pending friend requests
        List<Friendship> friendRequests = friendshipRepo.findByFriendAndStatus(currentUser, FriendshipStatus.pending);
        for (Friendship fr : friendRequests) {
            allRequests.add(mapToDto(fr));
        }

        // 2. Fetch pending group join requests
        List<ParticipantRole> adminRoles = Arrays.asList(ParticipantRole.owner, ParticipantRole.admin);
        List<ConversationParticipant> joinRequests = participantRepo.findPendingJoinRequestsForAdmin(currentUser, ParticipantStatus.PENDING, adminRoles);
        for (ConversationParticipant cpr : joinRequests) {
            allRequests.add(mapToDto(cpr));
        }

        // 3. Sort all requests by timestamp, newest first
        allRequests.sort(Comparator.comparing(PendingRequestDto::getTimestamp).reversed());

        return allRequests;
    }

    // Helper method to map a Friendship entity to our DTO
    private PendingRequestDto mapToDto(Friendship friendship) {
        PendingRequestDto dto = new PendingRequestDto();
        dto.setRequestType(RequestType.FRIEND_REQUEST);
        dto.setTimestamp(friendship.getCreatedAt());
        dto.setFriendshipId(friendship.getId()); // This is the ID to handle the request

        User requester = friendship.getUser();
        dto.setRequesterId(requester.getId());
        dto.setRequesterNickname(requester.getNickname());
        dto.setRequesterAvatarUrl(requester.getAvatarUrl());

        return dto;
    }

    // Helper method to map a ConversationParticipant entity to our DTO
    private PendingRequestDto mapToDto(ConversationParticipant participantRequest) {
        PendingRequestDto dto = new PendingRequestDto();
        dto.setRequestType(RequestType.GROUP_JOIN_REQUEST);
        dto.setTimestamp(participantRequest.getJoinedAt());

        // These are the IDs needed to handle the request
        dto.setConversationId(participantRequest.getConversation().getId());
        dto.setRequesterId(participantRequest.getUser().getId());

        dto.setConversationName(participantRequest.getConversation().getName());

        User requester = participantRequest.getUser();
        dto.setRequesterNickname(requester.getNickname());
        dto.setRequesterAvatarUrl(requester.getAvatarUrl());

        return dto;
    }
}