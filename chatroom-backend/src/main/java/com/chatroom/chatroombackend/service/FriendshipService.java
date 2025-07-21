package com.chatroom.chatroombackend.service;

import com.chatroom.chatroombackend.entity.Friendship;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.entity.UserBlockId;
import com.chatroom.chatroombackend.enums.FriendshipStatus;
import com.chatroom.chatroombackend.repository.FriendshipRepository;
import com.chatroom.chatroombackend.repository.UserBlockRepository;
import com.chatroom.chatroombackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendshipService {

    @Autowired private FriendshipRepository friendshipRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private UserBlockRepository userBlockRepo;

    @Transactional
    public Friendship sendFriendRequest(User requester, Long targetUserId) {
        // 1. Validation
        if (requester.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot send a friend request to yourself.");
        }

        User targetUser = userRepo.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found."));

        // 2. Check if either user has blocked the other
        if (userBlockRepo.existsById(new UserBlockId(requester.getId(), targetUserId)) ||
                userBlockRepo.existsById(new UserBlockId(targetUserId, requester.getId()))) {
            throw new IllegalStateException("Cannot send request due to a block between users.");
        }

        // 3. Check for existing friendship or pending request
        if (friendshipRepo.findFriendshipBetweenUsers(requester, targetUser).isPresent()) {
            throw new IllegalStateException("A friendship or pending request already exists.");
        }

        // 4. Create and save the new pending friendship
        Friendship newRequest = new Friendship();
        newRequest.setUser(requester);
        newRequest.setFriend(targetUser);
        newRequest.setStatus(FriendshipStatus.pending);

        return friendshipRepo.save(newRequest);
    }

    @Transactional
    public void handleFriendRequest(User currentUser, Long friendshipId, String action) {
        // 1. Find the pending request
        Friendship friendship = friendshipRepo.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found."));

        // 2. Authorization: Ensure the current user is the RECIPIENT of the request
        if (!friendship.getFriend().getId().equals(currentUser.getId())) {
            throw new SecurityException("You do not have permission to handle this friend request.");
        }

        // 3. Validation: Check if the request is still pending
        if (friendship.getStatus() != FriendshipStatus.pending) {
            throw new IllegalStateException("This friend request has already been handled.");
        }

        // 4. Perform the action
        switch (action.toUpperCase()) {
            case "ACCEPT":
                friendship.setStatus(FriendshipStatus.accepted);
                friendshipRepo.save(friendship);
                break;
            case "REJECT":
                // Rejecting a request simply removes the pending record.
                friendshipRepo.delete(friendship);
                break;
            default:
                throw new IllegalArgumentException("Invalid action. Must be 'ACCEPT' or 'REJECT'.");
        }
    }
}