package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Report entities.
 *
 * By extending JpaRepository, this interface automatically inherits methods for standard
 * CRUD (Create, Read, Update, Delete) operations, including save(), findById(), findAll(), delete(), etc.
 *
 * You do not need to write an implementation class for this interface. Spring Data JPA
 * will dynamically create a proxy bean at runtime that handles the database interactions.
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    // JpaRepository<Report, Long> means this repository manages 'Report' entities,
    // and the primary key of the 'Report' entity is of type 'Long'.

    // You can add custom query methods here if needed, for example:
    // List<Report> findByStatus(ReportStatus status);
}