package com.orderms.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderms.notification.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SSEService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Store active SSE connections in memory
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final long SSE_TIMEOUT = 30 * 60 * 1000; // 30 minutes
    private static final String REDIS_SSE_KEY_PREFIX = "sse:connection:";

    public SseEmitter createConnection(String userId) {
        log.info("Creating SSE connection for user: {}", userId);

        // Remove old emitter if exists
        removeConnection(userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.put(userId, emitter);

        // Store in Redis for cross-instance awareness
        String redisKey = REDIS_SSE_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(redisKey, "connected", SSE_TIMEOUT, TimeUnit.MILLISECONDS);

        // Handle completion and timeout
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for user: {}", userId);
            removeConnection(userId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE connection timeout for user: {}", userId);
            removeConnection(userId);
        });

        emitter.onError(e -> {
            log.error("SSE connection error for user: {}", userId, e);
            removeConnection(userId);
        });

        // Send initial connection message
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to notification service"));
        } catch (IOException e) {
            log.error("Failed to send initial SSE message to user: {}", userId, e);
            removeConnection(userId);
        }

        log.info("SSE connection created successfully for user: {}. Total active connections: {}",
                userId, emitters.size());

        return emitter;
    }

    public void sendNotification(String userId, NotificationMessage notification) {
        log.info("Sending SSE notification to user: {} - Type: {}", userId, notification.getType());

        SseEmitter emitter = emitters.get(userId);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));

                log.info("SSE notification sent successfully to user: {}", userId);
            } catch (IOException e) {
                log.error("Failed to send SSE notification to user: {}", userId, e);
                removeConnection(userId);
            }
        } else {
            log.warn("No active SSE connection for user: {}. Notification will be stored in DB.", userId);
        }
    }

    public void removeConnection(String userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("Error completing emitter for user: {}", userId, e);
            }
        }

        // Remove from Redis
        String redisKey = REDIS_SSE_KEY_PREFIX + userId;
        redisTemplate.delete(redisKey);

        log.info("SSE connection removed for user: {}. Total active connections: {}",
                userId, emitters.size());
    }

    public boolean hasActiveConnection(String userId) {
        return emitters.containsKey(userId);
    }

    public int getActiveConnectionCount() {
        return emitters.size();
    }

    public void sendHeartbeat(String userId) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
            } catch (IOException e) {
                log.error("Failed to send heartbeat to user: {}", userId, e);
                removeConnection(userId);
            }
        }
    }
}