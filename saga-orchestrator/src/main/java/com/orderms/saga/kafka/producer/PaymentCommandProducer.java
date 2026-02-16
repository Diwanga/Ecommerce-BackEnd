package com.orderms.saga.kafka.producer;

import com.orderms.saga.kafka.event.PaymentCommand;
import com.orderms.saga.kafka.event.RefundCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCommandProducer {

//    private static final String PAYMENT_COMMANDS_TOPIC = "payment-commands";
    private static final String PAYMENT_COMMANDS_TOPIC = "payment-commands";
    private static final String REFUND_COMMANDS_TOPIC = "refund-commands";


    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentCommand(PaymentCommand command) {
        log.info("Sending PaymentCommand for orderId: {}", command.getOrderId());
        kafkaTemplate.send(PAYMENT_COMMANDS_TOPIC, command.getOrderId().toString(), command);
    }

//    public void sendRefundCommand(RefundCommand command) {
//        log.info("Sending RefundCommand for orderId: {}", command.getOrderId());
//        kafkaTemplate.send(PAYMENT_COMMANDS_TOPIC, command.getOrderId().toString(), command);
//    }
    public void sendRefundCommand(RefundCommand command) {
        log.info("Sending RefundCommand for orderId: {}", command.getOrderId());
        kafkaTemplate.send(REFUND_COMMANDS_TOPIC, command.getOrderId().toString(), command);
    }
}