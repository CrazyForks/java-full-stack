package com.example.bootserver.order.web;

import com.example.bootserver.common.result.Result;
import com.example.bootserver.config.OpenApiConfig;
import com.example.bootserver.order.application.CreatedOrder;
import com.example.bootserver.order.application.OrderDetail;
import com.example.bootserver.order.application.OrderPage;
import com.example.bootserver.order.application.OrderQueryService;
import com.example.bootserver.order.application.OrderService;
import com.example.bootserver.order.domain.OrderSelection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final OrderQueryService queries;

    public OrderController(OrderService orders, OrderQueryService queries) {
        this.orders = orders;
        this.queries = queries;
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

    @GetMapping
    @Operation(summary = "分页查询本人订单", description = "按创建时间和订单 ID 倒序，仅返回当前 JWT 用户的订单。")
    public Result<OrderPageResponse> listOrders(@AuthenticationPrincipal Long userId,
                                                @Valid @ModelAttribute OrderPageRequest request) {
        OrderPage page = queries.listOrders(userId, request.getPage(), request.getSize());
        return Result.ok(new OrderPageResponse(page.page(), page.size(), page.total(), page.items().stream()
                .map(order -> new OrderSummaryResponse(order.id(), order.orderNo(), order.status(),
                        order.totalAmount(), order.createTime()))
                .toList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询本人订单详情", description = "返回订单头和下单价格快照；不存在或非本人订单统一返回 404。")
    public Result<OrderDetailResponse> getOrder(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        OrderDetail order = queries.getOrder(userId, id);
        return Result.ok(new OrderDetailResponse(order.id(), order.orderNo(), order.status(), order.totalAmount(),
                order.createTime(), order.items().stream()
                .map(item -> new OrderItemResponse(item.skuId(), item.quantity(), item.price()))
                .toList()));
    }
}
