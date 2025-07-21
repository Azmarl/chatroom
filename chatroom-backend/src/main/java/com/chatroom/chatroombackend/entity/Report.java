package com.chatroom.chatroombackend.entity;

import com.chatroom.chatroombackend.enums.ReportedEntityType;
import com.chatroom.chatroombackend.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reported_entity_type", columnDefinition = "ENUM('user', 'conversation', 'message', 'moment', 'comment')")
    private ReportedEntityType reportedEntityType;

    @Column(name = "reported_entity_id", nullable = false)
    private Long reportedEntityId;

    @Lob
    @Column(nullable = false)
    private String reason;

    @Column(name = "evidence_url", length = 255)
    private String evidenceUrl;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('pending', 'in_review', 'resolved', 'rejected')")
    private ReportStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handler_id")
    private User handler; // 管理员也是用户

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Lob
    @Column(name = "resolution_details")
    private String resolutionDetails;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}