package com.orderms.payment.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCommand {

    private String commandId;
    private Long orderId;
    private BigDecimal amount;
    private String paymentMethod;
}