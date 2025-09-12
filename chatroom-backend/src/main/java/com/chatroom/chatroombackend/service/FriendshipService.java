package com.chatroom.chatroombackend.service;

import com.chatroom.chatroombackend.dto.UserSearchResultDto;
import com.chatroom.chatroombackend.entity.Friendship;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.entity.UserBlockId;
import com.chatroom.chatroombackend.enums.FriendshipStatus;
import com.chatroom.chatroombackend.repository.FriendshipRepository;
import com.chatroom.chatroombackend.repository.UserBlockRepository;
import com.chatroom.chatroombackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FriendshipService {

  @Autowired private FriendshipRepository friendshipRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private UserBlockRepository userBlockRepo;
  @Autowired private SimpMessagingTemplate messagingTemplate;

  @Transactional
  public Friendship sendFriendRequest(User requester, String targetUsername) {
    if (requester.getUsername().equals(targetUsername)) {
      throw new IllegalArgumentException("You cannot send a friend request to yourself.");
    }
    User targetUser =
        userRepo
            .findByUsername(targetUsername)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "User with username '" + targetUsername + "' not found."));
    if (userBlockRepo.findBlockedIdsByBlockerId(requester.getId()).contains(targetUser.getId())) {
      throw new IllegalStateException("Cannot send request due to a block between users.");
    }

    // 2. (核心修改) 检查已存在的请求记录
    Optional<Friendship> existingFriendshipOpt =
        friendshipRepo.findFriendshipBetweenUsers(requester, targetUser);

    if (existingFriendshipOpt.isPresent()) {
      Friendship existingFriendship = existingFriendshipOpt.get();
      // 如果已经是好友或请求待处理，则直接报错
      if (existingFriendship.getStatus() == FriendshipStatus.accepted
          || existingFriendship.getStatus() == FriendshipStatus.pending) {
        throw new IllegalStateException("A friendship or pending request already exists.");
      }

      // 如果之前被拒绝过 (status == REJECTED)
      if (existingFriendship.getStatus() == FriendshipStatus.rejected) {
        // 检查请求次数是否已达上限
        if (existingFriendship.getRequestCount() >= 3) {
          throw new IllegalStateException(
              "You have been denied for over three times, you can't send request to this user again.");
        }
        // 如果未达上限，则更新现有记录：计数+1，状态改回PENDING
        existingFriendship.setRequestCount(existingFriendship.getRequestCount() + 1);
        existingFriendship.setStatus(FriendshipStatus.pending);
        return friendshipRepo.save(existingFriendship);
      }
    }

    // 3. 如果没有任何历史记录，则创建新的请求
    Friendship newRequest = new Friendship();
    newRequest.setUser(requester);
    newRequest.setFriend(targetUser);
    newRequest.setStatus(FriendshipStatus.pending);
    newRequest.setRequestCount(1); // 第一次请求，计数为1

    messagingTemplate.convertAndSendToUser(
        targetUser.getId().toString(), "/queue/notifications", newRequest);

    return friendshipRepo.save(newRequest);
  }

  @Transactional
  public void handleFriendRequest(User currentUser, Long friendshipId, String action) {
    Friendship friendship =
        friendshipRepo
            .findById(friendshipId)
            .orElseThrow(() -> new IllegalArgumentException("Friend request not found."));

    if (!friendship.getFriend().getId().equals(currentUser.getId())) {
      throw new SecurityException("You do not have permission to handle this friend request.");
    }
    if (friendship.getStatus() != FriendshipStatus.pending) {
      throw new IllegalStateException("This friend request has already been handled.");
    }

    switch (action.toUpperCase()) {
      case "ACCEPT":
        friendship.setStatus(FriendshipStatus.accepted);
        friendshipRepo.save(friendship);
        break;
      case "REJECT":
        // (核心修改) 不再删除记录，而是更新状态为REJECTED
        friendship.setStatus(FriendshipStatus.rejected);
        friendshipRepo.save(friendship);
        break;
      default:
        throw new IllegalArgumentException("Invalid action. Must be 'ACCEPT' or 'REJECT'.");
    }
  }

  /**
   * 按用户名精确搜索用户，并转换为DTO返回. 此方法现在返回一个列表，该列表最多包含一个元素或为空.
   *
   * @param currentUser 执行搜索的当前用户.
   * @param usernameQuery 要精确搜索的用户名.
   * @return 一个包含0个或1个搜索结果的DTO列表.
   */
  public List<UserSearchResultDto> searchUsersByUsername(User currentUser, String usernameQuery) {
    // 1. 验证：不能搜索自己
    if (currentUser.getUsername().equals(usernameQuery)) {
      return Collections.emptyList(); // 返回空列表
    }

    // 2. 使用 findByUsername 进行精确查找
    Optional<User> foundUserOpt = userRepo.findByUsername(usernameQuery);

    // 如果根据用户名没有找到任何用户，直接返回空列表
    if (foundUserOpt.isEmpty()) {
      return Collections.emptyList();
    }

    User foundUser = foundUserOpt.get();

    // 3. 验证：检查找到的用户是否已被当前用户拉黑
    boolean isBlocked =
        userBlockRepo.existsById(new UserBlockId(currentUser.getId(), foundUser.getId()));

    if (isBlocked) {
      return Collections.emptyList(); // 如果已拉黑，同样返回空列表
    }

    // 4. 如果找到了用户且未被拉黑，将其映射为DTO并放入列表中返回
    UserSearchResultDto dto = mapToUserSearchResultDto(foundUser);
    return List.of(dto); // 使用 List.of() 创建一个只包含一个元素的列表
  }

  /**
   * (核心新增) 获取当前用户的所有好友列表。
   *
   * @param currentUser 当前登录的用户实体
   * @return 一个包含好友详细信息的DTO列表
   */
  public List<UserSearchResultDto> getFriends(User currentUser) {
    // 1. 从数据库获取所有与当前用户相关且状态为ACCEPTED的好友关系
    List<Friendship> friendships =
        friendshipRepo.findAllFriendsByUserIdAndStatus(
            currentUser.getId(), FriendshipStatus.accepted);

    // 2. 遍历好友关系列表，提取出好友（非当前用户）的User对象
    List<User> friends =
        friendships.stream()
            .map(
                friendship -> {
                  // 判断这条记录中，谁是好友
                  if (friendship.getUser().getId().equals(currentUser.getId())) {
                    // 如果user_id是当前用户，那么friend_id是好友
                    return friendship.getFriend();
                  } else {
                    // 如果friend_id是当前用户，那么user_id是好友
                    return friendship.getUser();
                  }
                })
            .toList();

    // 3. 将User实体列表映射为对前端友好的DTO列表
    return friends.stream().map(this::mapToUserSearchResultDto).collect(Collectors.toList());
  }

  @Transactional
  public void deleteFriendship(User currentUser, Long friendId) {
    // 查找两个方向的好友关系
    Optional<Friendship> friendship1 =
        friendshipRepo.findByUserIdAndFriendId(currentUser.getId(), friendId);
    Optional<Friendship> friendship2 =
        friendshipRepo.findByUserIdAndFriendId(friendId, currentUser.getId());

    // 至少应该存在一条记录
    if (friendship1.isEmpty() && friendship2.isEmpty()) {
      throw new IllegalArgumentException("Friendship not found.");
    }

    // 删除找到的记录
    friendship1.ifPresent(friendshipRepo::delete);
    friendship2.ifPresent(friendshipRepo::delete);
  }

  // Helper method to map User entity to DTO (此方法保持不变)
  private UserSearchResultDto mapToUserSearchResultDto(User user) {
    UserSearchResultDto dto = new UserSearchResultDto();
    dto.setId(user.getId());
    dto.setUsername(user.getUsername());
    dto.setNickname(user.getNickname());
    dto.setAvatarUrl(user.getAvatarUrl());
    return dto;
  }
}
