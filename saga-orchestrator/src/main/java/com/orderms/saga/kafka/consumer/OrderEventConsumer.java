package com.orderms.saga.kafka.consumer;

import com.orderms.saga.kafka.event.OrderCreatedEvent;
import com.orderms.saga.service.SagaOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final SagaOrchestrationService sagaOrchestrationService;

    @KafkaListener(
            topics = "order-events",
            groupId = "saga-orchestrator-group",
            containerFactory = "orderEventKafkaListenerContainerFactory"
    )
    public void consumeOrderCreatedEvent(OrderCreatedEvent event, Acknowledgment acknowledgment) {
        log.info("Received OrderCreatedEvent for orderId: {}", event.getOrderId());

        try {
            sagaOrchestrationService.startSaga(event);
            acknowledgment.acknowledge();
            log.info("Saga started successfully for orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to start saga for orderId: {}", event.getOrderId(), e);
            // Don't acknowledge - message will be redelivered
        }
    }
}