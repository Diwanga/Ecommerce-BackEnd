package com.diwangaamasith.PaymentService.service;

import com.diwangaamasith.PaymentService.model.PaymentRequest;
import com.diwangaamasith.PaymentService.model.PaymentResponse;

public interface PaymentService {
    long doPayment(PaymentRequest paymentRequest);

    PaymentResponse getPaymentDetailsByOrderId(String orderId);
}
