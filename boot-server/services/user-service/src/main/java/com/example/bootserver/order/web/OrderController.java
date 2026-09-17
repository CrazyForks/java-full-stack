package com.example.bootserver.order.web;

import com.example.bootserver.common.result.Result;
import com.example.bootserver.config.OpenApiConfig;
import com.example.bootserver.order.application.CreatedOrder;
import com.example.bootserver.order.application.OrderService;
import com.example.bootserver.order.domain.OrderSelection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 订单接口；主体身份只取已验证的 JWT。 */
@RestController
@RequestMapping("/orders")
@Tag(name = "订单", description = "当前用户下单")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTHENTICATION)
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    @Operation(summary = "创建订单", description = "同一用户携带同一幂等键重试相同内容返回原单；不同内容返回 409。")
    public Result<CreateOrderResponse> createOrder(@AuthenticationPrincipal Long userId,
                                                    @Valid @RequestBody CreateOrderRequest request) {
        CreatedOrder order = orders.createOrder(userId, request.idempotencyKey(), request.items().stream()
                .map(item -> new OrderSelection(item.skuId(), item.quantity()))
                .toList());
        return Result.ok(new CreateOrderResponse(order.id(), order.orderNo(), order.status(), order.totalAmount()));
    }
}
