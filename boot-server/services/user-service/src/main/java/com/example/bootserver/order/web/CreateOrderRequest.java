package com.example.bootserver.order.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 下单请求；用户身份、价格和金额不接受客户端输入。 */
public record CreateOrderRequest(@NotEmpty @Size(max = 50) List<@NotNull @Valid CreateOrderItemRequest> items) {
}
