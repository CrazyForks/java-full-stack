package com.example.bootserver.payment.web;

import com.example.bootserver.payment.domain.PaymentStatus;

import java.math.BigDecimal;

/** 同步模拟支付成功响应。 */
public record PaymentResponse(Long id, Long orderId, String payNo,
                              BigDecimal amount, PaymentStatus status) {
}
