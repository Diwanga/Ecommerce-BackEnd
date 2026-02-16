package com.orderms.notification.service;

import com.orderms.notification.dto.NotificationMessage;
import com.orderms.notification.entity.Notification;
import com.orderms.notification.entity.NotificationType;
import com.orderms.notification.kafka.event.OrderCompletedEvent;
import com.orderms.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SSEService sseService;

    @Transactional
    public void processOrderCompletedEvent(OrderCompletedEvent event) {
        log.info("Processing OrderCompletedEvent for orderId: {}, userId: {}",
                event.getOrderId(), event.getUserId());

        // Determine notification type based on message
        NotificationType type = determineNotificationType(event.getMessage());

        // Save notification to database
        Notification notification = new Notification();
        notification.setUserId(event.getUserId());
        notification.setOrderId(event.getOrderId());
        notification.setMessage(event.getMessage());
        notification.setType(type);
        notification.setIsRead(false);

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification saved to database with id: {}", savedNotification.getId());

        // Create notification message for SSE
        NotificationMessage message = NotificationMessage.builder()
                .id(savedNotification.getId())
                .userId(event.getUserId())
                .orderId(event.getOrderId())
                .message(event.getMessage())
                .type(type)
                .timestamp(savedNotification.getCreatedAt())
                .build();

        // Try to send via SSE
        if (sseService.hasActiveConnection(event.getUserId())) {
            sseService.sendNotification(event.getUserId(), message);
            log.info("Notification sent via SSE to user: {}", event.getUserId());
        } else {
            log.info("No active SSE connection for user: {}. Notification stored in DB for later retrieval.",
                    event.getUserId());
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationMessage> getUserNotifications(String userId) {
        log.info("Fetching notifications for user: {}", userId);

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return notifications.stream()
                .map(this::mapToNotificationMessage)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationMessage> getUnreadNotifications(String userId) {
        log.info("Fetching unread notifications for user: {}", userId);

        List<Notification> notifications = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        return notifications.stream()
                .map(this::mapToNotificationMessage)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        log.info("Marking notification as read: {}", notificationId);

        notificationRepository.findById(notificationId)
                .ifPresent(notification -> {
                    notification.setIsRead(true);
                    notificationRepository.save(notification);
                });
    }

    @Transactional
    public void markAllAsRead(String userId) {
        log.info("Marking all notifications as read for user: {}", userId);

        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        unreadNotifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);

        log.info("Marked {} notifications as read for user: {}", unreadNotifications.size(), userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    private NotificationType determineNotificationType(String message) {
        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("confirmed") || lowerMessage.contains("successful")) {
            return NotificationType.ORDER_CONFIRMED;
        } else if (lowerMessage.contains("cancelled") || lowerMessage.contains("failed")) {
            return NotificationType.ORDER_CANCELLED;
        } else {
            return NotificationType.ORDER_CREATED;
        }
    }

    private NotificationMessage mapToNotificationMessage(Notification notification) {
        return NotificationMessage.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .orderId(notification.getOrderId())
                .message(notification.getMessage())
                .type(notification.getType())
                .timestamp(notification.getCreatedAt())
                .build();
    }
}