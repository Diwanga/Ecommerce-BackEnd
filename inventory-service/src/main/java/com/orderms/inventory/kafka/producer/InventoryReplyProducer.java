package com.orderms.inventory.kafka.producer;

import com.orderms.inventory.kafka.event.InventoryReply;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReplyProducer {

    private static final String INVENTORY_REPLIES_TOPIC = "inventory-replies";

    private final KafkaTemplate<String, InventoryReply> kafkaTemplate;

    public void publishInventoryReply(InventoryReply reply) {
        log.info("Publishing InventoryReply for orderId: {}, success: {}, instance: {}",
                reply.getOrderId(), reply.isSuccess(), reply.getInstanceId());

        CompletableFuture<SendResult<String, InventoryReply>> future =
                kafkaTemplate.send(INVENTORY_REPLIES_TOPIC, reply.getOrderId().toString(), reply);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("InventoryReply published successfully for orderId: {} - Offset: {}",
                        reply.getOrderId(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish InventoryReply for orderId: {}",
                        reply.getOrderId(), ex);
            }
        });
    }
}