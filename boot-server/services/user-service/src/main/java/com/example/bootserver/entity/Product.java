package com.example.bootserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 商品持久化实体；状态表示销售生命周期，deleted 表示逻辑删除。 */
@Data
@TableName("t_product")
public class Product {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ON_SALE = "ON_SALE";
    public static final String STATUS_OFF_SALE = "OFF_SALE";

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
