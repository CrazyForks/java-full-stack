package com.example.bootserver.stock.application;

import com.example.bootserver.common.error.BusinessException;
import com.example.bootserver.common.error.ErrorCode;
import com.example.bootserver.service.ProductQueryService;
import com.example.bootserver.stock.domain.Stock;
import com.example.bootserver.stock.domain.StockBelowLockedException;
import com.example.bootserver.stock.domain.StockRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 库存管理用例；商品侧只提供 SKU 存在性，库存表仅由本上下文写入。 */
@Service
public class StockService {

    private final StockRepository repository;
    private final StockWriteStepService writeSteps;
    private final ProductQueryService productQueryService;

    public StockService(StockRepository repository, StockWriteStepService writeSteps,
                        ProductQueryService productQueryService) {
        this.repository = repository;
        this.writeSteps = writeSteps;
        this.productQueryService = productQueryService;
    }

    /** 设置绝对总量；先由聚合检查快照，再由 SQL 在写入时检查最新锁定量。 */
    public Stock setTotal(Long skuId, long total) {
        if (!productQueryService.hasSkuById(skuId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SKU 不存在");
        }
        Stock existing = repository.getBySkuId(skuId).orElse(null);
        if (existing == null) {
            try {
                writeSteps.insert(Stock.create(skuId, total));
                return getBySkuId(skuId);
            } catch (DuplicateKeyException ignored) {
                // 同一 SKU 的另一请求先插入；独立事务回滚后按最新锁定量重试条件更新。
                existing = repository.getBySkuId(skuId).orElseThrow();
            }
        }
        try {
            existing.withTotal(total);
        } catch (StockBelowLockedException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, exception.getMessage());
        }
        if (writeSteps.updateTotalIfEnough(skuId, total) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "库存锁定量已变化，请重新查询后再设置");
        }
        return getBySkuId(skuId);
    }

    @Transactional(readOnly = true)
    public Stock getBySkuId(Long skuId) {
        return repository.getBySkuId(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "库存尚未配置"));
    }
}
