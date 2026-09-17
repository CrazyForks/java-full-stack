package com.example.bootserver.order;

import com.example.bootserver.order.application.OrderService;
import com.example.bootserver.order.domain.Order;
import com.example.bootserver.order.domain.OrderLine;
import com.example.bootserver.order.domain.OrderRepository;
import com.example.bootserver.order.domain.OrderStatus;
import com.example.bootserver.order.infrastructure.OrderEntity;
import com.example.bootserver.order.infrastructure.OrderItemEntity;
import com.example.bootserver.order.infrastructure.OrderItemMapper;
import com.example.bootserver.order.infrastructure.OrderMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 订单领域不依赖 Web、Spring 或 ORM；应用层使用聚合仓储端口。 */
class OrderArchitectureTest {
    @Test
    void orderDomainIsFrameworkFree() {
        for (Class<?> type : List.of(Order.class, OrderLine.class, OrderStatus.class, OrderRepository.class)) {
            assertThat(type.getAnnotations()).isEmpty();
            assertThat(Arrays.stream(type.getDeclaredFields()).map(Field::getType))
                    .allMatch(fieldType -> fieldType.isPrimitive()
                            || fieldType.getPackageName().startsWith("java.")
                            || fieldType.getPackageName().equals("com.example.bootserver.order.domain"));
        }
    }

    @Test
    void orderApplicationDoesNotDependOnPersistenceEntitiesOrMappers() {
        assertThat(Arrays.stream(OrderService.class.getDeclaredFields()).map(Field::getType))
                .contains(OrderRepository.class)
                .doesNotContain(OrderMapper.class, OrderItemMapper.class,
                        OrderEntity.class, OrderItemEntity.class);
    }
}
