package com.example.bootserver.cart.application;

import com.example.bootserver.cart.domain.CartEntry;
import com.example.bootserver.cart.domain.CartItemRepository;
import com.example.bootserver.cart.domain.CartQuantity;
import com.example.bootserver.common.error.BusinessException;
import com.example.bootserver.common.error.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 当前用户的购物车用例；不持有跨步骤事务，唯一键竞争后可在新事务重试。 */
@Service
public class CartService {

    private final CartItemRepository repository;
    private final CartWriteStepService writeSteps;

    public CartService(CartItemRepository repository, CartWriteStepService writeSteps) {
        this.repository = repository;
        this.writeSteps = writeSteps;
    }

    /** 请求数量是增量；条件更新防丢失更新，唯一键解决同时首次加购。 */
    public void addItem(Long userId, Long skuId, int delta) {
        CartQuantity quantity = checkedQuantity(delta);
        if (!repository.isOnSaleSku(skuId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SKU 不存在或未上架");
        }
        if (writeSteps.increase(userId, skuId, quantity) == 1) {
            return;
        }
        try {
            writeSteps.insert(userId, skuId, quantity);
            return;
        } catch (DuplicateKeyException ignored) {
            // 并发首次加购的另一个请求已插入；插入事务已回滚，在新的事务中重试原子累加。
        }
        if (writeSteps.increase(userId, skuId, quantity) == 0) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "购物车数量不能超过 999");
        }
    }

    @Transactional(readOnly = true)
    public List<CartEntry> listMyItems(Long userId) {
        return repository.listByUserId(userId);
    }

    public void updateItemQuantity(Long userId, Long itemId, int quantity) {
        if (writeSteps.replace(userId, itemId, checkedQuantity(quantity)) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "购物车条目不存在");
        }
    }

    public void deleteItem(Long userId, Long itemId) {
        if (writeSteps.delete(userId, itemId) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "购物车条目不存在");
        }
    }

    private CartQuantity checkedQuantity(int value) {
        try {
            return new CartQuantity(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, exception.getMessage());
        }
    }
}
