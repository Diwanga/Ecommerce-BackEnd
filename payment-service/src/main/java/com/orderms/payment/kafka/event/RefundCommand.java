package com.orderms.payment.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundCommand {

    private String commandId;
    private Long orderId;
    private Long paymentId;
    private String reason;
}