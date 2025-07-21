package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.entity.MomentImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing MomentImage entities.
 *
 * It extends JpaRepository, which provides all standard CRUD operations.
 * No custom query methods are needed at this time as images are managed
 * through the 'Moment' entity's One-to-Many relationship.
 */
@Repository
public interface MomentImageRepository extends JpaRepository<MomentImage, Long> {
}