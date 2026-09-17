package com.example.bootserver.order.domain;

/** 订单生命周期机器值；本卡只创建 CREATED，后续卡片实现迁移。 */
public enum OrderStatus {
    CREATED, PAID, SHIPPED, DONE, CANCELLED
}
