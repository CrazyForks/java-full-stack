package com.example.bootserver.order.application;

import com.example.bootserver.common.error.BusinessException;
import com.example.bootserver.common.error.ErrorCode;
import com.example.bootserver.order.domain.Order;
import com.example.bootserver.order.domain.OrderLine;
import com.example.bootserver.order.domain.OrderRepository;
import com.example.bootserver.service.OrderableSkuQuote;
import com.example.bootserver.service.ProductQueryService;
import com.example.bootserver.stock.application.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 下单用例的唯一事务边界：快照报价、订单写入和库存预扣全部同成同败。 */
@Service
public class OrderService {
    private final ProductQueryService products;
    private final StockService stocks;
    private final OrderRepository orders;
    private final OrderNumberGenerator numbers;

    public OrderService(ProductQueryService products, StockService stocks, OrderRepository orders,
                        OrderNumberGenerator numbers) {
        this.products = products;
        this.stocks = stocks;
        this.orders = orders;
        this.numbers = numbers;
    }

    @Transactional
    public CreatedOrder createOrder(Long userId, List<CreateOrderItem> items) {
        if (items == null || items.isEmpty() || items.size() > 50) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "订单明细数量必须在 1 到 50 之间");
        }
        Set<Long> skuIds = new HashSet<>();
        for (CreateOrderItem item : items) {
            if (item == null || item.skuId() == null || item.skuId() <= 0
                    || item.quantity() < 1 || item.quantity() > 999 || !skuIds.add(item.skuId())) {
                throw new BusinessException(ErrorCode.PARAMETER_ERROR, "订单 SKU 或数量不合法，且不能重复");
            }
        }
        Map<Long, OrderableSkuQuote> quotes = products.listOrderableSkuQuotes(skuIds).stream()
                .collect(Collectors.toMap(OrderableSkuQuote::skuId, Function.identity()));
        if (quotes.size() != skuIds.size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SKU 不存在或未上架");
        }
        try {
            List<OrderLine> lines = items.stream()
                    .map(item -> new OrderLine(item.skuId(), item.quantity(),
                            quotes.get(item.skuId()).unitPrice()))
                    .toList();
            Order order = Order.create(userId, numbers.nextOrderNo(), lines);
            Long id = orders.insert(order);
            for (OrderLine line : lines) {
                if (!stocks.reserveIfAvailable(line.skuId(), line.quantity())) {
                    throw new BusinessException(ErrorCode.CONFLICT, "库存未配置或可用量不足");
                }
            }
            return new CreatedOrder(id, order.orderNo(), order.status(), order.totalAmount());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, exception.getMessage());
        }
    }
}
