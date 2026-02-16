package com.orderms.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {

    private Long id;
    private String productId;
    private String productName;
    private Integer availableStock;
    private Integer reservedStock;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}