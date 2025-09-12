package com.chatroom.chatroombackend.service;

import com.chatroom.chatroombackend.dto.*;
import com.chatroom.chatroombackend.entity.*;
import com.chatroom.chatroombackend.enums.*;
import com.chatroom.chatroombackend.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chatroom.chatroombackend.entity.Report;
import com.chatroom.chatroombackend.entity.Message;
import com.chatroom.chatroombackend.repository.ConversationBlockRepository;
import com.chatroom.chatroombackend.repository.MessageRepository;
import com.chatroom.chatroombackend.repository.ReportRepository;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

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
  @Autowired private SimpMessagingTemplate messagingTemplate;
  @Autowired private ConversationParticipantRepository conversationParticipantRepo;
  @Autowired private UserBlockRepository userBlockRepository;
  @Autowired private ConversationBlockRepository conversationBlockRepos;

  /**
   * 生成一个唯一的11位数字UUID。 该方法会循环生成数字，直到找到一个在数据库中不存在的数字为止，以确保唯一性。
   *
   * @return 唯一的11位数字字符串
   */
  private String generateUniqueRandom11DigitNumber() {
    String uniqueId;
    do {
      // 生成一个11位数的随机数字
      long randomNum = ThreadLocalRandom.current().nextLong(10_000_000_000L, 100_000_000_000L);
      uniqueId = String.valueOf(randomNum);
    } while (conversationRepo.findByUuid(uniqueId).isPresent()); // 检查数据库中是否已存在
    return uniqueId;
  }

  @Transactional
  public Conversation createConversation(
      User creator, CreateConversationRequest request, HttpServletRequest httpRequest) {
    // 1. 检测是否超出创建上限
    long existingGroups = conversationRepo.countByOwnerId(creator.getId());
    if (existingGroups >= MAX_GROUP_CHATS_ALLOWED) {
      throw new IllegalStateException("已达到群聊创建数量上限");
    }

    Set<Tag> tags = new HashSet<>();
    if (request.getTags() != null && !request.getTags().isEmpty()) {
      for (String tagName : request.getTags()) {
        Tag tag = tagRepo.findByName(tagName).orElseGet(() -> tagRepo.save(new Tag(tagName)));
        tags.add(tag);
      }
    }

    // Get Geolocation from server-side IP lookup
    LocationDto location = geoIpService.getLocationFromRequest(httpRequest);

    // Create Conversation entity
    Conversation conversation = new Conversation();
    conversation.setName(request.getName());
    conversation.setDescription(request.getDescription());
    conversation.setType(ConversationType.GROUP);
    conversation.setOwner(creator);
    conversation.setTags(tags); // Set tags
    conversation.setLatitude(location.getLatitude()); // Set location
    conversation.setLongitude(location.getLongitude());
    conversation.setCity(location.getCity());

    // 新增的核心逻辑：生成唯一的11位数字ID并设置
    String newUuid = generateUniqueRandom11DigitNumber();
    conversation.setUuid(newUuid);

    Conversation savedConversation = conversationRepo.save(conversation);

    // 3. 添加参与者
    List<ConversationParticipant> participants = new ArrayList<>();
    // 3.1 添加创建者自己
    participants.add(
        createParticipant(
            savedConversation, creator, ParticipantRole.owner, ParticipantStatus.APPROVED));

    // 3.2 添加邀请的好友
    if (!request.getMemberIds().isEmpty()) {
      userRepo
          .findAllById(request.getMemberIds())
          .forEach(
              member -> {
                // 这里可以增加逻辑：检查被邀请者是否是创建者的好友
                participants.add(
                    createParticipant(
                        savedConversation,
                        member,
                        ParticipantRole.member,
                        ParticipantStatus.APPROVED));
              });
    }

    // (核心新增) 生成并设置群聊头像
    try {
      // 1. 获取完整的成员 User 对象列表 (包括创建者)
      List<User> members = null;
      if (!request.getMemberIds().isEmpty()) {
        members = userRepo.findAllById(request.getMemberIds());
      }
      if (members != null && !members.contains(creator)) {
        members.add(creator);
      }

      // 2. 调用头像生成方法
      String avatarFileName = null;
      if (members != null) {
        avatarFileName = generateGroupAvatar(members);
      }

      // 3. 构建头像的完整URL并设置给群聊
      // String avatarUrl = appConfig.getServerUrl() + "/uploads/" + avatarFileName;
      String avatarUrl = "http://localhost:8080/uploads/" + avatarFileName; // 示例URL
      savedConversation.setAvatarUrl(avatarUrl);

    } catch (IOException e) {
      // 如果头像生成失败，可以设置一个默认头像或记录日志，但不应中断整个创建流程
      System.err.println("Failed to generate group avatar: " + e.getMessage());
      savedConversation.setAvatarUrl("http://localhost:8080/uploads/default_group_avatar.png");
    }

    participantRepo.saveAll(participants);
    return savedConversation;
  }

  @Transactional
  public ConversationParticipant createParticipant(
      Conversation c, User u, ParticipantRole r, ParticipantStatus s) {
    ConversationParticipant p = new ConversationParticipant();
    p.setId(new ConversationParticipantId(c.getId(), u.getId()));
    p.setConversation(c);
    p.setUser(u);
    p.setRole(r);
    p.setStatus(s);
    p.setUnreadCount(0);
    p.setJoinedAt(LocalDateTime.now()); // 确保设置加入时间
    p.setPinned(false); // 默认不置顶
    p.setAreNotificationsMuted(false); // 默认不静音
    p.setIsMuted(false); // 默认不禁言
    return p;
  }

  /**
   * (核心新增) 查找或创建一个与指定伙伴的私聊会话。
   *
   * @param currentUser 当前登录用户
   * @param partnerId 聊天伙伴的用户ID
   * @return 找到的或新创建的会话实体
   */
  @Transactional
  public ConversationSummaryDto findOrCreatePrivateConversation(User currentUser, Long partnerId) {
    if (currentUser.getId().equals(partnerId)) {
      throw new IllegalArgumentException("Cannot create a private chat with yourself.");
    }

    // 1. 查找对方用户是否存在
    User partner =
        userRepo
            .findById(partnerId)
            .orElseThrow(() -> new IllegalArgumentException("Partner user not found."));

    // 2. 使用新的查询方法，查找是否已存在这两个人之间的私聊
    Optional<Conversation> existingConversation =
        conversationRepo.findPrivateConversationBetweenUsers(currentUser.getId(), partnerId);

    ConversationSummaryDto dto = new ConversationSummaryDto();

    if (existingConversation.isPresent()) {
      Conversation c = existingConversation.get();
      ConversationParticipant currentParticipant =
          participantRepo.findByConversationIdAndUserId(c.getId(), currentUser.getId()).get();
      Message m = messageRepo.findTopByConversationOrderByCreatedAtDesc(c).get();

      dto.setConversationId(c.getId());
      dto.setType(ConversationType.PRIVATE);
      dto.setName(partner.getNickname());
      dto.setAvatarUrl(partner.getAvatarUrl());
      if (currentParticipant.getDeletedHistoryAt().isAfter(m.getCreatedAt())) {
        dto.setLastMessageContent(" ");
        dto.setLastMessageTimestamp(null);
      } else {
        dto.setLastMessageContent(m.getContent());
        dto.setLastMessageTimestamp(m.getCreatedAt());
      }
      dto.setUnreadCount(currentParticipant.getUnreadCount());
      dto.setPinned(currentParticipant.isPinned());
      dto.setNotificationsMuted(currentParticipant.isAreNotificationsMuted());
      return dto;
    }

    // 3. 如果不存在，则创建新的私聊会话
    Conversation newConversation = new Conversation();
    newConversation.setUuid(UUID.randomUUID().toString());
    newConversation.setType(ConversationType.PRIVATE);
    newConversation.setPublic(false); // 私聊默认不公开
    newConversation.setCreatedAt(LocalDateTime.now());
    newConversation.setUpdatedAt(LocalDateTime.now());
    newConversation.setIsActive(true);
    // 对于私聊，name, description, avatar, owner 等字段可以为 null

    Conversation savedConversation = conversationRepo.save(newConversation);

    // 4. 将两个用户都添加为参与者
    ConversationParticipant currentUserParticipant =
        new ConversationParticipant(savedConversation, currentUser);
    currentUserParticipant.setRole(ParticipantRole.member); // 私聊中双方都是成员
    currentUserParticipant.setStatus(ParticipantStatus.APPROVED);
    currentUserParticipant.setJoinedAt(LocalDateTime.now());

    ConversationParticipant partnerParticipant =
        new ConversationParticipant(savedConversation, partner);
    partnerParticipant.setRole(ParticipantRole.member);
    partnerParticipant.setStatus(ParticipantStatus.APPROVED);
    partnerParticipant.setJoinedAt(LocalDateTime.now());

    participantRepo.saveAll(List.of(currentUserParticipant, partnerParticipant));

    dto.setConversationId(newConversation.getId());
    dto.setType(ConversationType.PRIVATE);
    dto.setName(partner.getNickname());
    dto.setAvatarUrl(partner.getAvatarUrl());
    dto.setLastMessageContent(null);
    dto.setLastMessageTimestamp(null);
    dto.setUnreadCount(0);
    return dto;
  }

  /**
   * (New Method) Gets the summary for a single conversation by its ID. Ensures the requesting user
   * is a member of the conversation.
   *
   * @param conversationId The ID of the conversation to fetch.
   * @param currentUser The user making the request.
   * @return A ConversationSummaryDto for the requested conversation.
   */
  @Transactional(readOnly = true)
  public ConversationSummaryDto getConversationSummaryById(Long conversationId, User currentUser) {
    // 1. Find the conversation
    Conversation conversation =
        conversationRepo
            .findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));

    // 2. Security Check: Ensure the user is a participant
    participantRepo
        .findByConversationIdAndUserId(conversationId, currentUser.getId())
        .orElseThrow(() -> new AccessDeniedException("You are not a member of this conversation."));

    // 3. Use the helper method to build and return the DTO
    return mapToConversationSummaryDto(conversation, currentUser);
  }

  @Transactional
  public List<ConversationSummaryDto> getAllConversations(User currentUser) {
    List<Conversation> conversations =
        conversationRepo.findAllByParticipantUserId(currentUser.getId());
    return conversations.stream()
        .map(convo -> mapToConversationSummaryDto(convo, currentUser)) // Use the new helper method
        .collect(Collectors.toList());
  }

  @Transactional
  public void deleteConversation(User currentUser, Long conversationId) {
    ConversationParticipant participant =
        participantRepo
            .findByConversationIdAndUserId(conversationId, currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("你不在该聊天中"));

    // 标记为对当前用户隐藏（更新删除历史的时间戳）
    participant.setDeletedHistoryAt(LocalDateTime.now());
    participantRepo.save(participant);
  }

  @Transactional
  public List<SearchResultDto> searchFriendOrConversation(User currentUser, String query) {
    // 搜索好友
    List<SearchResultDto> friendResults =
        friendshipRepo.findFriendsByUsernameOrNickname(currentUser, query).stream()
            .map(
                user -> {
                  SearchResultDto dto = new SearchResultDto();
                  dto.setId(user.getId());
                  dto.setName(user.getNickname());
                  dto.setAvatarUrl(user.getAvatarUrl());
                  dto.setType("user");
                  return dto;
                })
            .toList();

    // 搜索群聊
    List<SearchResultDto> conversationResults =
        conversationRepo.findGroupConversationsByName(currentUser.getId(), query).stream()
            .map(
                convo -> {
                  SearchResultDto dto = new SearchResultDto();
                  dto.setId(convo.getId());
                  dto.setName(convo.getName());
                  dto.setAvatarUrl(convo.getAvatarUrl());
                  dto.setType("conversation");
                  return dto;
                })
            .toList();

    // 合并结果
    return Stream.concat(friendResults.stream(), conversationResults.stream())
        .collect(Collectors.toList());
  }

  // A helper method to get a participant and verify they exist
  @Transactional
  public ConversationParticipant getParticipant(Long conversationId, Long userId) {
    return participantRepo
        .findByConversationIdAndUserId(conversationId, userId)
        .orElseThrow(
            () -> new IllegalStateException("User is not a participant of this conversation."));
  }

  // A helper method to check for Admin/Owner privileges
  @Transactional
  public void checkAdminOrOwner(ConversationParticipant actor) {
    if (actor.getRole() != ParticipantRole.admin && actor.getRole() != ParticipantRole.owner) {
      throw new SecurityException(
          "Permission denied. Only admins or the owner can perform this action.");
    }
  }

  @Transactional
  public void inviteFriend(User actor, Long conversationId, Long targetUserId) {
    getParticipant(conversationId, actor.getId()); // Verify actor is in the group

    Conversation conversation =
        conversationRepo
            .findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));

    if (conversation.getType() != ConversationType.GROUP) {
      throw new IllegalArgumentException("Cannot invite users to a private chat.");
    }

    if (blockRepo.existsByConversationIdAndBlockedUserId(conversationId, targetUserId)) {
      throw new IllegalStateException("This user is blocked from joining this group.");
    }

    // Check if user is already a participant
    if (participantRepo.findByConversationIdAndUserId(conversationId, targetUserId).isPresent()) {
      throw new IllegalStateException("User is already a member of this group.");
    }

    User targetUser =
        userRepo
            .findById(targetUserId)
            .orElseThrow(() -> new IllegalArgumentException("Target user not found."));

    ConversationParticipant newParticipant =
        createParticipant(
            conversation, targetUser, ParticipantRole.member, ParticipantStatus.APPROVED);
    participantRepo.save(newParticipant);

    // 发送 WebSocket 通知给被邀请的用户
    sendGroupInvitationNotification(targetUser, conversation, actor);
  }

  private void sendGroupInvitationNotification(
      User targetUser, Conversation conversation, User inviter) {
    // 创建通知对象
    Map<String, Object> notification = new HashMap<>();
    notification.put("type", "GROUP_INVITATION");
    notification.put("groupId", conversation.getId());
    notification.put("groupUuid", conversation.getUuid());
    notification.put("groupName", conversation.getName());
    notification.put("inviterId", inviter.getId());
    notification.put("inviterName", inviter.getNickname());
    notification.put("timestamp", System.currentTimeMillis());

    messagingTemplate.convertAndSendToUser(
        targetUser.getId().toString(), "/queue/notifications", notification);
  }

  @Transactional
  public MessageDto sendMessage(User sender, Long conversationId, MessageRequest request) {
    ConversationParticipant senderParticipant = getParticipant(conversationId, sender.getId());
    if (senderParticipant.getIsMuted()) {
      throw new SecurityException("You are silenced in this group and cannot send messages.");
    }

    Message message = new Message();
    message.setSender(sender);
    message.setConversation(senderParticipant.getConversation());
    message.setContent(request.getContent());
    message.setMessageType(request.getType());
    message.setCreatedAt(LocalDateTime.now());

    if (request.getReplyToMessageId() != null) {
      Message repliedTo =
          messageRepo
              .findById(request.getReplyToMessageId())
              .orElseThrow(() -> new IllegalArgumentException("Message to reply to not found."));
      message.setRepliedToMessage(repliedTo);
    }

    // In a real application, you would now update unread counts and broadcast via WebSocket
    Message savedMessage = messageRepo.save(message);

    // (核心新增) 更新其他参与者的未读计数
    // 1. 找到除发送者外的所有参与者
    List<ConversationParticipant> otherParticipants =
        participantRepo.findByConversationIdAndUserIdNot(conversationId, sender.getId());

    // 2. 遍历并增加他们的 unread_count
    for (ConversationParticipant participant : otherParticipants) {
      int currentUnread = participant.getUnreadCount() != null ? participant.getUnreadCount() : 0;
      participant.setUnreadCount(currentUnread + 1);
    }
    participantRepo.saveAll(otherParticipants);

    // 2. 将保存后的消息转换为 DTO
    MessageDto messageDto = MessageDto.fromEntity(savedMessage);

    // 3. (核心新增) 定义广播目的地并发送消息
    String destination = "/topic/conversations/" + conversationId;
    messagingTemplate.convertAndSend(destination, messageDto);

    // 4. 将 DTO 返回给原始的 HTTP 请求方
    return messageDto;
  }

  /**
   * (核心新增) 将一个会话标记为已读。
   *
   * @param user 当前用户
   * @param conversationId 要标记为已读的会话ID
   */
  @Transactional
  public void markConversationAsRead(User user, Long conversationId) {
    // 1. 找到当前用户在该会话中的参与记录
    ConversationParticipant participant =
        participantRepo
            .findByConversationIdAndUserId(conversationId, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Participant not found."));

    // 2. 将未读计数清零
    participant.setUnreadCount(0);

    // 3. (可选但推荐) 更新 last_read_message_id
    //    找到这个会话中最新的一条消息
    messageRepo
        .findTopByConversationIdOrderByCreatedAtDesc(conversationId)
        .ifPresent(lastMessage -> participant.setLastReadMessageId(lastMessage.getId()));

    // 4. 保存更新
    participantRepo.save(participant);
  }

  @Transactional
  public void recallMessage(User currentUser, Long conversationId, Long messageId) {
    Message message =
        messageRepo
            .findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("Message not found."));

    // 1. 权限校验：确保是消息发送者本人
    if (!message.getSender().getId().equals(currentUser.getId())) {
      throw new SecurityException("You can only recall your own messages.");
    }

    // 2. (可选) 时间校验：例如，只允许撤回2分钟内的消息
    if (Duration.between(message.getCreatedAt(), LocalDateTime.now()).toMinutes() > 2) {
      throw new IllegalStateException("You can no longer recall this message.");
    }

    // 3. 标记为已撤回并保存
    message.setRecalled(true);
    messageRepo.save(message);

    // 4. (重要) 通过WebSocket广播“消息已撤回”的通知
    // 我们需要一个新的DTO来广播这个事件
    RecallNotificationDto notification = new RecallNotificationDto(conversationId, messageId);
    messagingTemplate.convertAndSend(
        "/topic/conversations/" + conversationId + "/recalls", notification);
  }

  @Transactional
  public void reportMessage(User reporter, Long messageId, ReportRequest request) {
    if (!messageRepo.existsById(messageId))
      throw new IllegalArgumentException("Message not found.");

    Report report = new Report();
    report.setReporter(reporter);
    report.setReportedEntityType(ReportedEntityType.message);
    report.setReportedEntityId(messageId);
    report.setReason(request.getReason());
    reportRepo.save(report);
  }

  @Transactional
  public void reportConversation(User reporter, Long conversationId, ReportRequest request) {
    if (!conversationRepo.existsById(conversationId))
      throw new IllegalArgumentException("Conversation not found.");

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
    if (participant.getRole() == ParticipantRole.owner
        && participant.getConversation().getType() == ConversationType.GROUP) {
      // Simple policy: Owner cannot leave. More complex logic (transfer ownership) would go here.
      throw new IllegalStateException(
          "Owner cannot leave the group. Please transfer ownership first.");
    }
    participantRepo.delete(participant);
  }

  @Transactional
  public void silenceUser(
      User actor, Long conversationId, Long targetUserId, LocalDateTime silenceUntil) {
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
  public void manageAdmin(User currentUser, String uuid, Long targetUserId, String action) {
    Conversation conversation =
        conversationRepo
            .findByUuid(uuid)
            .orElseThrow(() -> new IllegalArgumentException("Group not found."));
    User targetUser =
        userRepo
            .findById(targetUserId)
            .orElseThrow(() -> new IllegalArgumentException("Target user not found."));

    // 权限校验：只有群主(OWNER)才能设置管理员
    ConversationParticipant currentUserParticipant =
        getParticipant(conversation.getId(), currentUser.getId());
    if (currentUserParticipant.getRole() != ParticipantRole.owner) {
      throw new SecurityException("Only the group owner can manage admins.");
    }

    ConversationParticipant targetParticipant =
        getParticipant(conversation.getId(), targetUser.getId());

    // 不能对自己或已经是群主的人操作
    if (targetParticipant.getRole() == ParticipantRole.owner) {
      throw new IllegalStateException("Cannot change the owner's role.");
    }

    // 执行操作
    if ("PROMOTE".equalsIgnoreCase(action)) {
      targetParticipant.setRole(ParticipantRole.admin);
    } else if ("DEMOTE".equalsIgnoreCase(action)) {
      targetParticipant.setRole(ParticipantRole.member);
    } else {
      throw new IllegalArgumentException("Invalid action specified.");
    }

    participantRepo.save(targetParticipant);
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
    Conversation group =
        conversationRepo
            .findByUuid(String.valueOf(conversationId))
            .orElseThrow(() -> new IllegalArgumentException("Group not found."));

    if (group.getType() != ConversationType.GROUP) {
      throw new IllegalArgumentException("You can only request to join groups.");
    }

    if (!group.isPublic()) {
      throw new IllegalStateException("This group is private and does not accept join requests.");
    }

    if (participantRepo
        .findByConversationIdAndUserId(conversationId, requester.getId())
        .isPresent()) {
      throw new IllegalStateException(
          "You are already a member or have a pending request for this group.");
    }

    if (blockRepo.existsByConversationIdAndBlockedUserId(conversationId, requester.getId())) {
      throw new IllegalStateException("You are blocked from joining this group.");
    }

    // 2. Create a 'pending' participant record
    ConversationParticipant joinRequest =
        createParticipant(group, requester, ParticipantRole.waiting, ParticipantStatus.PENDING);
    participantRepo.save(joinRequest);
  }

  @Transactional(readOnly = true)
  public List<GroupJoinRequestDto> getJoinRequests(User currentUser, String uuid) {
    Conversation conversation =
        conversationRepo
            .findByUuid(uuid)
            .orElseThrow(() -> new IllegalArgumentException("Group not found."));

    // 权限校验：只有群主或管理员才能查看
    ConversationParticipant currentUserParticipant =
        getParticipant(conversation.getId(), currentUser.getId());
    if (currentUserParticipant.getRole() == ParticipantRole.member) {
      throw new SecurityException("Only owner or admins can view join requests.");
    }

    // 查找所有状态为 PENDING 的请求
    List<ConversationParticipant> requests =
        participantRepo.findByConversationAndStatus(conversation, ParticipantStatus.PENDING);
    System.out.println(requests);
    // 转换为 DTO
    return requests.stream().map(GroupJoinRequestDto::fromEntity).collect(Collectors.toList());
  }

  @Transactional
  public void handleGroupJoinRequest(
      User actor, Long conversationId, Long requesterId, String action) {
    // 1. Authorization: Check if the current user (actor) is an admin or owner of the group
    ConversationParticipant actorParticipant = getParticipant(conversationId, actor.getId());
    checkAdminOrOwner(actorParticipant); // This helper method already throws SecurityException

    // 2. Find the pending join request from the requester
    ConversationParticipant requesterParticipant =
        participantRepo
            .findByConversationIdAndUserId(conversationId, requesterId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Join request from the specified user not found."));

    // 3. Validation: Ensure the request is pending
    if (requesterParticipant.getStatus() != ParticipantStatus.PENDING) {
      throw new IllegalStateException("This join request has already been handled.");
    }

    // 4. Perform the action
    switch (action.toUpperCase()) {
      case "ACCEPT":
        requesterParticipant.setStatus(ParticipantStatus.APPROVED);
        requesterParticipant.setJoinedAt(LocalDateTime.now());
        requesterParticipant.setIsMuted(false);
        requesterParticipant.setRole(ParticipantRole.member);
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

  @Transactional
  public void forwardMessage(User currentUser, ForwardMessageRequest request) {
    // 1. 找到原始消息
    Message originalMessage =
        messageRepo
            .findById(request.getOriginalMessageId())
            .orElseThrow(() -> new IllegalArgumentException("Original message not found."));

    // 2. 验证当前用户是否有权转发此消息 (例如，必须是该消息所在会话的成员)
    // ... 此处省略权限校验逻辑 ...

    // 3. 遍历所有目标会话
    request
        .getTargetConversationIds()
        .forEach(
            convoId -> {
              Conversation targetConversation =
                  conversationRepo
                      .findById(convoId)
                      .orElseThrow(
                          () -> new IllegalArgumentException("Target conversation not found."));

              // 4. 为每个目标会话创建一条新的消息 (复制内容)
              Message forwardedMessage = new Message();
              forwardedMessage.setConversation(targetConversation);
              forwardedMessage.setSender(currentUser); // 转发者是当前用户
              forwardedMessage.setContent(originalMessage.getContent());
              forwardedMessage.setMessageType(originalMessage.getMessageType());
              // ... 其他需要复制的属性 ...

              Message savedMsg = messageRepo.save(forwardedMessage);
              // (可选) 通过WebSocket广播这条新消息
              messagingTemplate.convertAndSend(
                  "/topic/conversations/" + convoId, MessageDto.fromEntity(savedMsg));
            });

    // 5. (可选) 如果有附带留言，也作为一条新消息发送
    if (request.getAttachedMessage() != null && !request.getAttachedMessage().trim().isEmpty()) {
      // 再次遍历所有目标会话，发送留言
      request
          .getTargetConversationIds()
          .forEach(
              convoId -> {
                Conversation targetConversation =
                    conversationRepo
                        .findById(convoId)
                        .orElseThrow(
                            () -> new IllegalArgumentException("Target conversation not found."));

                Message attachedMsg = new Message();
                attachedMsg.setConversation(targetConversation);
                attachedMsg.setSender(currentUser);
                attachedMsg.setContent(request.getAttachedMessage().trim());
                attachedMsg.setMessageType(MessageType.text); // 留言是纯文本消息

                Message savedAttachedMsg = messageRepo.save(attachedMsg);
                // 同样广播这条留言消息
                messagingTemplate.convertAndSend(
                    "/topic/conversations/" + convoId, MessageDto.fromEntity(savedAttachedMsg));
              });
    }
  }

  /**
   * 根据UUID查找群聊的详细信息，包括所有成员.
   *
   * @param uuid 要搜索的群聊的UUID.
   * @return 包含群聊详细信息的DTO.
   */
  @Transactional
  public GroupDetailsDto findGroupDetailsByUuid(String uuid, String username) {
    // 1. 根据UUID精确查找会话
    Conversation conversation =
        conversationRepo
            .findByUuid(uuid)
            .orElseThrow(
                () -> new IllegalArgumentException("Group with UUID " + uuid + " not found."));

    // 2. 验证这确实是一个群聊
    if (conversation.getType() != ConversationType.GROUP) {
      throw new IllegalArgumentException("The provided ID does not belong to a group chat.");
    }

    // 3. 获取该群聊的所有参与者
    List<ConversationParticipant> participants =
        participantRepo.findAllByConversation(conversation);

    // 4. 将参与者中的用户信息映射到 UserSearchResultDto 列表
    List<GroupMemberDto> memberDtos =
        participants.stream()
            .map(participant -> mapToGroupMemberDto(participant.getUser(), participant.getRole()))
            .collect(Collectors.toList());

    // 5. 组装并返回最终的 GroupDetailsDto
    return GroupDetailsDto.fromEntity(
        conversation,
        memberDtos,
        conversationRepo.findUserByConversationIdAndUsername(uuid, username).getRole());
  }

  @Transactional
  public List<GroupDetailsDto> getJoinedGroups(User currentUser) {
    // 1. 从数据库获取用户参与的所有群聊会话
    List<Conversation> groups =
        conversationRepo.findConversationsByParticipantAndType(
            currentUser.getId(), ConversationType.GROUP);

    // 2. 遍历每个群聊，将其转换为 GroupDetailsDto
    return groups.stream()
        .map((Conversation group) -> mapToGroupDetailsDto(group, currentUser))
        .collect(Collectors.toList());
  }

  @Transactional
  public List<MessageDto> getMessagesForConversation(Long conversationId, User currentUser) {
    // 1. 权限校验：确保当前用户是该会话的成员
    participantRepo
        .findByConversationIdAndUserId(conversationId, currentUser.getId())
        .orElseThrow(() -> new AccessDeniedException("You are not a member of this conversation."));

    ConversationParticipant participant = getParticipant(conversationId, currentUser.getId());

    // 2. 调用仓库方法获取消息实体列表
    List<Message> messages =
        messageRepo.findByConversationIdAndIsDeletedFalseOrderByCreatedAtAsc(conversationId);

    // 3. 将实体列表转换为 DTO 列表并返回
    if (participant.getDeletedHistoryAt() != null) {
      return messages.stream()
          .filter(message -> message.getCreatedAt().isAfter(participant.getDeletedHistoryAt()))
          .map(MessageDto::fromEntity) // 复用我们之前创建的转换方法
          .collect(Collectors.toList());
    } else {
      return messages.stream()
          .map(MessageDto::fromEntity) // 复用我们之前创建的转换方法
          .collect(Collectors.toList());
    }
  }

  @Transactional
  public GroupDetailsDto mapToGroupDetailsDto(Conversation group, User currentUser) {
    // 1. 获取群聊的所有成员，并将他们转换为 UserSearchResultDto
    List<GroupMemberDto> members =
        conversationRepo.findAllUserByConversationId(group.getId()).stream()
            .map(
                participant -> {
                  User user = participant.getUser();
                  ParticipantRole role = participant.getRole(); // 获取成员角色
                  return mapToGroupMemberDto(user, role); // 将 user 和 role 传递给 mapToGroupMemberDto
                })
            .collect(Collectors.toList());

    ParticipantRole currentUserRole =
        conversationRepo.findAllUserByConversationId(group.getId()).stream()
            .filter(p -> p.getUser().getId().equals(currentUser.getId()))
            .map(ConversationParticipant::getRole)
            .findFirst()
            .orElse(ParticipantRole.member);

    // 2. 使用 GroupDetailsDto 提供的静态工厂方法创建最终的 DTO
    return GroupDetailsDto.fromEntity(group, members, currentUserRole);
  }

  @Transactional
  // 辅助方法：将 User 实体映射到 UserSearchResultDto
  public GroupMemberDto mapToGroupMemberDto(User user, ParticipantRole role) {
    GroupMemberDto dto = new GroupMemberDto();
    dto.setId(user.getId());
    dto.setUsername(user.getUsername());
    dto.setNickname(user.getNickname());
    dto.setAvatarUrl(user.getAvatarUrl());
    dto.setRole(role);
    return dto;
  }

  private ConversationSummaryDto mapToConversationSummaryDto(Conversation convo, User currentUser) {
    ConversationSummaryDto dto = new ConversationSummaryDto();
    dto.setConversationId(convo.getId());
    dto.setType(convo.getType());
    dto.setUuid(convo.getUuid());

    // Set pinned and muted status from the participant record
    participantRepo
        .findByConversationIdAndUserId(convo.getId(), currentUser.getId())
        .ifPresent(
            p -> {
              dto.setUnreadCount(p.getUnreadCount() != null ? p.getUnreadCount() : 0);
              dto.setPinned(p.isPinned());
              dto.setNotificationsMuted(p.isAreNotificationsMuted());
            });

    // Set last message details
    messageRepo
        .findTopByConversationOrderByCreatedAtDesc(convo)
        .ifPresent(
            msg -> {
              dto.setLastMessageContent(msg.getContent());
              dto.setLastMessageTimestamp(msg.getCreatedAt());
            });

    // Set name and avatar (logic differs for GROUP vs PRIVATE)
    if (convo.getType() == ConversationType.GROUP) {
      dto.setName(convo.getName());
      dto.setAvatarUrl(convo.getAvatarUrl());
    } else { // For private chats, find the other participant
      participantRepo.findAllByConversation(convo).stream()
          .filter(p -> !p.getUser().getId().equals(currentUser.getId()))
          .findFirst()
          .ifPresent(
              other -> {
                dto.setName(other.getUser().getNickname());
                dto.setAvatarUrl(other.getUser().getAvatarUrl());
              });
    }
    return dto;
  }

  /**
   * (核心新增) 生成拼接头像并保存为文件
   *
   * @param members 成员列表
   * @return 生成的头像文件名
   * @throws IOException 如果图片读写失败
   */
  @Transactional
  public String generateGroupAvatar(List<User> members) throws IOException {
    int size = 200; // 生成图片尺寸，通常比前端绘制的尺寸大一些，保证清晰度
    BufferedImage combined = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = combined.createGraphics();

    // 绘制背景色
    g.setColor(new java.awt.Color(224, 224, 224));
    g.fillRect(0, 0, size, size);

    // 只取最多前9个成员用于绘制
    List<User> membersToDraw = members.stream().limit(9).toList();
    int count = membersToDraw.size();

    int gap = 4; // 图片间距

    if (count > 0) {
      // 根据成员数量决定布局
      int[][] positions;
      int smallSize;

      if (count == 1) {
        // 1个成员：居中绘制
        positions = new int[][] {{0, 0}};
        smallSize = size;
      } else if (count == 2) {
        // 2个成员：水平并排
        smallSize = (size - gap) / 2;
        positions = new int[][] {{0, 0}, {smallSize + gap, 0}};
      } else if (count <= 4) {
        // 3或4个成员：2x2网格
        smallSize = (size - gap) / 2;
        positions =
            new int[][] {
              {0, 0},
              {smallSize + gap, 0},
              {0, smallSize + gap},
              {smallSize + gap, smallSize + gap}
            };
      } else { // 5到9个成员：3x3网格
        smallSize = (size - 2 * gap) / 3;
        positions = new int[count][2];
        for (int i = 0; i < count; i++) {
          int row = i / 3;
          int col = i % 3;
          positions[i][0] = col * (smallSize + gap);
          positions[i][1] = row * (smallSize + gap);
        }
      }

      // 循环绘制每个成员的头像
      for (int i = 0; i < count; i++) {
        User member = membersToDraw.get(i);
        try {
          // 读取成员头像图片
          BufferedImage avatar = ImageIO.read(new URL(member.getAvatarUrl()));

          // 根据计算好的位置和尺寸绘制头像
          int x = positions[i][0];
          int y = positions[i][1];
          int w = smallSize;
          int h = smallSize;

          // 针对2个成员的特殊处理，高度填满
          if (count == 2) {
            h = size;
          }

          g.drawImage(avatar, x, y, w, h, null);
        } catch (Exception e) {
          System.err.println("Error drawing avatar for user: " + member.getUsername());
          e.printStackTrace();
          // 如果某个头像加载失败，可以绘制一个默认的占位符
          g.setColor(new java.awt.Color(150, 150, 150));
          g.fillRect(positions[i][0], positions[i][1], smallSize, smallSize);
        }
      }
    }

    g.dispose();

    // 保存文件
    String fileName = "group-" + UUID.randomUUID() + ".png";
    // String uploadDir = appConfig.getUploadPath();
    String uploadDir = "uploads/";
    File outputFile = new File(Paths.get(uploadDir, fileName).toString());
    outputFile.getParentFile().mkdirs();
    ImageIO.write(combined, "PNG", outputFile);

    return fileName;
  }

  @Transactional
  public List<ConversationBlock> getBlackList(Long conversationId) {
    return conversationRepo.getBlackListByConversationId(conversationId);
  }

  @Transactional
  public void unBlockUser(User currentUser, Long conversationId, Long targetId) {
    ConversationParticipant actorParticipant = getParticipant(conversationId, currentUser.getId());
    checkAdminOrOwner(actorParticipant);
    ConversationBlock blockedUser =
        blockRepo.findByConversationIdAndUserId(conversationId, targetId);
    if (blockedUser == null)
      throw new SecurityException("Target user is not blocked in this conversation.");
    blockRepo.delete(blockedUser);
  }

  @Transactional
  public List<GroupMemberDto> findMembersByUuid(Long id) {
    return conversationRepo.findAllUserByConversationId(id).stream()
        .map(participant -> mapToGroupMemberDto(participant.getUser(), participant.getRole()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<MutedUserDto> getMutedList(User currentUser, Long conversationId) {
    // 权限校验
    ConversationParticipant participant = getParticipant(conversationId, currentUser.getId());
    checkAdminOrOwner(participant);

    // 获取所有被禁言的参与者
    List<ConversationParticipant> mutedParticipants =
        participantRepo.findByConversationIdAndIsMutedTrue(conversationId);

    // 转换为新的 DTO
    return mutedParticipants.stream().map(MutedUserDto::fromEntity).collect(Collectors.toList());
  }

  @Transactional
  public void deletePrivateConversation(User currentUser, Long friendId) {
    // 查找两个用户之间的私聊会话
    Optional<Conversation> privateConversation =
        conversationRepo.findPrivateConversationBetweenUsers(currentUser.getId(), friendId);

    if (privateConversation.isPresent()) {
      Conversation conversation = privateConversation.get();

      // 删除所有相关消息
      messageRepo.deleteByConversationId(conversation.getId());

      // 删除所有参与者记录
      conversationParticipantRepo.deleteByConversationId(conversation.getId());

      // 删除会话本身
      conversationRepo.delete(conversation);
    }
  }

  @Transactional(readOnly = true)
  public ConversationStatus checkConversationStatus(User currentUser, Long conversationId) {
    // 1. 查找会话
    Conversation conversation = conversationRepo.findById(conversationId).orElse(null);
    if (conversation == null) {
      return ConversationStatus.CONVERSATION_NOT_FOUND;
    }

    // 2. 根据会话类型进行不同检查
    if (conversation.getType() == ConversationType.GROUP) {
      // 检查是否被拉黑
      if (blockRepo.existsByConversationIdAndBlockedUserId(conversationId, currentUser.getId())) {
        return ConversationStatus.BLOCKED_FROM_GROUP;
      }
      // 检查是否是成员，以及是否被禁言
      return participantRepo
          .findByConversationIdAndUserId(conversationId, currentUser.getId())
          .map(
              participant -> {
                if (participant.getIsMuted()) {
                  return ConversationStatus.MUTED;
                }
                return ConversationStatus.OK;
              })
          .orElse(ConversationStatus.NOT_A_MEMBER);

    } else {
      return participantRepo
          .findPartnerInPrivateConversation(conversationId, currentUser.getId())
          .map(
              partner -> {
                // 检查好友关系是否存在
                if (!friendshipRepo.existsByUserAndFriend(currentUser, partner)) {
                  return ConversationStatus.FRIENDSHIP_TERMINATED;
                }
                return ConversationStatus.OK;
              })
          .orElse(ConversationStatus.CONVERSATION_NOT_FOUND);
    }
  }
}
