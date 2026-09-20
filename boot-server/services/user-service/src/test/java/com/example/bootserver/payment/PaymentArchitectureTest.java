package com.example.bootserver.payment;

import com.example.bootserver.order.application.OrderPaymentService;
import com.example.bootserver.payment.application.PaymentNumberGenerator;
import com.example.bootserver.payment.application.PaymentService;
import com.example.bootserver.payment.domain.Payment;
import com.example.bootserver.payment.domain.PaymentRepository;
import com.example.bootserver.payment.domain.PaymentStatus;
import com.example.bootserver.payment.infrastructure.PaymentEntity;
import com.example.bootserver.payment.infrastructure.PaymentMapper;
import com.example.bootserver.stock.application.StockService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 支付领域保持纯 Java，跨上下文写入只通过显式应用契约。 */
class PaymentArchitectureTest {
    @Test
    void paymentDomainIsFrameworkFree() {
        for (Class<?> type : List.of(Payment.class, PaymentStatus.class, PaymentRepository.class)) {
            assertThat(type.getAnnotations()).isEmpty();
            assertThat(Arrays.stream(type.getDeclaredFields()).map(Field::getType))
                    .allMatch(fieldType -> fieldType.isPrimitive()
                            || fieldType.getPackageName().startsWith("java.")
                            || fieldType.getPackageName().equals("com.example.bootserver.payment.domain"));
        }
    }

    @Test
    void paymentApplicationUsesPortsAndContextServices() {
        assertThat(Arrays.stream(PaymentService.class.getDeclaredFields()).map(Field::getType))
                .containsExactly(OrderPaymentService.class, PaymentRepository.class,
                        PaymentNumberGenerator.class, StockService.class)
                .doesNotContain(PaymentMapper.class, PaymentEntity.class);
    }
}
