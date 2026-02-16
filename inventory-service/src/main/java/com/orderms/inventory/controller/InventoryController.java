package com.orderms.inventory.controller;

import com.orderms.inventory.dto.InventoryResponse;
import com.orderms.inventory.dto.StockCheckRequest;
import com.orderms.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Value("${instance.id:default}")
    private String instanceId;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("instance", instanceId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> checkStock(@Valid @RequestBody StockCheckRequest request) {
        log.info("Instance {} - Checking stock for product: {}, quantity: {}",
                instanceId, request.getProductId(), request.getQuantity());

        boolean available = inventoryService.checkStock(request.getProductId(), request.getQuantity());

        Map<String, Object> response = new HashMap<>();
        response.put("productId", request.getProductId());
        response.put("requestedQuantity", request.getQuantity());
        response.put("available", available);
        response.put("instance", instanceId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable String productId) {
        log.info("Instance {} - Fetching inventory for product: {}", instanceId, productId);

        InventoryResponse response = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAllInventory() {
        log.info("Instance {} - Fetching all inventory", instanceId);

        List<InventoryResponse> inventory = inventoryService.getAllInventory();
        return ResponseEntity.ok(inventory);
    }
}