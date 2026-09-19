-- ==============================================================================
-- Docker MySQL 首次启动初始化脚本
-- ==============================================================================
-- 为每个 DDD 业务服务创建独立 Schema，实现"数据库级"的限界上下文隔离。
-- 表结构与初始化数据由各服务的 Flyway 迁移脚本负责，这里只建库和授权。
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS ddd_auth       DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ddd_user       DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ddd_product    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ddd_inventory  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ddd_order      DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ddd_payment    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ddd_logistics  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ddd_cart       DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
