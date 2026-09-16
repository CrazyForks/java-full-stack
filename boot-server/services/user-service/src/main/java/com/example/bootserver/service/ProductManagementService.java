package com.example.bootserver.service;

import com.example.bootserver.common.error.BusinessException;
import com.example.bootserver.common.error.ErrorCode;
import com.example.bootserver.controller.dto.CreateProductRequest;
import com.example.bootserver.controller.dto.CreateSkuRequest;
import com.example.bootserver.controller.dto.CreatedProductResponse;
import com.example.bootserver.controller.dto.ProductStatusResponse;
import com.example.bootserver.controller.dto.SkuPriceResponse;
import com.example.bootserver.controller.dto.UpdateProductStatusRequest;
import com.example.bootserver.controller.dto.UpdateSkuPriceRequest;
import com.example.bootserver.entity.Product;
import com.example.bootserver.entity.Sku;
import com.example.bootserver.mapper.ProductMapper;
import com.example.bootserver.mapper.SkuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 管理员商品写入规则；数据库约束负责并发下的最终一致性。 */
@Service
public class ProductManagementService {

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;

    public ProductManagementService(ProductMapper productMapper, SkuMapper skuMapper) {
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
    }

    /** 商品和所有 SKU 在同一事务创建，任一业务编码冲突时整体回滚。 */
    @Transactional
    public CreatedProductResponse createProduct(CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setStatus(Product.STATUS_DRAFT);
        productMapper.insert(product);

        List<CreatedProductResponse.CreatedSkuResponse> createdSkus = new ArrayList<>();
        for (CreateSkuRequest item : request.skus()) {
            Sku sku = Sku.create(product.getId(), item.skuCode(), item.price());
            skuMapper.insert(sku);
            createdSkus.add(new CreatedProductResponse.CreatedSkuResponse(
                    sku.getId(), sku.getSkuCode(), sku.getVersion()));
        }
        return new CreatedProductResponse(product.getId(), product.getStatus(), List.copyOf(createdSkus));
    }

    /** 乐观锁插件以请求版本加入 UPDATE 条件，受影响行数为 0 即并发冲突。 */
    @Transactional
    public SkuPriceResponse updateSkuPrice(Long id, UpdateSkuPriceRequest request) {
        Sku existing = skuMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SKU 不存在");
        }

        Sku update = new Sku();
        update.setId(id);
        update.changePrice(request.price());
        update.setVersion(request.version());
        update.setUpdateTime(LocalDateTime.now());
        if (skuMapper.updateById(update) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "SKU 价格版本已变化");
        }
        return new SkuPriceResponse(id, update.getPrice(), update.getVersion());
    }

    /** 状态写入只接受本卡的上架/下架动作，创建后的草稿默认值保持独立。 */
    @Transactional
    public ProductStatusResponse updateProductStatus(Long id, UpdateProductStatusRequest request) {
        Product existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        if (!Product.STATUS_ON_SALE.equals(request.status())
                && !Product.STATUS_OFF_SALE.equals(request.status())) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "状态只能为上架或下架");
        }
        Product update = new Product();
        update.setId(id);
        update.setStatus(request.status());
        update.setUpdateTime(LocalDateTime.now());
        if (productMapper.updateById(update) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        return new ProductStatusResponse(id, update.getStatus());
    }
}
