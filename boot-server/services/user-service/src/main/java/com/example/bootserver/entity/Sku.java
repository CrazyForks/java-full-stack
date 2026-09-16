package com.example.bootserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.example.bootserver.common.error.BusinessException;
import com.example.bootserver.common.error.ErrorCode;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** SKU 持久化实体；skuCode 在逻辑删除后仍由数据库保持唯一。 */
@Data
@TableName("t_sku")
public class Sku {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String skuCode;
    private BigDecimal price;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 创建初始 SKU：价格不变式在构造处守住，version 从 0 起步。 */
    public static Sku create(Long productId, String skuCode, BigDecimal price) {
        Sku sku = new Sku();
        sku.setProductId(productId);
        sku.setSkuCode(skuCode);
        sku.changePrice(price);
        sku.setVersion(0);
        return sku;
    }

    /** 价格非负是 SKU 的不变式：任何入口（HTTP / 未来的 MQ、定时任务）改价都必须经过这里。 */
    public void changePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.signum() < 0) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "价格不能为负数");
        }
        this.price = newPrice;
    }
}
