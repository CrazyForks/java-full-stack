package com.example.bootserver.order.application;

import com.example.bootserver.common.error.BusinessException;
import com.example.bootserver.common.error.ErrorCode;
import com.example.bootserver.order.domain.IllegalOrderStateException;
import com.example.bootserver.order.domain.OrderRepository;
import com.example.bootserver.order.domain.OrderStatus;
import com.example.bootserver.stock.application.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 本人取消订单；状态迁移与全部库存释放在同一本地事务提交。 */
@Service
public class OrderCancellationService {
    private final OrderRepository orders;
    private final OrderQueryRepository queries;
    private final StockService stocks;

    public OrderCancellationService(OrderRepository orders, OrderQueryRepository queries, StockService stocks) {
        this.orders = orders;
        this.queries = queries;
        this.stocks = stocks;
    }

    @Transactional
    public void cancel(Long userId, Long orderId) {
        OrderStatus target = OrderStatus.CREATED.cancel();
        if (orders.updateStatusIfCurrent(orderId, userId, OrderStatus.CREATED, target) == 0) {
            rejectMissingOrIllegalState(userId, orderId);
        }
        OrderDetail order = queries.getByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        order.items().forEach(item -> stocks.releaseReserved(item.skuId(), item.quantity()));
    }

    private void rejectMissingOrIllegalState(Long userId, Long orderId) {
        OrderStatus current = orders.getStatusByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        try {
            current.cancel();
        } catch (IllegalOrderStateException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, exception.getMessage());
        }
        throw new BusinessException(ErrorCode.CONFLICT, "订单状态已变化，请重试");
    }
}
