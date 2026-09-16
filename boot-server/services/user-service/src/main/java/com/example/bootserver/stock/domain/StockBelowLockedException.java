package com.example.bootserver.stock.domain;

/** 目标总量低于已锁定量，库存聚合拒绝状态变更。 */
public class StockBelowLockedException extends RuntimeException {

    public StockBelowLockedException() {
        super("库存总量不能低于已锁定量");
    }
}
