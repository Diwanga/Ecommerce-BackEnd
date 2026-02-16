package com.orderms.notification.kafka.consumer;

import com.orderms.notification.kafka.event.OrderCompletedEvent;
import com.orderms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "notification-events",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderCompletedEvent(OrderCompletedEvent event, Acknowledgment acknowledgment) {
        log.info("Received OrderCompletedEvent for orderId: {}, userId: {}",
                event.getOrderId(), event.getUserId());

        try {
            notificationService.processOrderCompletedEvent(event);
            acknowledgment.acknowledge();
            log.info("OrderCompletedEvent processed successfully for orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process OrderCompletedEvent for orderId: {}", event.getOrderId(), e);
            // Don't acknowledge - message will be redelivered
        }
    }
}