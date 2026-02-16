package com.orderms.order.kafka.consumer;

import com.orderms.order.kafka.event.OrderCompletedEvent;
import com.orderms.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "order-completed-events",
            groupId = "order-service-group",
            containerFactory = "orderCompletedEventKafkaListenerContainerFactory"
    )
    public void consumeOrderCompletedEvent(OrderCompletedEvent event, Acknowledgment acknowledgment) {
        log.info("Received OrderCompletedEvent for orderId: {}, success: {}",
                event.getOrderId(), event.isSuccess());

        try {
            if (event.isSuccess()) {
                // Mark order as completed
                orderService.completeOrder(event.getOrderId());
                log.info("Order completed successfully: {}", event.getOrderId());
            } else {
                // Mark order as cancelled due to saga compensation
                orderService.cancelOrder(event.getOrderId(), event.getMessage());
                log.info("Order cancelled due to saga failure: {}, reason: {}",
                        event.getOrderId(), event.getMessage());
            }

            // Manually acknowledge the message
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process OrderCompletedEvent for orderId: {}",
                    event.getOrderId(), e);
            // Don't acknowledge on failure - message will be retried
        }
    }
}