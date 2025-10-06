package com.adx.ad_x.repository;

import com.adx.ad_x.model.Notification;
import com.adx.ad_x.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find notifications by user
    List<Notification> findByUserAndActiveTrueOrderByCreatedAtDesc(User user);

    // Find unread notifications by user
    List<Notification> findByUserAndIsReadFalseAndActiveTrueOrderByCreatedAtDesc(User user);

    // Count unread notifications by user
    Long countByUserAndIsReadFalseAndActiveTrue(User user);

    // Find notifications by type
    List<Notification> findByUserAndTypeAndActiveTrueOrderByCreatedAtDesc(User user, String type);

    // Mark all notifications as read for user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user = :user AND n.isRead = false")
    void markAllAsRead(@Param("user") User user);

    // Delete old notifications (cleanup)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate AND n.isRead = true")
    void deleteOldNotifications(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    // Find notifications by related entity
    List<Notification> findByUserAndRelatedEntityIdAndRelatedEntityTypeAndActiveTrueOrderByCreatedAtDesc(
            User user, Long relatedEntityId, String relatedEntityType);
}
