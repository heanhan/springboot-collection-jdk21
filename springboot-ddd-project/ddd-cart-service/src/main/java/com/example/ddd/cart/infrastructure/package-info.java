/**
 * 购物车基础设施层：原子创建用户行并加锁，JSON 转换有限大小的聚合条目。
 * Feign 实现商品端口；JPA 乐观锁版本和序列化格式只存在于此层。
 * 依赖方向为 interfaces → application → domain ← infrastructure。
 */
package com.example.ddd.cart.infrastructure;
