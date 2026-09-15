package com.example.bootserver.controller;

import com.example.bootserver.common.result.Result;
import com.example.bootserver.config.OpenApiConfig;
import com.example.bootserver.controller.dto.ProductDetailResponse;
import com.example.bootserver.controller.dto.CreateProductRequest;
import com.example.bootserver.controller.dto.CreatedProductResponse;
import com.example.bootserver.controller.dto.ProductPageRequest;
import com.example.bootserver.controller.dto.ProductPageResponse;
import com.example.bootserver.controller.dto.ProductStatusResponse;
import com.example.bootserver.controller.dto.UpdateProductStatusRequest;
import com.example.bootserver.service.ProductManagementService;
import com.example.bootserver.service.ProductQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 商品接口；读取仅需有效 JWT，写入要求商品管理权限。 */
@RestController
@RequestMapping("/products")
@Tag(name = "商品", description = "商品浏览与管理员创建、上下架")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTHENTICATION)
public class ProductController {

    private final ProductQueryService productQueryService;
    private final ProductManagementService productManagementService;

    public ProductController(ProductQueryService productQueryService,
                             ProductManagementService productManagementService) {
        this.productQueryService = productQueryService;
        this.productManagementService = productManagementService;
    }

    /** 分页列表：只返回上架商品，按商品 ID 升序。 */
    @GetMapping
    @Operation(summary = "分页浏览上架商品", description = "任意有效 JWT 均可访问；页码从 1 开始，每页最多 100 条。")
    public Result<ProductPageResponse> listProductsByPage(@Valid @ModelAttribute ProductPageRequest request) {
        return Result.ok(productQueryService.listProductsByPage(request.getPage(), request.getSize()));
    }

    /** 商品详情：未上架或不存在时返回 404。 */
    @GetMapping("/{id}")
    @Operation(summary = "查看上架商品详情", description = "任意有效 JWT 均可访问；返回商品及其未删除的 SKU。")
    public Result<ProductDetailResponse> getProductById(@PathVariable Long id) {
        return Result.ok(productQueryService.getProductById(id));
    }

    /** 管理员创建草稿商品与 SKU；数据库事务保证整体创建。 */
    @PostMapping
    @Operation(summary = "创建商品与 SKU", description = "需要 product:manage 权限；商品初始状态为草稿。")
    public Result<CreatedProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return Result.ok(productManagementService.createProduct(request));
    }

    /** 管理员上架或下架商品，C 端查询只展示上架状态。 */
    @PutMapping("/{id}/status")
    @Operation(summary = "商品上架或下架", description = "需要 product:manage 权限；只接受 ON_SALE 或 OFF_SALE。")
    public Result<ProductStatusResponse> updateProductStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateProductStatusRequest request) {
        return Result.ok(productManagementService.updateProductStatus(id, request));
    }
}
