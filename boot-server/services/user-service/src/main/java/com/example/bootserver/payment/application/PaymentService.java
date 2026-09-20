package com.example.bootserver.payment.application;

import com.example.bootserver.order.application.OrderPaymentService;
import com.example.bootserver.order.application.PayableOrder;
import com.example.bootserver.payment.domain.Payment;
import com.example.bootserver.payment.domain.PaymentRepository;
import com.example.bootserver.stock.application.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 同步模拟支付编排；订单迁移、支付单和库存实扣共享同一本地事务。 */
@Service
public class PaymentService {
    private final OrderPaymentService orders;
    private final PaymentRepository payments;
    private final PaymentNumberGenerator numbers;
    private final StockService stocks;

    public PaymentService(OrderPaymentService orders, PaymentRepository payments,
                          PaymentNumberGenerator numbers, StockService stocks) {
        this.orders = orders;
        this.payments = payments;
        this.numbers = numbers;
        this.stocks = stocks;
    }

    @Transactional
    public PaymentResult pay(Long userId, Long orderId) {
        PayableOrder order = orders.claimForPayment(userId, orderId);
        Payment payment = Payment.success(order.id(), numbers.nextPayNo(), order.totalAmount());
        Long paymentId = payments.insert(payment);
        order.items().forEach(item -> stocks.settleReserved(item.skuId(), item.quantity()));
        return new PaymentResult(paymentId, payment.orderId(), payment.payNo(), payment.amount(), payment.status());
    }
}
