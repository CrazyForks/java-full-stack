package com.example.bootserver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bootserver.common.error.BusinessException;
import com.example.bootserver.common.error.ErrorCode;
import com.example.bootserver.controller.dto.ProductDetailResponse;
import com.example.bootserver.controller.dto.ProductPageResponse;
import com.example.bootserver.controller.dto.ProductSummaryResponse;
import com.example.bootserver.controller.dto.SkuResponse;
import com.example.bootserver.entity.Product;
import com.example.bootserver.entity.Sku;
import com.example.bootserver.mapper.ProductMapper;
import com.example.bootserver.mapper.SkuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 商品只读查询规则：仅展示上架且未逻辑删除的商品和 SKU。 */
@Service
public class ProductQueryService {

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;

    public ProductQueryService(ProductMapper productMapper, SkuMapper skuMapper) {
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
    }

    /** 商品上下文提供给库存管理的只读契约：草稿或下架 SKU 仍可配置库存。 */
    @Transactional(readOnly = true)
    public boolean hasSkuById(Long skuId) {
        return skuMapper.selectById(skuId) != null;
    }

    /** 一次批量读取商品和 SKU；仅向订单上下文返回当前可售的报价快照。 */
    public List<OrderableSkuQuote> listOrderableSkuQuotes(Set<Long> skuIds) {
        if (skuIds.isEmpty()) {
            return List.of();
        }
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>().in(Sku::getId, skuIds));
        if (skus.isEmpty()) {
            return List.of();
        }
        Set<Long> productIds = skus.stream().map(Sku::getProductId).collect(Collectors.toSet());
        Set<Long> onSaleProductIds = productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .in(Product::getId, productIds)
                        .eq(Product::getStatus, Product.STATUS_ON_SALE))
                .stream().map(Product::getId).collect(Collectors.toSet());
        return skus.stream().filter(sku -> onSaleProductIds.contains(sku.getProductId()))
                .map(sku -> new OrderableSkuQuote(sku.getId(), sku.getPrice())).toList();
    }

    /** 按主键稳定排序后分页，避免不同请求的记录顺序漂移。 */
    @Transactional(readOnly = true)
    public ProductPageResponse listProductsByPage(long page, long size) {
        Page<Product> result = productMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, Product.STATUS_ON_SALE)
                        .orderByAsc(Product::getId));
        List<ProductSummaryResponse> items = result.getRecords().stream()
                .map(product -> new ProductSummaryResponse(
                        product.getId(), product.getName(), product.getDescription()))
                .toList();
        return new ProductPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), items);
    }

    /** 下架、草稿、逻辑删除和不存在的商品都以相同的不存在语义返回。 */
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || !Product.STATUS_ON_SALE.equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        List<SkuResponse> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                        .eq(Sku::getProductId, id)
                        .orderByAsc(Sku::getId))
                .stream()
                .map(sku -> new SkuResponse(sku.getId(), sku.getSkuCode(), sku.getPrice()))
                .toList();
        return new ProductDetailResponse(product.getId(), product.getName(), product.getDescription(), skus);
    }
}
