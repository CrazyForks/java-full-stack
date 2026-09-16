package com.example.bootserver.cart.infrastructure;

import com.example.bootserver.cart.domain.CartEntry;
import com.example.bootserver.cart.domain.CartItemRepository;
import com.example.bootserver.cart.domain.CartQuantity;
import com.example.bootserver.entity.Product;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 将购物车端口映射到 MyBatis；商品表只通过只读查询参与可售判断与现价投影。 */
@Repository
public class MyBatisCartItemRepository implements CartItemRepository {

    private final CartItemMapper mapper;

    public MyBatisCartItemRepository(CartItemMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean isOnSaleSku(Long skuId) {
        return mapper.countOnSaleSku(skuId, Product.STATUS_ON_SALE) > 0;
    }

    @Override
    public int increase(Long userId, Long skuId, CartQuantity delta) {
        return mapper.increase(userId, skuId, delta.value(), CartQuantity.MAX);
    }

    @Override
    public void insert(Long userId, Long skuId, CartQuantity quantity) {
        CartItemEntity entity = new CartItemEntity();
        entity.setUserId(userId);
        entity.setSkuId(skuId);
        entity.setQuantity(quantity.value());
        mapper.insert(entity);
    }

    @Override
    public int replace(Long userId, Long itemId, CartQuantity quantity) {
        return mapper.replace(userId, itemId, quantity.value());
    }

    @Override
    public int delete(Long userId, Long itemId) {
        return mapper.deleteOwned(userId, itemId);
    }

    @Override
    public List<CartEntry> listByUserId(Long userId) {
        return mapper.listByUserId(userId).stream()
                .map(row -> new CartEntry(row.getId(), row.getSkuId(), row.getSkuCode(),
                        row.getProductName(), row.getUnitPrice(), row.getQuantity()))
                .toList();
    }
}
