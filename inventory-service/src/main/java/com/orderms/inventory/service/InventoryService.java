package com.orderms.inventory.service;

import com.orderms.inventory.dto.InventoryResponse;
import com.orderms.inventory.entity.Inventory;
import com.orderms.inventory.entity.ProcessedCommand;
import com.orderms.inventory.exception.InsufficientStockException;
import com.orderms.inventory.exception.InventoryNotFoundException;
import com.orderms.inventory.kafka.event.InventoryCommand;
import com.orderms.inventory.kafka.event.InventoryReply;
import com.orderms.inventory.kafka.producer.InventoryReplyProducer;
import com.orderms.inventory.repository.InventoryRepository;
import com.orderms.inventory.repository.ProcessedCommandRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.*;
import java.util.List;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProcessedCommandRepository processedCommandRepository;
    private final InventoryReplyProducer inventoryReplyProducer;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${instance.id:default}")
    private String instanceId;

    private static final String REDIS_COMMAND_PREFIX = "processed_command:";

    @Transactional
    @Bulkhead(name = "inventory-processing")
    @CircuitBreaker(name = "database")
    @Retry(name = "inventory-update", fallbackMethod = "reserveStockFallback")
    public void reserveStock(InventoryCommand command) {
        String commandId = command.getCommandId();
        log.info("Instance {} - Processing InventoryCommand {} for orderId: {}",
                instanceId, commandId, command.getOrderId());

        // Check idempotency using Redis (fast path)
        if (isCommandProcessed(commandId)) {
            log.info("Instance {} - Command {} already processed (Redis cache hit)", instanceId, commandId);
            return;
        }

        // Check idempotency using database (slow path)
        if (processedCommandRepository.existsByCommandId(commandId)) {
            log.info("Instance {} - Command {} already processed (Database hit)", instanceId, commandId);
            cacheProcessedCommand(commandId);
            return;
        }

        try {
            // Reserve stock for all items
            for (InventoryCommand.InventoryItem item : command.getItems()) {
                reserveStockForProduct(item.getProductId(), item.getQuantity());
            }

            // Mark command as processed
            ProcessedCommand processedCommand = new ProcessedCommand();
            processedCommand.setCommandId(commandId);
            processedCommandRepository.save(processedCommand);
            cacheProcessedCommand(commandId);

            log.info("Instance {} - Stock reserved successfully for orderId: {}",
                    instanceId, command.getOrderId());

            // Publish success reply
            InventoryReply reply = InventoryReply.builder()
                    .orderId(command.getOrderId())
                    .success(true)
                    .instanceId(instanceId)
                    .build();

            inventoryReplyProducer.publishInventoryReply(reply);

        } catch (InsufficientStockException e) {
            log.error("Instance {} - Insufficient stock for orderId: {}", instanceId, command.getOrderId());

            // Publish failure reply
            InventoryReply reply = InventoryReply.builder()
                    .orderId(command.getOrderId())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .instanceId(instanceId)
                    .build();

            inventoryReplyProducer.publishInventoryReply(reply);

        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Instance {} - Optimistic lock failure for orderId: {} - Retrying...",
                    instanceId, command.getOrderId());
            throw e; // Will trigger retry
        }
    }

    @Transactional
    @Retry(name = "inventory-update")
    public void reserveStockForProduct(String productId, Integer quantity) {
        log.info("Instance {} - Reserving {} units of product {}", instanceId, quantity, productId);

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Product not found: " + productId));

        if (inventory.getAvailableStock() < quantity) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                            productId, inventory.getAvailableStock(), quantity));
        }

        // Update stock with optimistic locking
        inventory.setAvailableStock(inventory.getAvailableStock() - quantity);
        inventory.setReservedStock(inventory.getReservedStock() + quantity);

        inventoryRepository.save(inventory);

        log.info("Instance {} - Reserved {} units. Product: {}, Available: {}, Reserved: {}",
                instanceId, quantity, productId, inventory.getAvailableStock(), inventory.getReservedStock());
    }

    @Transactional
    @Retry(name = "inventory-update")
    public void releaseStock(InventoryCommand.InventoryItem item) {
        log.info("Instance {} - Releasing {} units of product {}",
                instanceId, item.getQuantity(), item.getProductId());

        Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException("Product not found: " + item.getProductId()));

        // Release reserved stock back to available
        inventory.setReservedStock(inventory.getReservedStock() - item.getQuantity());
        inventory.setAvailableStock(inventory.getAvailableStock() + item.getQuantity());

        inventoryRepository.save(inventory);

        log.info("Instance {} - Released {} units. Product: {}, Available: {}, Reserved: {}",
                instanceId, item.getQuantity(), item.getProductId(),
                inventory.getAvailableStock(), inventory.getReservedStock());
    }

    @Transactional(readOnly = true)
    public boolean checkStock(String productId, Integer quantity) {
        return inventoryRepository.findByProductIdWithSufficientStock(productId, quantity)
                .isPresent();
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(String productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Product not found: " + productId));

        return mapToInventoryResponse(inventory);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(this::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    // Idempotency helpers
    private boolean isCommandProcessed(String commandId) {
        String key = REDIS_COMMAND_PREFIX + commandId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private void cacheProcessedCommand(String commandId) {
        String key = REDIS_COMMAND_PREFIX + commandId;
        redisTemplate.opsForValue().set(key, "true", 24, TimeUnit.HOURS);
    }

    // Fallback method for circuit breaker
    private void reserveStockFallback(InventoryCommand command, Exception ex) {
        log.error("Instance {} - Fallback triggered for orderId: {}", instanceId, command.getOrderId(), ex);

        InventoryReply reply = InventoryReply.builder()
                .orderId(command.getOrderId())
                .success(false)
                .errorMessage("Inventory service temporarily unavailable")
                .instanceId(instanceId)
                .build();

        inventoryReplyProducer.publishInventoryReply(reply);
    }

    private InventoryResponse mapToInventoryResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .productName(inventory.getProductName())
                .availableStock(inventory.getAvailableStock())
                .reservedStock(inventory.getReservedStock())
                .version(inventory.getVersion())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}