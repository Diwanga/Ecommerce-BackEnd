package com.orderms.notification.controller;

import com.orderms.notification.dto.NotificationMessage;
import com.orderms.notification.service.NotificationService;
import com.orderms.notification.service.SSEService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow CORS for frontend
public class NotificationController {

    private final NotificationService notificationService;
    private final SSEService sseService;

    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@PathVariable String userId) {
        log.info("SSE stream requested for user: {}", userId);
        return sseService.createConnection(userId);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationMessage>> getUserNotifications(@PathVariable String userId) {
        log.info("Fetching all notifications for user: {}", userId);

        List<NotificationMessage> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{userId}/unread")
    public ResponseEntity<List<NotificationMessage>> getUnreadNotifications(@PathVariable String userId) {
        log.info("Fetching unread notifications for user: {}", userId);

        List<NotificationMessage> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{userId}/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String userId) {
        log.info("Fetching unread count for user: {}", userId);

        long count = notificationService.getUnreadCount(userId);

        Map<String, Long> response = new HashMap<>();
        response.put("count", count);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        log.info("Marking notification as read: {}", notificationId);

        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable String userId) {
        log.info("Marking all notifications as read for user: {}", userId);

        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("Fetching notification service statistics");

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeConnections", sseService.getActiveConnectionCount());

        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/stream/{userId}")
    public ResponseEntity<Void> closeConnection(@PathVariable String userId) {
        log.info("Closing SSE connection for user: {}", userId);

        sseService.removeConnection(userId);
        return ResponseEntity.ok().build();
    }
}