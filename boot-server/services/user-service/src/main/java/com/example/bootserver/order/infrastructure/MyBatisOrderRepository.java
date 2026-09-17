package com.example.bootserver.order.infrastructure;

import com.example.bootserver.order.domain.Order;
import com.example.bootserver.order.domain.OrderLine;
import com.example.bootserver.order.domain.OrderRepository;
import org.springframework.stereotype.Repository;

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
}
