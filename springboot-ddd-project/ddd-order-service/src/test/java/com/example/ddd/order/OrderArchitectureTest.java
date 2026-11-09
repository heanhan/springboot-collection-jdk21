package com.example.ddd.order;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 架构守护测试：把 package-info.java 里的分层约定变成可执行的 CI 约束。
 *
 * <p>核心承诺（对应父 POM 的依赖方向说明）：
 * <pre>interfaces -&gt; application -&gt; domain &lt;- infrastructure</pre>
 * domain 层是纯粹的业务模型——不依赖 Spring / JPA 技术框架，
 * 不依赖 ddd-api-contract 对外契约（集成事件由基础设施层在出站边界翻译），
 * 也不反向依赖任何其他层。</p>
 *
 * <p>说明：{@code com.example.ddd.common} 是共享内核（Money/Address/DomainEvent 抽象），
 * 各层均可依赖，不在禁止之列。</p>
 */
@AnalyzeClasses(packages = "com.example.ddd.order",
        importOptions = ImportOption.DoNotIncludeTests.class)
public class OrderArchitectureTest {

    private static final String DOMAIN = "..order.domain..";

    /** 规则 1：domain 不依赖 Spring 与 JPA——领域模型保持纯 POJO。 */
    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring_or_jpa = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "javax.persistence..")
            .because("领域层不允许被技术框架污染：持久化注解属于 infrastructure 的 PO，"
                    + "事务与装配属于 application / infrastructure 的职责");

    /** 规则 2：domain 不依赖 contract——发布语言（集成事件）只能在出站边界出现。 */
    @ArchTest
    static final ArchRule domain_should_not_depend_on_contract = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAPackage("com.example.ddd.contract..")
            .because("contract 是跨上下文承诺兼容的发布语言；领域层收集内部领域事件，"
                    + "由 infrastructure.mq.OrderIntegrationEventTranslator 在出站时翻译为集成事件");

    /** 规则 3：domain 不反向依赖任何其他层——依赖永远向内指。 */
    @ArchTest
    static final ArchRule domain_should_not_depend_on_other_layers = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..order.application..", "..order.infrastructure..", "..order.interfaces..")
            .because("domain 是核心，不允许感知外层；接口与实现在 domain 定义、由外层依赖");

    /** 规则 4：application 不依赖本服务的 infrastructure / interfaces——编排层只面向端口与领域。 */
    @ArchTest
    static final ArchRule application_should_not_depend_on_outer_layers = noClasses()
            .that().resideInAPackage("..order.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..order.infrastructure..", "..order.interfaces..")
            .because("应用层通过 application.port 下的抽象与基础设施协作，具体实现由 Spring 装配注入");
}
