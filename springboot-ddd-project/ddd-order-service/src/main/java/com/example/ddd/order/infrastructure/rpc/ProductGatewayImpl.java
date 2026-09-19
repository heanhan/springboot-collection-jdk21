package com.example.ddd.order.infrastructure.rpc;

import com.example.ddd.common.domain.valueobject.Money;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.common.result.Result;
import com.example.ddd.contract.product.ProductFeignClient;
import com.example.ddd.contract.product.SkuDTO;
import com.example.ddd.order.application.port.ProductGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 适配器 (Adapter)：{@link ProductGateway} 的 Feign 实现，即商品上下文的防腐层落地。
 *
 * <p><b>翻译职责：</b>把商品服务的 {@link SkuDTO} 翻译成订单语言的 {@link SkuSnapshot}，
 * 使订单领域不直接依赖商品契约 DTO。Feign 调用异常统一转成 {@link BusinessException}。</p>
 */
@Component
public class ProductGatewayImpl implements ProductGateway {

    private static final Logger log = LoggerFactory.getLogger(ProductGatewayImpl.class);
    private static final String STATUS_ON_SALE = "ON_SALE";

    private final ProductFeignClient productFeignClient;

    public ProductGatewayImpl(ProductFeignClient productFeignClient) {
        this.productFeignClient = productFeignClient;
    }

    @Override
    public Optional<SkuSnapshot> getSku(String skuId) {
        Result<SkuDTO> result;
        try {
            result = productFeignClient.getSku(skuId);
        } catch (Exception e) {
            log.error("[ACL] 调用商品服务失败 skuId={}", skuId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "商品服务暂不可用，请稍后重试", e);
        }
        if (result == null || !result.isSuccess() || result.getData() == null) {
            return Optional.empty();
        }
        SkuDTO dto = result.getData();
        return Optional.of(new SkuSnapshot(dto.skuId(), dto.spuId(), dto.spuName(), dto.skuName(),
                dto.image(), Money.ofCny(dto.price()), STATUS_ON_SALE.equals(dto.status())));
    }
}
