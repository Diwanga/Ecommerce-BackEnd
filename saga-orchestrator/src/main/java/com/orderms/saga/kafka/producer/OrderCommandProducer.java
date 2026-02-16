package com.orderms.saga.kafka.producer;

import com.orderms.saga.kafka.event.OrderCancelledCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCommandProducer {

    private static final String ORDER_COMMANDS_TOPIC = "order-commands";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderCancelledCommand(OrderCancelledCommand command) {
        log.info("Sending OrderCancelledCommand for orderId: {}", command.getOrderId());
        kafkaTemplate.send(ORDER_COMMANDS_TOPIC, command.getOrderId().toString(), command);
    }
}