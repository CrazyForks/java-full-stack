package com.example.bootserver.order.application;

import java.util.Optional;

/** 订单只读查询端口；所有查询都必须带当前用户标识。 */
public interface OrderQueryRepository {
    OrderPage listByUserId(Long userId, long page, long size);

    Optional<OrderDetail> getByIdAndUserId(Long id, Long userId);
}
