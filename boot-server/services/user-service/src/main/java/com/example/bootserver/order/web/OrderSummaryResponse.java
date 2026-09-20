package com.example.bootserver.order.web;

import com.example.bootserver.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单分页列表项。 */
public record OrderSummaryResponse(Long id, String orderNo, OrderStatus status,
                                   BigDecimal totalAmount, LocalDateTime createTime) {
}
