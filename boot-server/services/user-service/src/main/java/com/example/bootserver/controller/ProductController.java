package com.example.bootserver.controller;

import com.example.bootserver.common.result.Result;
import com.example.bootserver.config.OpenApiConfig;
import com.example.bootserver.controller.dto.ProductDetailResponse;
import com.example.bootserver.controller.dto.ProductPageRequest;
import com.example.bootserver.controller.dto.ProductPageResponse;
import com.example.bootserver.service.ProductQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 商品读取接口；资源路径的外部前缀由 MVC 配置提供，访问仅要求有效 JWT。 */
@RestController
@RequestMapping("/products")
@Tag(name = "商品", description = "已登录用户可浏览的上架商品和 SKU")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTHENTICATION)
public class ProductController {

    private final ProductQueryService productQueryService;

    public ProductController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
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
}
