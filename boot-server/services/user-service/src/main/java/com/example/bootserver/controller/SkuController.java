package com.example.bootserver.controller;

import com.example.bootserver.common.result.Result;
import com.example.bootserver.config.OpenApiConfig;
import com.example.bootserver.controller.dto.SkuPriceResponse;
import com.example.bootserver.controller.dto.UpdateSkuPriceRequest;
import com.example.bootserver.service.ProductManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员 SKU 改价接口；版本由客户端提交，数据库条件更新防止覆盖。 */
@RestController
@RequestMapping("/skus")
@Tag(name = "SKU 管理", description = "管理员按版本调整 SKU 价格")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTHENTICATION)
public class SkuController {

    private final ProductManagementService productManagementService;

    public SkuController(ProductManagementService productManagementService) {
        this.productManagementService = productManagementService;
    }

    /** 改价：相同版本的并发请求只有一个能完成条件更新。 */
    @PutMapping("/{id}/price")
    @Operation(summary = "按版本修改 SKU 价格", description = "需要 product:manage 权限；版本不匹配返回 409。")
    public Result<SkuPriceResponse> updateSkuPrice(
            @PathVariable Long id, @Valid @RequestBody UpdateSkuPriceRequest request) {
        return Result.ok(productManagementService.updateSkuPrice(id, request));
    }
}
