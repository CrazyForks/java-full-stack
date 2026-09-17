package com.example.bootserver.order.domain;

import java.util.Optional;

/** 订单聚合的持久化端口；订单头与明细写入加入调用方事务。 */
public interface OrderRepository {
    Long insert(Order order);

    Optional<ExistingOrder> getByUserIdAndIdempotencyKey(Long userId, IdempotencyKey key);
}
