package com.orderms.inventory.kafka.consumer;

import com.orderms.inventory.kafka.event.InventoryCommand;
import com.orderms.inventory.kafka.event.ReleaseStockCommand;
import com.orderms.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryCommandConsumer {

    private final InventoryService inventoryService;

    @Value("${instance.id:default}")
    private String instanceId;

    @KafkaListener(
            topics = "inventory-commands",
            groupId = "inventory-service-group",
            containerFactory = "inventoryCommandKafkaListenerContainerFactory"
    )
    public void consumeInventoryCommand(InventoryCommand command, Acknowledgment acknowledgment) {
        log.info("Instance {} - Received InventoryCommand {} for orderId: {}",
                instanceId, command.getCommandId(), command.getOrderId());

        try {
            inventoryService.reserveStock(command);
            acknowledgment.acknowledge();
            log.info("Instance {} - InventoryCommand processed successfully for orderId: {}",
                    instanceId, command.getOrderId());
        } catch (Exception e) {
            log.error("Instance {} - Failed to process InventoryCommand for orderId: {}",
                    instanceId, command.getOrderId(), e);
            // Don't acknowledge - message will be redelivered
        }
    }

    @KafkaListener(
            topics = "inventory-commands",
            groupId = "inventory-service-group",
            containerFactory = "releaseStockKafkaListenerContainerFactory"
    )
    public void consumeReleaseStockCommand(ReleaseStockCommand command, Acknowledgment acknowledgment) {
        log.info("Instance {} - Received ReleaseStockCommand {} for orderId: {}",
                instanceId, command.getCommandId(), command.getOrderId());

        try {
            for (InventoryCommand.InventoryItem item : command.getItems()) {
                inventoryService.releaseStock(item);
            }
            acknowledgment.acknowledge();
            log.info("Instance {} - ReleaseStockCommand processed successfully for orderId: {}",
                    instanceId, command.getOrderId());
        } catch (Exception e) {
            log.error("Instance {} - Failed to process ReleaseStockCommand for orderId: {}",
                    instanceId, command.getOrderId(), e);
            // Don't acknowledge - message will be redelivered
        }
    }
}