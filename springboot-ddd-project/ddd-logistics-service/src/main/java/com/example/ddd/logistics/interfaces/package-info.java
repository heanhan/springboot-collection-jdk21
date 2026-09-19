/**
 * 物流接口层：提供本人发货单查询和签收，以及可信内网的 Feign 查询契约。
 * 身份从认证服务解析，归属校验后调用应用服务，不直接更新轨迹或库存。
 */
package com.example.ddd.logistics.interfaces;
