package com.orderms.payment.kafka.consumer;

import com.orderms.payment.kafka.event.PaymentCommand;
import com.orderms.payment.kafka.event.RefundCommand;
import com.orderms.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCommandConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "payment-commands",
            groupId = "payment-service-group",
            containerFactory = "paymentCommandKafkaListenerContainerFactory"
    )
    public void consumePaymentCommand(PaymentCommand command, Acknowledgment acknowledgment) {
        log.info("Received PaymentCommand for orderId: {}", command.getOrderId());

        try {
            paymentService.processPayment(command);
            acknowledgment.acknowledge();
            log.info("PaymentCommand processed successfully for orderId: {}", command.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process PaymentCommand for orderId: {}", command.getOrderId(), e);
            // Don't acknowledge - message will be redelivered
        }
    }

    @KafkaListener(
            topics = "refund-commands",
            groupId = "payment-service-group",
            containerFactory = "refundCommandKafkaListenerContainerFactory"
    )
    public void consumeRefundCommand(RefundCommand command, Acknowledgment acknowledgment) {
        log.info("Received RefundCommand for orderId: {}", command.getOrderId());

        try {
            paymentService.refundPayment(command.getOrderId(), command.getReason());
            acknowledgment.acknowledge();
            log.info("RefundCommand processed successfully for orderId: {}", command.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process RefundCommand for orderId: {}", command.getOrderId(), e);
            // Don't acknowledge - message will be redelivered
        }
    }
}