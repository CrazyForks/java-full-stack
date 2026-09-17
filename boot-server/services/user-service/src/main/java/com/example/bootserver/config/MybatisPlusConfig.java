package com.example.bootserver.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置 —— 注册 SKU 乐观锁和 H2 分页插件。
 * <p>
 * 注意：MP 3.5.9+ 起分页插件依赖的 jsqlparser 改为可选依赖，
 * 必须额外引入 mybatis-plus-jsqlparser（见 app 模块 pom）分页才会生效。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 乐观锁：update 时自动把 version 拼进 WHERE 并 +1（当前仅 Sku 在用，见 entity/Sku.java 的 @Version）；
        // 影响行数为 0 表示并发冲突，由调用方决定重试或报失败。
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 物理分页：selectPage 先 count 再按声明的 H2 方言生成 LIMIT 子句；
        // 不注册该插件时分页静默失效、整表查回。消费方：ProductQueryService、UserService。
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        return interceptor;
    }
}
