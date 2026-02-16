package com.orderms.notification.dto;

import com.orderms.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessage {

    private Long id;
    private String userId;
    private Long orderId;
    private String message;
    private NotificationType type;
    private LocalDateTime timestamp;
}