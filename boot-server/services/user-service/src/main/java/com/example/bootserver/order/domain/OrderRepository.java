package com.example.bootserver.order.domain;

/** 订单聚合的持久化端口；订单头与明细写入加入调用方事务。 */
public interface OrderRepository {
    Long insert(Order order);
}
