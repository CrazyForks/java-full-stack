package com.example.bootserver.cart.domain;

import java.math.BigDecimal;

/** 购物车只读投影；价格来自当前 SKU，而非加购时的快照。 */
public record CartEntry(Long id, Long skuId, String skuCode, String productName,
                        BigDecimal unitPrice, int quantity) {
}
