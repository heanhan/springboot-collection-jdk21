/**
 * 购物车接口层：校验请求数量，以认证用户作为聚合标识，委托应用层执行业务。
 * 内部条目查询仅用于可信内网；HTTP 层不访问 PO，也不预占库存。
 */
package com.example.ddd.cart.interfaces;
