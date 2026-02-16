package com.orderms.inventory.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReply {

    private Long orderId;
    private boolean success;
    private String errorMessage;
    private String instanceId;
}