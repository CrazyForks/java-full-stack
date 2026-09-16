package com.example.bootserver.cart.infrastructure;

import lombok.Data;

import java.math.BigDecimal;

/** 购物车与商品关联查询的数据库行，避免把 Mapper 行模型暴露到领域层。 */
@Data
public class CartItemRow {

    private Long id;
    private Long skuId;
    private String skuCode;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
}
