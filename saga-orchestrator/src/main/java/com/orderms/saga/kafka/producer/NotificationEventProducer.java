package com.orderms.saga.kafka.producer;

import com.orderms.saga.kafka.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventProducer {

    private static final String NOTIFICATION_EVENTS_TOPIC = "notification-events";
    private static final String ORDER_COMPLETED_EVENTS_TOPIC = "order-completed-events";  //


    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderCompletedEvent(OrderCompletedEvent event) {
//        log.info("Sending OrderCompletedEvent for orderId: {}", event.getOrderId());
//        kafkaTemplate.send(NOTIFICATION_EVENTS_TOPIC, event.getOrderId().toString(), event);

        String key = event.getOrderId().toString();

        // Send to notification service
        kafkaTemplate.send(NOTIFICATION_EVENTS_TOPIC, key, event);
        log.info("OrderCompletedEvent sent to notification-events for orderId: {}", event.getOrderId());

        // ← ADD THIS: Send to order service to update status
        kafkaTemplate.send(ORDER_COMPLETED_EVENTS_TOPIC, key, event);
        log.info("OrderCompletedEvent sent to order-completed-events for orderId: {}", event.getOrderId());
    }
}