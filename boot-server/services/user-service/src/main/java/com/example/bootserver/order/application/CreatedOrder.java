package com.example.bootserver.order.application;

import com.example.bootserver.order.domain.OrderStatus;

import java.math.BigDecimal;

/** 创建订单的最小结果；明细查询属于后续 M2-07。 */
public record CreatedOrder(Long id, String orderNo, OrderStatus status, BigDecimal totalAmount) {
}
