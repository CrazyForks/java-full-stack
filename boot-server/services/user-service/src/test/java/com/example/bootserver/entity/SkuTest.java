package com.example.bootserver.entity;

import com.example.bootserver.common.error.BusinessException;
import com.example.bootserver.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SKU 领域规则的纯单元测试：价格不变式收在实体里，
 * 校验行为不需要 Spring 上下文与数据库即可验证。
 */
class SkuTest {

    @Test
    void changePriceRejectsNegativePrice() {
        Sku sku = new Sku();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sku.changePrice(new BigDecimal("-0.01")));

        assertEquals(ErrorCode.PARAMETER_ERROR, exception.getErrorCode());
    }

    @Test
    void changePriceRejectsNullPrice() {
        Sku sku = new Sku();

        assertThrows(BusinessException.class, () -> sku.changePrice(null));
    }

    @Test
    void createBuildsSkuWithValidatedPriceAndZeroVersion() {
        Sku sku = Sku.create(1L, "SKU-A", new BigDecimal("19.90"));

        assertEquals(new BigDecimal("19.90"), sku.getPrice());
        assertEquals(0, sku.getVersion());
    }
}
