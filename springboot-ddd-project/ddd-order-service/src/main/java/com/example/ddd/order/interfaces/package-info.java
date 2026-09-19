/**
 * 订单接口层：认证与归属校验、请求校验和契约转换，业务状态转换交给应用服务。
 * 对外提供用户 REST 与内部 Feign Facade，不直接操作数据库或发布 MQ。
 */
package com.example.ddd.order.interfaces;
