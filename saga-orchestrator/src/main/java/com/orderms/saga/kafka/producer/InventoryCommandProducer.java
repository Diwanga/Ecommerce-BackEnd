package com.orderms.saga.kafka.producer;

import com.orderms.saga.kafka.event.InventoryCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryCommandProducer {

    private static final String INVENTORY_COMMANDS_TOPIC = "inventory-commands";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendInventoryCommand(InventoryCommand command) {
        log.info("Sending InventoryCommand for orderId: {}", command.getOrderId());
        kafkaTemplate.send(INVENTORY_COMMANDS_TOPIC, command.getOrderId().toString(), command);
    }
}