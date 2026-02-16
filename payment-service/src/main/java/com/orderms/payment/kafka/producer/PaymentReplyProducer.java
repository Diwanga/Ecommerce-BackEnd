package com.orderms.payment.kafka.producer;

import com.orderms.payment.kafka.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReplyProducer {

    private static final String PAYMENT_REPLIES_TOPIC = "payment-replies";

    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;

    public void publishPaymentCompletedEvent(PaymentCompletedEvent event) {
        log.info("Publishing PaymentCompletedEvent for orderId: {}, success: {}",
                event.getOrderId(), event.isSuccess());

        CompletableFuture<SendResult<String, PaymentCompletedEvent>> future =
                kafkaTemplate.send(PAYMENT_REPLIES_TOPIC, event.getOrderId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("PaymentCompletedEvent published successfully for orderId: {} - Offset: {}",
                        event.getOrderId(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish PaymentCompletedEvent for orderId: {}",
                        event.getOrderId(), ex);
            }
        });
    }
}