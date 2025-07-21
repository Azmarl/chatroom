package com.chatroom.chatroombackend.service;

import com.chatroom.chatroombackend.entity.*;
import com.chatroom.chatroombackend.enums.ReportedEntityType;
import com.chatroom.chatroombackend.repository.*;
import com.chatroom.chatroombackend.service.ImageProcessingService;
import com.chatroom.chatroombackend.service.SensitiveWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MomentService {

    @Autowired private MomentRepository momentRepo;
    @Autowired private MomentImageRepository momentImageRepo;
    @Autowired private MomentLikeRepository momentLikeRepo;
    @Autowired private CommentRepository commentRepo;
    @Autowired private ReportRepository reportRepo;
    @Autowired private UserBlockRepository userBlockRepo;
    @Autowired private SensitiveWordService sensitiveWordService;
    @Autowired private ImageProcessingService imageProcessingService;

    @Transactional
    public Moment postMoment(User poster, String content, List<MultipartFile> images) throws IOException {
        // 1. Rate Limiting
        long recentMoments = momentRepo.countByPosterAndCreatedAtAfter(poster, LocalDateTime.now().minusHours(1));
        if (recentMoments >= 3) {
            throw new IllegalStateException("You can post a maximum of 3 moments per hour.");
        }

        // 2. Input Validation
        if (content != null && content.length() > 1000) {
            throw new IllegalArgumentException("Content cannot exceed 1000 characters.");
        }
        if (images != null && images.size() > 9) {
            throw new IllegalArgumentException("You can upload a maximum of 9 images.");
        }

        // 3. Sensitive Word Check
        if (sensitiveWordService.containsSensitiveWords(content)) {
            throw new IllegalArgumentException("Your post contains sensitive content and cannot be published.");
        }

        // 4. Create and save the Moment entity
        Moment moment = new Moment();
        moment.setPoster(poster);
        moment.setContent(content);
        Moment savedMoment = momentRepo.save(moment);

        // 5. Process and save images
        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < images.size(); i++) {
                MultipartFile imageFile = images.get(i);
                String imageUrl = imageProcessingService.processAndUpload(imageFile);

                MomentImage momentImage = new MomentImage();
                momentImage.setMoment(savedMoment);
                momentImage.setImageUrl(imageUrl);
                momentImage.setOrderInMoment(i);
                momentImageRepo.save(momentImage);
            }
        }
        return savedMoment;
    }

    @Transactional
    public void deleteMoment(User user, Long momentId) {
        Moment moment = momentRepo.findById(momentId).orElseThrow(() -> new IllegalArgumentException("Moment not found."));
        if (!moment.getPoster().getId().equals(user.getId())) {
            throw new SecurityException("You can only delete your own moments.");
        }
        moment.setIsDeleted(true);
        momentRepo.save(moment);
    }

    @Transactional
    public int toggleLikeMoment(User user, Long momentId) {
        Moment moment = momentRepo.findById(momentId).orElseThrow(() -> new IllegalArgumentException("Moment not found."));

        momentLikeRepo.findByMomentAndUser(moment, user)
                .ifPresentOrElse(
                        like -> { // If like exists (unlike)
                            momentLikeRepo.delete(like);
                            moment.setLikesCount(Math.max(0, moment.getLikesCount() - 1));
                        },
                        () -> { // If like does not exist (like)
                            MomentLike newLike = new MomentLike();
                            newLike.setId(new MomentLikeId(momentId, user.getId()));
                            newLike.setMoment(moment);
                            newLike.setUser(user);
                            momentLikeRepo.save(newLike);
                            moment.setLikesCount(moment.getLikesCount() + 1);
                        }
                );
        return momentRepo.save(moment).getLikesCount();
    }

    @Transactional
    public Comment commentMoment(User user, Long momentId, String content, Long parentCommentId) {
        Moment moment = momentRepo.findById(momentId).orElseThrow(() -> new IllegalArgumentException("Moment not found."));

        Comment comment = new Comment();
        comment.setPoster(user);
        comment.setMoment(moment);
        comment.setContent(content);

        if (parentCommentId != null) {
            Comment parent = commentRepo.findById(parentCommentId).orElseThrow(() -> new IllegalArgumentException("Parent comment not found."));
            comment.setParentComment(parent);
        }

        moment.setCommentsCount(moment.getCommentsCount() + 1);
        momentRepo.save(moment);

        return commentRepo.save(comment);
    }

    @Transactional
    public void deleteComment(User user, Long commentId) {
        Comment comment = commentRepo.findById(commentId).orElseThrow(() -> new IllegalArgumentException("Comment not found."));
        Moment moment = comment.getMoment();

        // Allow deleting if you are the comment poster OR the moment poster
        if (!comment.getPoster().getId().equals(user.getId()) && !moment.getPoster().getId().equals(user.getId())) {
            throw new SecurityException("Permission denied to delete this comment.");
        }

        comment.setIsDeleted(true);
        commentRepo.save(comment);

        moment.setCommentsCount(Math.max(0, moment.getCommentsCount() - 1));
        momentRepo.save(moment);
    }

    public Page<Moment> getMoments(User user, Pageable pageable) {
        // Get IDs of users blocked by the current user
        List<Long> blockedUserIds = userBlockRepo.findBlockedIdsByBlockerId(user.getId());
        // Also add the current user to the list to not see their own blocks

        // The repository method needs to be updated to accept this list
        return momentRepo.findMoments(blockedUserIds, pageable);
    }

    public void reportMoment(User reporter, Long momentId, String reason) {
        if (!momentRepo.existsById(momentId)) throw new IllegalArgumentException("Moment not found.");

        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedEntityType(ReportedEntityType.moment);
        report.setReportedEntityId(momentId);
        report.setReason(reason);
        reportRepo.save(report);
    }
}