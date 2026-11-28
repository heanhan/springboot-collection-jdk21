package com.example.dynamic.jpa.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 多租户数据源治理配置（绑定前缀 {@code app.datasource}）。
 *
 * <p>覆盖启动注册策略、租户连接池独立调优、连接密码加密、多实例路由广播等生产化能力。</p>
 *
 * @author zhaojh
 */
@Data
@ConfigurationProperties(prefix = "app.datasource")
public class TenantDataSourceProperties {

    /**
     * 启动注册策略
     */
    private Startup startup = new Startup();

    /**
     * 租户连接池独立调优
     */
    private TenantPool tenantPool = new TenantPool();

    /**
     * 连接密码加密（Druid ConfigFilter RSA）
     */
    private Crypto crypto = new Crypto();

    /**
     * 多实例路由变更广播（Redis Pub/Sub）
     */
    private ClusterSync clusterSync = new ClusterSync();

    @Data
    public static class Startup {
        /**
         * 启动时是否全量注册租户连接池；关闭则完全依赖运行期懒加载
         */
        private boolean eagerRegister = true;
        /**
         * 单个租户数据源初始化失败时是否中断启动；false=失败隔离（告警跳过，运行期懒加载兜底）
         */
        private boolean failFast = false;
    }

    @Data
    public static class TenantPool {
        /**
         * 初始连接数，0=懒建连，避免租户数增长导致连接爆炸
         */
        private int initialSize = 0;
        private int minIdle = 0;
        private int maxActive = 8;
        private int maxWait = 10000;
        private int connectTimeoutMillis = 3000;
        private int socketTimeoutMillis = 10000;
        private long idleEvictMillis = 1800000;
        private int maxTotalConnections = 200;
    }

    @Data
    public static class Crypto {
        /**
         * 是否开启租户库连接密码加密存储
         */
        private boolean enabled = false;
        /**
         * Druid ConfigFilter 解密用公钥
         */
        private String publicKey = "";
        /**
         * 新增数据源时加密用私钥（仅管理侧持有）
         */
        private String privateKey = "";
    }

    @Data
    public static class ClusterSync {
        private boolean enabled = false;
        private String channel = "dynjpa:datasource:changes";
    }

    @Data
    public static class Provisioner {
        private String host = "127.0.0.1";
        private int port = 3306;
        private String username = "";
        private String password = "";
        private boolean createUser = true;
        private String userHost = "%";
        private String sslMode = "DISABLED";
    }

    private Provisioner provisioner = new Provisioner();
}
