package com.example.bootserver.order.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.bootserver.order.domain.ExistingOrder;
import com.example.bootserver.order.domain.IdempotencyKey;
import com.example.bootserver.order.domain.Order;
import com.example.bootserver.order.domain.OrderLine;
import com.example.bootserver.order.domain.OrderRepository;
import com.example.bootserver.order.domain.OrderRequestFingerprint;
import com.example.bootserver.order.domain.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** 把订单聚合转换成两个表的持久化对象。 */
@Repository
public class MyBatisOrderRepository implements OrderRepository {
    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;

    public MyBatisOrderRepository(OrderMapper orderMapper, OrderItemMapper itemMapper) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
    }

    @Override
    public Long insert(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderNo(order.orderNo());
        entity.setUserId(order.userId());
        entity.setIdempotencyKey(order.idempotencyKey().value());
        entity.setRequestFingerprint(order.fingerprint().value());
        entity.setStatus(order.status().name());
        entity.setTotalAmount(order.totalAmount());
        orderMapper.insert(entity);
        for (OrderLine line : order.lines()) {
            OrderItemEntity item = new OrderItemEntity();
            item.setOrderId(entity.getId());
            item.setSkuId(line.skuId());
            item.setQuantity(line.quantity());
            item.setPrice(line.unitPrice());
            itemMapper.insert(item);
        }
        return entity.getId();
    }

    @Override
    public Optional<ExistingOrder> getByUserIdAndIdempotencyKey(Long userId, IdempotencyKey key) {
        OrderEntity entity = orderMapper.selectOne(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getIdempotencyKey, key.value()));
        return Optional.ofNullable(entity).map(found -> new ExistingOrder(found.getId(), found.getOrderNo(),
                OrderStatus.valueOf(found.getStatus()), found.getTotalAmount(),
                new OrderRequestFingerprint(found.getRequestFingerprint())));
    }
}
