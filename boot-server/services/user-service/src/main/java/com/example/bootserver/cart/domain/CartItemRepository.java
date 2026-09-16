package com.example.bootserver.cart.domain;

import java.util.List;

/** 购物车持久化端口；原子累加与唯一键是并发不变式的数据库兜底。 */
public interface CartItemRepository {

    boolean isOnSaleSku(Long skuId);

    int increase(Long userId, Long skuId, CartQuantity delta);

    void insert(Long userId, Long skuId, CartQuantity quantity);

    int replace(Long userId, Long itemId, CartQuantity quantity);

    int delete(Long userId, Long itemId);

    List<CartEntry> listByUserId(Long userId);
}
