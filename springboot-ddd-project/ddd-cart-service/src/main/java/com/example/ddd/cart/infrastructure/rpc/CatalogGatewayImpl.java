package com.example.ddd.cart.infrastructure.rpc;

import com.example.ddd.cart.application.port.CatalogGateway;
import com.example.ddd.contract.product.*;
import org.springframework.stereotype.Component;

/** 基础设施 Feign 适配器：异常向上传播，禁止使用伪造价格降级。 */
@Component
public class CatalogGatewayImpl implements CatalogGateway {
    private final ProductFeignClient client;
    public CatalogGatewayImpl(ProductFeignClient client) { this.client=client; }
    public SkuDTO get(String id) { return client.getSku(id).requireData(); }
}
