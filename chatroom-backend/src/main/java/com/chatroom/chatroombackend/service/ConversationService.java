package com.chatroom.chatroombackend.service;

import com.chatroom.chatroombackend.dto.*;
import com.chatroom.chatroombackend.entity.*;
import com.chatroom.chatroombackend.enums.*;
import com.chatroom.chatroombackend.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chatroom.chatroombackend.entity.Report;
import com.chatroom.chatroombackend.entity.Message;
import com.chatroom.chatroombackend.repository.ConversationBlockRepository;
import com.chatroom.chatroombackend.repository.MessageRepository;
import com.chatroom.chatroombackend.repository.ReportRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ConversationService {

    private static final int MAX_GROUP_CHATS_ALLOWED = 3; // 示例：每个用户最多创建5个群

    @Autowired private ConversationRepository conversationRepo;
    @Autowired private ConversationParticipantRepository participantRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private MessageRepository messageRepo;
    @Autowired private FriendshipRepository friendshipRepo;
    @Autowired private ReportRepository reportRepo;
    @Autowired private ConversationBlockRepository blockRepo;
    @Autowired private TagRepository tagRepo;
    @Autowired private GeoIpService geoIpService;

    @Transactional
    public Conversation createConversation(User creator, CreateConversationRequest request, HttpServletRequest httpRequest) {
        // 1. 检测是否超出创建上限
        long existingGroups = conversationRepo.countByOwnerId(creator.getId());
        if (existingGroups >= MAX_GROUP_CHATS_ALLOWED) {
            throw new IllegalStateException("已达到群聊创建数量上限");
        }

        Set<Tag> tags = new HashSet<>();
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tagName : request.getTags()) {
                Tag tag = tagRepo.findByName(tagName)
                        .orElseGet(() -> tagRepo.save(new Tag(tagName)));
                tags.add(tag);
            }
        }

        // Get Geolocation from server-side IP lookup
        LocationDto location = geoIpService.getLocationFromRequest(httpRequest);

        // Create Conversation entity
        Conversation conversation = new Conversation();
        conversation.setName(request.getName());
        conversation.setDescription(request.getDescription());
        conversation.setType(ConversationType.group_chat);
        conversation.setOwner(creator);
        conversation.setTags(tags); // Set tags
        conversation.setLatitude(location.getLatitude()); // Set location
        conversation.setLongitude(location.getLongitude());
        conversation.setCity(location.getCity());

        Conversation savedConversation = conversationRepo.save(conversation);

        // 3. 添加参与者
        List<ConversationParticipant> participants = new ArrayList<>();
        // 3.1 添加创建者自己
        participants.add(createParticipant(savedConversation, creator, ParticipantRole.owner, ParticipantStatus.APPROVED));

        // 3.2 添加邀请的好友
        if (request.getInitialMemberIds() != null) {
            userRepo.findAllById(request.getInitialMemberIds()).forEach(member -> {
                // 这里可以增加逻辑：检查被邀请者是否是创建者的好友
                participants.add(createParticipant(savedConversation, member, ParticipantRole.member, ParticipantStatus.APPROVED));
            });
        }
        participantRepo.saveAll(participants);
        return savedConversation;
    }

    private ConversationParticipant createParticipant(Conversation c, User u, ParticipantRole r, ParticipantStatus s) {
        ConversationParticipant p = new ConversationParticipant();
        p.setId(new ConversationParticipantId(c.getId(), u.getId()));
        p.setConversation(c);
        p.setUser(u);
        p.setRole(r);
        p.setStatus(s); // Set the status
        return p;
    }

    public List<ConversationSummaryDto> getAllConversations(User currentUser) {
        List<Conversation> conversations = conversationRepo.findAllByParticipantUserId(currentUser.getId());

        return conversations.stream().map(convo -> {
            ConversationSummaryDto dto = new ConversationSummaryDto();
            dto.setConversationId(convo.getId());
            dto.setType(convo.getType());

            Optional<Message> lastMessageOpt = messageRepo.findTopByConversationOrderByCreatedAtDesc(convo);
            lastMessageOpt.ifPresent(msg -> {
                dto.setLastMessageContent(msg.getContent());
                dto.setLastMessageTimestamp(msg.getCreatedAt());
            });

            participantRepo.findByConversationIdAndUserId(convo.getId(), currentUser.getId())
                    .ifPresent(p -> dto.setUnreadCount(p.getUnreadCount()));

            // 设置名称和头像
            if (convo.getType() == ConversationType.group_chat) {
                dto.setName(convo.getName());
                dto.setAvatarUrl(convo.getAvatarUrl());
            } else { // 私聊
                participantRepo.findAllByConversation(convo).stream()
                        .filter(p -> !p.getUser().getId().equals(currentUser.getId()))
                        .findFirst().ifPresent(other -> {
                            dto.setName(other.getUser().getNickname());
                            dto.setAvatarUrl(other.getUser().getAvatarUrl());
                        });
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void deleteConversation(User currentUser, Long conversationId) {
        ConversationParticipant participant = participantRepo.findByConversationIdAndUserId(conversationId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("你不在该聊天中"));

        // 不同的删除逻辑
        // 如果是群聊，直接退出（删除参与者记录）
        // 如果是私聊，则标记为对当前用户隐藏（更新删除历史的时间戳）
        if (participant.getConversation().getType() == ConversationType.group_chat) {
            // 注意：如果群主退出，需要处理群主转让的逻辑，这里简化为直接退出
            participantRepo.delete(participant);
        } else {
            participant.setDeletedHistoryAt(LocalDateTime.now());
            participantRepo.save(participant);
        }
    }

    public List<SearchResultDto> searchFriendOrConversation(User currentUser, String query) {
        // 搜索好友
        List<SearchResultDto> friendResults = friendshipRepo.findFriendsByUsernameOrNickname(currentUser, query)
                .stream().map(user -> {
                    SearchResultDto dto = new SearchResultDto();
                    dto.setId(user.getId());
                    dto.setName(user.getNickname());
                    dto.setAvatarUrl(user.getAvatarUrl());
                    dto.setType("user");
                    return dto;
                }).toList();

        // 搜索群聊
        List<SearchResultDto> conversationResults = conversationRepo.findGroupConversationsByName(currentUser.getId(), query)
                .stream().map(convo -> {
                    SearchResultDto dto = new SearchResultDto();
                    dto.setId(convo.getId());
                    dto.setName(convo.getName());
                    dto.setAvatarUrl(convo.getAvatarUrl());
                    dto.setType("conversation");
                    return dto;
                }).toList();

        // 合并结果
        return Stream.concat(friendResults.stream(), conversationResults.stream()).collect(Collectors.toList());
    }

    // A helper method to get a participant and verify they exist
    private ConversationParticipant getParticipant(Long conversationId, Long userId) {
        return participantRepo.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalStateException("User is not a participant of this conversation."));
    }

    // A helper method to check for Admin/Owner privileges
    private void checkAdminOrOwner(ConversationParticipant actor) {
        if (actor.getRole() != ParticipantRole.admin && actor.getRole() != ParticipantRole.owner) {
            throw new SecurityException("Permission denied. Only admins or the owner can perform this action.");
        }
    }

    @Transactional
    public void inviteFriend(User actor, Long conversationId, Long targetUserId) {
        getParticipant(conversationId, actor.getId()); // Verify actor is in the group

        Conversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));

        if (conversation.getType() != ConversationType.group_chat) {
            throw new IllegalArgumentException("Cannot invite users to a private chat.");
        }

        if (blockRepo.existsByConversationIdAndBlockedUserId(conversationId, targetUserId)) {
            throw new IllegalStateException("This user is blocked from joining this group.");
        }

        // Check if user is already a participant
        if (participantRepo.findByConversationIdAndUserId(conversationId, targetUserId).isPresent()) {
            throw new IllegalStateException("User is already a member of this group.");
        }

        User targetUser = userRepo.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found."));

        ConversationParticipant newParticipant = createParticipant(conversation, targetUser, ParticipantRole.member, ParticipantStatus.APPROVED);
        participantRepo.save(newParticipant);
    }

    @Transactional
    public Message sendMessage(User sender, Long conversationId, MessageRequest request) {
        ConversationParticipant senderParticipant = getParticipant(conversationId, sender.getId());
        if (senderParticipant.getIsMuted()) {
            throw new SecurityException("You are silenced in this group and cannot send messages.");
        }

        Message message = new Message();
        message.setSender(sender);
        message.setConversation(senderParticipant.getConversation());
        message.setContent(request.getContent());
        message.setMessageType(request.getType());

        if (request.getReplyToMessageId() != null) {
            Message repliedTo = messageRepo.findById(request.getReplyToMessageId())
                    .orElseThrow(() -> new IllegalArgumentException("Message to reply to not found."));
            message.setRepliedToMessage(repliedTo);
        }

        // In a real application, you would now update unread counts and broadcast via WebSocket
        return messageRepo.save(message);
    }

    @Transactional
    public void withdrawMessage(User actor, Long conversationId, Long messageId) {
        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found."));

        if (!message.getSender().getId().equals(actor.getId())) {
            throw new SecurityException("You can only withdraw your own messages.");
        }
        if (message.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(2))) {
            throw new IllegalStateException("Cannot withdraw messages sent more than 2 minutes ago.");
        }

        // Change to a system message instead of deleting
        message.setContent(actor.getNickname() + " withdrew a message");
        message.setMessageType(MessageType.system);
        messageRepo.save(message);
    }

    @Transactional
    public void reportMessage(User reporter, Long messageId, ReportRequest request) {
        if (!messageRepo.existsById(messageId)) throw new IllegalArgumentException("Message not found.");

        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedEntityType(ReportedEntityType.message);
        report.setReportedEntityId(messageId);
        report.setReason(request.getReason());
        reportRepo.save(report);
    }

    @Transactional
    public void reportConversation(User reporter, Long conversationId, ReportRequest request) {
        if (!conversationRepo.existsById(conversationId)) throw new IllegalArgumentException("Conversation not found.");

        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedEntityType(ReportedEntityType.conversation);
        report.setReportedEntityId(conversationId);
        report.setReason(request.getReason());
        report.setEvidenceUrl(request.getEvidenceUrl());
        reportRepo.save(report);
    }

    @Transactional
    public void leaveConversation(User user, Long conversationId) {
        ConversationParticipant participant = getParticipant(conversationId, user.getId());
        if (participant.getRole() == ParticipantRole.owner && participant.getConversation().getType() == ConversationType.group_chat) {
            // Simple policy: Owner cannot leave. More complex logic (transfer ownership) would go here.
            throw new IllegalStateException("Owner cannot leave the group. Please transfer ownership first.");
        }
        participantRepo.delete(participant);
    }

    @Transactional
    public void silenceUser(User actor, Long conversationId, Long targetUserId, LocalDateTime silenceUntil) {
        ConversationParticipant actorParticipant = getParticipant(conversationId, actor.getId());
        checkAdminOrOwner(actorParticipant);
        ConversationParticipant targetParticipant = getParticipant(conversationId, targetUserId);

        if (targetParticipant.getRole() == ParticipantRole.owner) {
            throw new SecurityException("Cannot silence the group owner.");
        }

        targetParticipant.setIsMuted(true);
        targetParticipant.setMutedUntil(silenceUntil);
        participantRepo.save(targetParticipant);
    }

    @Transactional
    public void kickUser(User actor, Long conversationId, Long targetUserId) {
        ConversationParticipant actorParticipant = getParticipant(conversationId, actor.getId());
        checkAdminOrOwner(actorParticipant);
        ConversationParticipant targetParticipant = getParticipant(conversationId, targetUserId);

        if (targetParticipant.getRole() == ParticipantRole.owner) {
            throw new SecurityException("Cannot kick the group owner.");
        }

        participantRepo.delete(targetParticipant);
    }

    @Transactional
    public void blockUser(User actor, Long conversationId, Long targetUserId, String reason) {
        // First, ensure the actor has permission and the target isn't the owner
        kickUser(actor, conversationId, targetUserId); // Reuse kick logic

        // Now, add to the block list
        Conversation conversation = conversationRepo.findById(conversationId).get();
        User targetUser = userRepo.findById(targetUserId).get();

        ConversationBlock block = new ConversationBlock();
        block.setConversation(conversation);
        block.setBlockedUser(targetUser);
        block.setBlockerUser(actor);
        block.setReason(reason);
        blockRepo.save(block);
    }

    @Transactional
    public void toggleConversationPin(User user, Long conversationId) {
        ConversationParticipant participant = getParticipant(conversationId, user.getId());
        participant.setPinned(!participant.isPinned());
        participantRepo.save(participant);
    }

    @Transactional
    public void toggleConversationMute(User user, Long conversationId) {
        ConversationParticipant participant = getParticipant(conversationId, user.getId());
        participant.setAreNotificationsMuted(!participant.isAreNotificationsMuted());
        participantRepo.save(participant);
    }

    @Transactional
    public void requestToJoinGroup(User requester, Long conversationId) {
        // 1. Validation
        Conversation group = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));

        if (group.getType() != ConversationType.group_chat) {
            throw new IllegalArgumentException("You can only request to join groups.");
        }

        if (!group.isPublic()) {
            throw new IllegalStateException("This group is private and does not accept join requests.");
        }

        if (participantRepo.findByConversationIdAndUserId(conversationId, requester.getId()).isPresent()) {
            throw new IllegalStateException("You are already a member or have a pending request for this group.");
        }

        if (blockRepo.existsByConversationIdAndBlockedUserId(conversationId, requester.getId())) {
            throw new IllegalStateException("You are blocked from joining this group.");
        }

        // 2. Create a 'pending' participant record
        ConversationParticipant joinRequest = createParticipant(group, requester, ParticipantRole.member, ParticipantStatus.PENDING);
        participantRepo.save(joinRequest);

        // In a real application, you would now send a notification to the group owner/admins.
    }

    @Transactional
    public void handleGroupJoinRequest(User actor, Long conversationId, Long requesterId, String action) {
        // 1. Authorization: Check if the current user (actor) is an admin or owner of the group
        ConversationParticipant actorParticipant = getParticipant(conversationId, actor.getId());
        checkAdminOrOwner(actorParticipant); // This helper method already throws SecurityException

        // 2. Find the pending join request from the requester
        ConversationParticipant requesterParticipant = participantRepo.findByConversationIdAndUserId(conversationId, requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Join request from the specified user not found."));

        // 3. Validation: Ensure the request is pending
        if (requesterParticipant.getStatus() != ParticipantStatus.PENDING) {
            throw new IllegalStateException("This join request has already been handled.");
        }

        // 4. Perform the action
        switch (action.toUpperCase()) {
            case "ACCEPT":
                requesterParticipant.setStatus(ParticipantStatus.APPROVED);
                participantRepo.save(requesterParticipant);
                // In a real app, send a "welcome to the group" notification
                break;
            case "REJECT":
                // Rejecting removes their pending participant record
                participantRepo.delete(requesterParticipant);
                break;
            default:
                throw new IllegalArgumentException("Invalid action. Must be 'ACCEPT' or 'REJECT'.");
        }
    }
}