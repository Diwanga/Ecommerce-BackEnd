package com.orderms.saga.kafka.consumer;

import com.orderms.saga.kafka.event.PaymentCompletedEvent;
import com.orderms.saga.service.SagaOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReplyConsumer {

    private final SagaOrchestrationService sagaOrchestrationService;

    @KafkaListener(
            topics = "payment-replies",
            groupId = "saga-orchestrator-group",
            containerFactory = "paymentReplyKafkaListenerContainerFactory"
    )
    public void consumePaymentCompletedEvent(PaymentCompletedEvent event, Acknowledgment acknowledgment) {
        log.info("Received PaymentCompletedEvent for orderId: {}, success: {}",
                event.getOrderId(), event.isSuccess());

        try {
            sagaOrchestrationService.handlePaymentCompleted(event);
            acknowledgment.acknowledge();
            log.info("Payment reply processed successfully for orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process payment reply for orderId: {}", event.getOrderId(), e);
            // Don't acknowledge - message will be redelivered
        }
    }
}