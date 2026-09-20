package com.example.bootserver.order.application;

import com.example.bootserver.common.error.BusinessException;
import com.example.bootserver.common.error.ErrorCode;
import com.example.bootserver.order.domain.IllegalOrderStateException;
import com.example.bootserver.order.domain.OrderRepository;
import com.example.bootserver.order.domain.OrderStatus;
import org.springframework.stereotype.Service;

/** 向支付上下文提供订单状态认领与支付快照，订单表仍只由订单上下文修改。 */
@Service
public class OrderPaymentService {
    private final OrderRepository orders;
    private final OrderQueryRepository queries;

    public OrderPaymentService(OrderRepository orders, OrderQueryRepository queries) {
        this.orders = orders;
        this.queries = queries;
    }

    public PayableOrder claimForPayment(Long userId, Long orderId) {
        OrderStatus target = OrderStatus.CREATED.pay();
        if (orders.updateStatusIfCurrent(orderId, userId, OrderStatus.CREATED, target) == 0) {
            OrderStatus current = orders.getStatusByIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
            try {
                current.pay();
            } catch (IllegalOrderStateException exception) {
                throw new BusinessException(ErrorCode.CONFLICT, exception.getMessage());
            }
            throw new BusinessException(ErrorCode.CONFLICT, "订单状态已变化，请重试");
        }
        OrderDetail order = queries.getByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        return new PayableOrder(order.id(), order.totalAmount(), order.items());
    }
}
