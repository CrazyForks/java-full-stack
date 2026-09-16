package com.example.bootserver.stock.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 锁定量可能由后续下单改变，因此调整总量必须在数据库重新检查锁定量。 */
public interface StockMapper extends BaseMapper<StockEntity> {

    @Update("""
            UPDATE t_stock SET total_count = #{total}, update_time = CURRENT_TIMESTAMP
            WHERE sku_id = #{skuId} AND locked_count <= #{total}
            """)
    int updateTotalIfEnough(@Param("skuId") Long skuId, @Param("total") long total);
}
