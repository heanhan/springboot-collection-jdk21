package com.example.ddd.cart.application.port;

import com.example.ddd.contract.product.SkuDTO;

/** 应用防腐层端口：结算预览读取实时商品信息，不把缓存价格当成交价格。 */
public interface CatalogGateway { SkuDTO get(String skuId); }
