package com.example.bootserver.order.application;

import com.example.bootserver.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 后台订单列表项；包含订单所有者标识。 */
public record AdminOrderSummary(Long id, String orderNo, Long userId, OrderStatus status,
                                BigDecimal totalAmount, LocalDateTime createTime) {
}
