// NotificationRepository.java
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

    // READ: Get all notifications for a user, newest first
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    // READ: Get unread notifications for a user
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    // READ: Count unread notifications for a user
    long countByUserAndIsReadFalse(User user);

    // UPDATE: Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    void markAllAsRead(@Param("user") User user);

    // UPDATE: Mark specific notification as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.user = :user")
    int markAsRead(@Param("id") Long id, @Param("user") User user);

    // DELETE: Delete all read notifications for a user
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user = :user AND n.isRead = true")
    void deleteAllRead(@Param("user") User user);

    // DELETE: Delete all notifications for a user
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user = :user")
    void deleteAllByUser(@Param("user") User user);
}