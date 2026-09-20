package com.example.bootserver.order.web;

import com.example.bootserver.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 当前用户的订单详情。 */
public record OrderDetailResponse(Long id, String orderNo, OrderStatus status, BigDecimal totalAmount,
                                  LocalDateTime createTime, List<OrderItemResponse> items) {
}
