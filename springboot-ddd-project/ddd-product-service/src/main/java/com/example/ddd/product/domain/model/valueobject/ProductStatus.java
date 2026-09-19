package com.example.ddd.product.domain.model.valueobject;

/**
 * 值对象：商品状态（sealed 类型）。
 *
 * <p><b>为什么用 sealed interface 而不是 enum？</b>
 * JDK 17+ 的 sealed 允许"每个状态携带自己的数据/行为"：
 * <ul>
 *   <li>{@link Draft}：草稿，不允许被下单，可能连 SKU 都还没建齐。</li>
 *   <li>{@link OnSale}：在售，可能带有上架时间、活动价等额外信息。</li>
 *   <li>{@link OffShelf}：已下架，可能带有下架原因（违规/售罄/停售）。</li>
 * </ul>
 * 用 enum 时这些差异化字段只能塞进 Map 或多个 nullable 字段；
 * sealed + record 让每个状态类型自描述。</p>
 *
 * <p><b>值对象特征：</b>无标识（同一状态多次出现视为同一个），不可变，按值相等。</p>
 */
public sealed interface ProductStatus permits ProductStatus.Draft, ProductStatus.OnSale, ProductStatus.OffShelf {

    /**
     * 状态的字符串码，用于持久化
     */
    String code();

    /**
     * 是否可被下单
     */
    boolean purchasable();

    /**
     * 草稿态
     */
    record Draft() implements ProductStatus {
        @Override
        public String code() {
            return "DRAFT";
        }

        @Override
        public boolean purchasable() {
            return false;
        }
    }

    /**
     * 在售。
     *
     * @param publishTime 上架时间戳（毫秒）
     */
    record OnSale(long publishTime) implements ProductStatus {
        @Override
        public String code() {
            return "ON_SALE";
        }

        @Override
        public boolean purchasable() {
            return true;
        }
    }

    /**
     * 已下架。
     *
     * @param reason 下架原因
     */
    record OffShelf(String reason) implements ProductStatus {
        @Override
        public String code() {
            return "OFF_SHELF";
        }

        @Override
        public boolean purchasable() {
            return false;
        }
    }

    // ============================================================
    // 工厂方法：从数据库字符串反序列化
    // ============================================================

    static ProductStatus of(String code, long publishTime, String offShelfReason) {
        if (code == null) return new Draft();
        return switch (code) {
            case "ON_SALE" -> new OnSale(publishTime);
            case "OFF_SHELF" -> new OffShelf(offShelfReason == null ? "" : offShelfReason);
            case "DRAFT" -> new Draft();
            default -> throw new IllegalArgumentException("未知商品状态: " + code);
        };
    }
}
