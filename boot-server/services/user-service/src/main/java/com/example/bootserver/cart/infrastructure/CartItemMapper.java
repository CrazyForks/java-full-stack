package com.example.bootserver.cart.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

/** 购物车 SQL：条件更新与唯一约束共同保证并发下不丢失增量。 */
public interface CartItemMapper extends BaseMapper<CartItemEntity> {

    @Select("""
            SELECT COUNT(*) FROM t_sku s
            JOIN t_product p ON p.id = s.product_id
            WHERE s.id = #{skuId} AND s.deleted = 0 AND p.deleted = 0 AND p.status = #{onSaleStatus}
            """)
    int countOnSaleSku(@Param("skuId") Long skuId, @Param("onSaleStatus") String onSaleStatus);

    @Update("""
            UPDATE t_cart_item SET quantity = quantity + #{delta}, update_time = CURRENT_TIMESTAMP
            WHERE user_id = #{userId} AND sku_id = #{skuId} AND quantity <= #{maxQuantity} - #{delta}
            """)
    int increase(@Param("userId") Long userId, @Param("skuId") Long skuId,
                 @Param("delta") int delta, @Param("maxQuantity") int maxQuantity);

    @Update("""
            UPDATE t_cart_item SET quantity = #{quantity}, update_time = CURRENT_TIMESTAMP
            WHERE id = #{itemId} AND user_id = #{userId}
            """)
    int replace(@Param("userId") Long userId, @Param("itemId") Long itemId,
                @Param("quantity") int quantity);

    @Delete("DELETE FROM t_cart_item WHERE id = #{itemId} AND user_id = #{userId}")
    int deleteOwned(@Param("userId") Long userId, @Param("itemId") Long itemId);

    @Select("""
            SELECT c.id, c.sku_id AS skuId, s.sku_code AS skuCode,
                   p.name AS productName, s.price AS unitPrice, c.quantity
            FROM t_cart_item c
            JOIN t_sku s ON s.id = c.sku_id
            JOIN t_product p ON p.id = s.product_id
            WHERE c.user_id = #{userId}
            ORDER BY c.id ASC
            """)
    List<CartItemRow> listByUserId(@Param("userId") Long userId);
}
