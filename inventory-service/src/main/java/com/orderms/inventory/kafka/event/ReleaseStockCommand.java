package com.orderms.inventory.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseStockCommand {

    private String commandId;
    private Long orderId;
    private List<InventoryCommand.InventoryItem> items;
}