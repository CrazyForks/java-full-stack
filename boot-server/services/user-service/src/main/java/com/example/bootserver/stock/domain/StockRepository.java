package com.example.bootserver.stock.domain;

import java.util.Optional;

/** 库存聚合的持久化契约；写入必须由基础设施层保证条件更新。 */
public interface StockRepository {

    Optional<Stock> getBySkuId(Long skuId);

    void insert(Stock stock);

    int updateTotalIfEnough(Long skuId, long total);
}
