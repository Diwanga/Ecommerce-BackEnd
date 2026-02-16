package com.orderms.order.kafka.consumer;

import com.orderms.order.kafka.event.OrderCancelledCommand;
import com.orderms.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledCommandConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "order-commands",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderCancelledCommand(OrderCancelledCommand command) {
        log.info("Received OrderCancelledCommand for orderId: {}", command.getOrderId());

        try {
            orderService.cancelOrder(command.getOrderId(), command.getReason());
            log.info("Order cancelled successfully: {}", command.getOrderId());
        } catch (Exception e) {
            log.error("Failed to cancel order: {}", command.getOrderId(), e);
        }
    }
}