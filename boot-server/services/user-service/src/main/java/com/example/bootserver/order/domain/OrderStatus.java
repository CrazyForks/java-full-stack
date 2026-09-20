package com.example.bootserver.order.domain;

/** 订单生命周期机器值；迁移规则由状态类型集中守护。 */
public enum OrderStatus {
    CREATED, PAID, SHIPPED, DONE, CANCELLED;

    /** 模拟支付只允许从待支付状态进入已支付。 */
    public OrderStatus pay() {
        if (this != CREATED) {
            throw new IllegalOrderStateException("当前订单状态不允许支付");
        }
        return PAID;
    }

    /** 只有待支付订单可由用户主动取消。 */
    public OrderStatus cancel() {
        if (this != CREATED) {
            throw new IllegalOrderStateException("当前订单状态不允许取消");
        }
        return CANCELLED;
    }
}
