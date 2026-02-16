package com.orderms.payment.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

/**
 * Simulates an external payment gateway (like Stripe, PayPal, etc.)
 * In production, this would make actual HTTP calls to payment provider APIs
 */
@Slf4j
@Service
public class PaymentGatewayService {

    private final Random random = new Random();

    @CircuitBreaker(name = "payment-gateway", fallbackMethod = "processPaymentFallback")
    @Retry(name = "payment-processing")
    @RateLimiter(name = "payment-api")
    public PaymentGatewayResponse processPayment(Long orderId, BigDecimal amount, String paymentMethod) {
        log.info("Processing payment for orderId: {}, amount: {}, method: {}", orderId, amount, paymentMethod);

        // Simulate network delay
        simulateNetworkDelay();

        // Simulate 15% payment failure rate for testing
        boolean success = random.nextInt(100) < 85;

        if (success) {
            String transactionId = "TXN-" + UUID.randomUUID().toString();
            log.info("Payment successful - Transaction ID: {}", transactionId);
            return PaymentGatewayResponse.builder()
                    .success(true)
                    .transactionId(transactionId)
                    .message("Payment processed successfully")
                    .build();
        } else {
            log.warn("Payment failed for orderId: {}", orderId);
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .message("Insufficient funds")
                    .build();
        }
    }

    @CircuitBreaker(name = "payment-gateway", fallbackMethod = "refundPaymentFallback")
    @Retry(name = "payment-processing")
    public PaymentGatewayResponse refundPayment(String transactionId, BigDecimal amount) {
        log.info("Processing refund for transactionId: {}, amount: {}", transactionId, amount);

        // Simulate network delay
        simulateNetworkDelay();

        // Refunds usually have higher success rate
        boolean success = random.nextInt(100) < 95;

        if (success) {
            String refundId = "REFUND-" + UUID.randomUUID().toString();
            log.info("Refund successful - Refund ID: {}", refundId);
            return PaymentGatewayResponse.builder()
                    .success(true)
                    .transactionId(refundId)
                    .message("Refund processed successfully")
                    .build();
        } else {
            log.warn("Refund failed for transactionId: {}", transactionId);
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .message("Refund processing failed")
                    .build();
        }
    }

    // Fallback method for circuit breaker
    private PaymentGatewayResponse processPaymentFallback(Long orderId, BigDecimal amount,
                                                          String paymentMethod, Exception ex) {
        log.error("Payment gateway circuit breaker triggered for orderId: {}", orderId, ex);
        return PaymentGatewayResponse.builder()
                .success(false)
                .message("Payment service temporarily unavailable. Please try again later.")
                .build();
    }

    // Fallback method for refund circuit breaker
    private PaymentGatewayResponse refundPaymentFallback(String transactionId, BigDecimal amount, Exception ex) {
        log.error("Refund gateway circuit breaker triggered for transactionId: {}", transactionId, ex);
        return PaymentGatewayResponse.builder()
                .success(false)
                .message("Refund service temporarily unavailable. Will retry automatically.")
                .build();
    }

    private void simulateNetworkDelay() {
        try {
            // Simulate 100-500ms network delay
            Thread.sleep(100 + random.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentGatewayResponse {
        private boolean success;
        private String transactionId;
        private String message;
    }
}