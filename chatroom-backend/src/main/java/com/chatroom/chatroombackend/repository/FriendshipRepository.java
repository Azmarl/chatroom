package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.entity.Friendship;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.enums.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // 搜索用户的好友（用户名或昵称匹配）
    @Query("SELECT f.friend FROM Friendship f WHERE f.user = :currentUser AND f.status = 'accepted' AND (f.friend.username LIKE %:query% OR f.friend.nickname LIKE %:query%)")
    List<User> findFriendsByUsernameOrNickname(User currentUser, String query);

    @Query("SELECT f.friend FROM Friendship f WHERE f.user = :currentUser AND f.status = 'accepted'")
    List<User> findAllFriends(User currentUser);

    @Query("SELECT f FROM Friendship f WHERE (f.user = :user1 AND f.friend = :user2) OR (f.user = :user2 AND f.friend = :user1)")
    Optional<Friendship> findFriendshipBetweenUsers(User user1, User user2);

    /**
     * Finds all pending friend requests where the given user is the recipient.
     * @param friend The user who received the requests.
     * @param status The status to filter by (should be PENDING).
     * @return A list of pending Friendship entities.
     */
    List<Friendship> findByFriendAndStatus(User friend, FriendshipStatus status);
}
