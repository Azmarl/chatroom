package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.entity.Friendship;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.enums.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

  // 搜索用户的好友（用户名或昵称匹配）
  @Query(
      "SELECT f.friend FROM Friendship f WHERE f.user = :currentUser AND f.status = 'accepted' AND (f.friend.username LIKE %:query% OR f.friend.nickname LIKE %:query%)")
  List<User> findFriendsByUsernameOrNickname(User currentUser, String query);

  @Query("SELECT f.friend FROM Friendship f WHERE f.user = :currentUser AND f.status = 'accepted'")
  List<User> findAllFriends(User currentUser);

  @Query(
      "SELECT f FROM Friendship f WHERE (f.user = :user1 AND f.friend = :user2) OR (f.user = :user2 AND f.friend = :user1)")
  Optional<Friendship> findFriendshipBetweenUsers(User user1, User user2);

  /**
   * Finds all pending friend requests where the given user is the recipient.
   *
   * @param friend The user who received the requests.
   * @param status The status to filter by (should be PENDING).
   * @return A list of pending Friendship entities.
   */
  List<Friendship> findByFriendAndStatus(User friend, FriendshipStatus status);

  /**
   * (核心新增) 查找一个用户所有已接受的好友关系。 这条查询会找出所有 status 为 ACCEPTED，并且 user_id 或 friend_id 为当前用户ID的记录。
   *
   * @param userId 当前用户的ID
   * @param status 要匹配的状态，这里是 FriendshipStatus.ACCEPTED
   * @return 一个包含所有相关好友关系实体的列表
   */
  @Query(
      "SELECT f FROM Friendship f WHERE (f.user.id = :userId OR f.friend.id = :userId) AND f.status = :status")
  List<Friendship> findAllFriendsByUserIdAndStatus(
      @Param("userId") Long userId, @Param("status") FriendshipStatus status);

  Optional<Friendship> findByUserIdAndFriendId(Long userId, Long friendId);

  boolean existsByUserAndFriend(User user, User friend);
}
