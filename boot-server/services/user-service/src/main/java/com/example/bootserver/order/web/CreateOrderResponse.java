package com.example.bootserver.order.web;

import com.example.bootserver.order.domain.OrderStatus;

import java.math.BigDecimal;

/** 新建订单回执。 */
public record CreateOrderResponse(Long id, String orderNo, OrderStatus status, BigDecimal totalAmount) {
}
