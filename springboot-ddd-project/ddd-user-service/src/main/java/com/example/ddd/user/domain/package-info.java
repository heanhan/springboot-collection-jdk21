/**
 * 领域层 (Domain Layer)：user-service 的业务核心。
 *
 * <p><b>本层职责：</b>
 * <ul>
 *   <li>定义业务概念：聚合根 ({@code User}, {@code Role}, {@code Permission})、
 *       实体 ({@code AddressEntry})、值对象 ({@code MemberLevel}, {@code UserStatus}, ...)。</li>
 *   <li>守护业务不变式：所有规则通过聚合根方法暴露，禁止外部直接改字段。</li>
 *   <li>发布领域事件：聚合根收集事件，应用服务在事务提交后发布。</li>
 *   <li>声明仓储接口：具体实现在 infrastructure 层（依赖倒置）。</li>
 * </ul>
 *
 * <p><b>依赖约束（严格）：</b>
 * <ul>
 *   <li>禁止 import {@code org.springframework.*}（除了极少数的 @Service 领域服务注解可选）。</li>
 *   <li>禁止 import {@code jakarta.persistence.*}。</li>
 *   <li>禁止 import {@code org.hibernate.*}。</li>
 *   <li>只依赖：JDK、{@code com.example.ddd.common.domain.*}、本包内其他类。</li>
 * </ul>
 *
 * <p><b>为什么如此严格？</b>
 * 领域层是业务的"永恒核心"，技术框架会过时（Spring 4 → 5 → 6，Hibernate 5 → 6），
 * 但业务规则相对稳定。让领域层独立于框架，可以：
 * <ol>
 *   <li>用普通 JUnit 就能测试业务规则，无需启动 Spring 容器。</li>
 *   <li>未来切换框架（例如换成 Quarkus）时业务代码零改动。</li>
 *   <li>阅读代码时能专注于业务，不被技术细节干扰。</li>
 * </ol>
 */
package com.example.ddd.user.domain;
