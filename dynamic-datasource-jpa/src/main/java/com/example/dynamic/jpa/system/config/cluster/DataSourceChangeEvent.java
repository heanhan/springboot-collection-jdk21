package com.example.dynamic.jpa.system.config.cluster;

/**
 * 租户数据源路由变更事件（多实例间通过 Redis Pub/Sub 广播）。
 *
 * <p>序列化格式：{@code TYPE:tenantId}，例如 {@code ADD:12}、{@code REMOVE:12}。</p>
 *
 * @author zhaojh
 */
public record DataSourceChangeEvent(Type type, Integer tenantId) {

    public enum Type {
        /** 新增/启用租户数据源 */
        ADD,
        /** 停用/移除租户数据源 */
        REMOVE
    }

    private static final String SEP = ":";

    public static DataSourceChangeEvent add(Integer tenantId) {
        return new DataSourceChangeEvent(Type.ADD, tenantId);
    }

    public static DataSourceChangeEvent remove(Integer tenantId) {
        return new DataSourceChangeEvent(Type.REMOVE, tenantId);
    }

    public String serialize() {
        return type.name() + SEP + tenantId;
    }

    /**
     * 解析广播负载；非法格式返回 null。
     */
    public static DataSourceChangeEvent parse(String payload) {
        if (payload == null) {
            return null;
        }
        int idx = payload.indexOf(SEP);
        if (idx <= 0 || idx == payload.length() - 1) {
            return null;
        }
        try {
            Type type = Type.valueOf(payload.substring(0, idx));
            Integer tenantId = Integer.valueOf(payload.substring(idx + 1));
            return new DataSourceChangeEvent(type, tenantId);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
