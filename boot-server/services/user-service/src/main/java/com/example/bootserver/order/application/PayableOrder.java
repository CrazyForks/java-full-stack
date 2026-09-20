package com.example.bootserver.order.application;

import java.math.BigDecimal;
import java.util.List;

/** 支付上下文所需的订单金额与已预扣明细快照。 */
public record PayableOrder(Long id, BigDecimal totalAmount, List<OrderItemView> items) {
    public PayableOrder {
        items = List.copyOf(items);
    }
}
