/**
 * 通用值对象 (Value Object)。
 *
 * <p>本包提供跨限界上下文复用的值对象：
 * <ul>
 *   <li>{@link com.example.ddd.common.domain.valueobject.Money}   金额（币种 + BigDecimal）</li>
 *   <li>{@link com.example.ddd.common.domain.valueobject.Address} 收货地址</li>
 *   <li>{@link com.example.ddd.common.domain.valueobject.Mobile}  中国大陆手机号</li>
 *   <li>{@link com.example.ddd.common.domain.valueobject.Email}   邮箱</li>
 * </ul>
 *
 * <p><b>值对象三原则：</b>无标识、不可变、按值相等。全部使用 JDK 21 record 实现。</p>
 */
package com.example.ddd.common.domain.valueobject;
