package com.example.bootserver.controller.dto;

import java.math.BigDecimal;

/** 商品详情中的 SKU 公开字段，价格保留数据库的精确十进制表示。 */
public record SkuResponse(Long id, String skuCode, BigDecimal price) {
}
