package com.example.bootserver.order.application;

/** 应用用例输入，不包含客户端不可决定的价格和用户身份。 */
public record CreateOrderItem(Long skuId, int quantity) {
}
