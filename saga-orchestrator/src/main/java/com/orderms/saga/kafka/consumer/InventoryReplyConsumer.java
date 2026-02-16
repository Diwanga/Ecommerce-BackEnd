package com.orderms.saga.kafka.consumer;

import com.orderms.saga.kafka.event.InventoryReply;
import com.orderms.saga.service.SagaOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReplyConsumer {

    private final SagaOrchestrationService sagaOrchestrationService;

    @KafkaListener(
            topics = "inventory-replies",
            groupId = "saga-orchestrator-group",
            containerFactory = "inventoryReplyKafkaListenerContainerFactory"
    )
    public void consumeInventoryReply(InventoryReply reply, Acknowledgment acknowledgment) {
        log.info("Received InventoryReply for orderId: {}, success: {}, instance: {}",
                reply.getOrderId(), reply.isSuccess(), reply.getInstanceId());

        try {
            sagaOrchestrationService.handleInventoryReply(reply);
            acknowledgment.acknowledge();
            log.info("Inventory reply processed successfully for orderId: {}", reply.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process inventory reply for orderId: {}", reply.getOrderId(), e);
            // Don't acknowledge - message will be redelivered
        }
    }
}