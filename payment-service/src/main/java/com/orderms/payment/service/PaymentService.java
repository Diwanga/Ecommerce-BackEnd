package com.orderms.payment.service;

import com.orderms.payment.dto.PaymentRequest;
import com.orderms.payment.dto.PaymentResponse;
import com.orderms.payment.entity.Payment;
import com.orderms.payment.entity.PaymentStatus;
import com.orderms.payment.exception.PaymentFailedException;
import com.orderms.payment.exception.PaymentNotFoundException;
import com.orderms.payment.kafka.event.PaymentCommand;
import com.orderms.payment.kafka.event.PaymentCompletedEvent;
import com.orderms.payment.kafka.producer.PaymentReplyProducer;
import com.orderms.payment.repository.PaymentRepository;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final PaymentReplyProducer paymentReplyProducer;

    @Transactional
    @Retry(name = "database")
    //@TimeLimiter(name = "payment")
    public void processPayment(PaymentCommand command) {
        log.info("Processing payment command for orderId: {}", command.getOrderId());

        // Create payment record
        Payment payment = new Payment();
        payment.setOrderId(command.getOrderId());
        payment.setAmount(command.getAmount());
        payment.setPaymentMethod(command.getPaymentMethod());
        payment.setStatus(PaymentStatus.PROCESSING);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment record created with id: {}", savedPayment.getId());

        try {
            // Call external payment gateway
            PaymentGatewayService.PaymentGatewayResponse gatewayResponse =
                    paymentGatewayService.processPayment(
                            command.getOrderId(),
                            command.getAmount(),
                            command.getPaymentMethod()
                    );

            if (gatewayResponse.isSuccess()) {
                // Payment successful
                savedPayment.setStatus(PaymentStatus.COMPLETED);
                savedPayment.setTransactionId(gatewayResponse.getTransactionId());
                paymentRepository.save(savedPayment);

                log.info("Payment completed successfully for orderId: {}, transactionId: {}",
                        command.getOrderId(), gatewayResponse.getTransactionId());

                // Publish success event
                PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                        .paymentId(savedPayment.getId())
                        .orderId(command.getOrderId())
                        .amount(command.getAmount())
                        .success(true)
                        .transactionId(gatewayResponse.getTransactionId())
                        .build();

                paymentReplyProducer.publishPaymentCompletedEvent(event);

            } else {
                // Payment failed
                savedPayment.setStatus(PaymentStatus.FAILED);
                savedPayment.setErrorMessage(gatewayResponse.getMessage());
                paymentRepository.save(savedPayment);

                log.error("Payment failed for orderId: {} - Reason: {}",
                        command.getOrderId(), gatewayResponse.getMessage());

                // Publish failure event
                PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                        .paymentId(savedPayment.getId())
                        .orderId(command.getOrderId())
                        .amount(command.getAmount())
                        .success(false)
                        .errorMessage(gatewayResponse.getMessage())
                        .build();

                paymentReplyProducer.publishPaymentCompletedEvent(event);
            }

        } catch (Exception e) {
            log.error("Exception occurred while processing payment for orderId: {}", command.getOrderId(), e);

            savedPayment.setStatus(PaymentStatus.FAILED);
            savedPayment.setErrorMessage("Payment processing error: " + e.getMessage());
            paymentRepository.save(savedPayment);

            // Publish failure event
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .paymentId(savedPayment.getId())
                    .orderId(command.getOrderId())
                    .amount(command.getAmount())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();

            paymentReplyProducer.publishPaymentCompletedEvent(event);
        }
    }

    @Transactional
    @Retry(name = "database")
    public void refundPayment(Long orderId, String reason) {
        log.info("Processing refund for orderId: {} - Reason: {}", orderId, reason);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for orderId: " + orderId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            log.warn("Cannot refund payment with status: {}", payment.getStatus());
            throw new PaymentFailedException("Cannot refund payment with status: " + payment.getStatus());
        }

        try {
            // Call payment gateway for refund
            PaymentGatewayService.PaymentGatewayResponse refundResponse =
                    paymentGatewayService.refundPayment(payment.getTransactionId(), payment.getAmount());

            if (refundResponse.isSuccess()) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
                log.info("Refund successful for orderId: {}", orderId);
            } else {
                log.error("Refund failed for orderId: {} - Reason: {}", orderId, refundResponse.getMessage());
                throw new PaymentFailedException("Refund failed: " + refundResponse.getMessage());
            }

        } catch (Exception e) {
            log.error("Exception occurred while refunding payment for orderId: {}", orderId, e);
            throw new PaymentFailedException("Refund processing error: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        log.info("Fetching payment with id: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + paymentId));

        return mapToPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        log.info("Fetching payment for orderId: {}", orderId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for orderId: " + orderId));

        return mapToPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        log.info("Fetching all payments");

        List<Payment> payments = paymentRepository.findAll();
        return payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .errorMessage(payment.getErrorMessage())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}