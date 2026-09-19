package com.example.ddd.common.result;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 分页响应包装。
 *
 * <p><b>为什么不直接使用 Spring Data 的 {@code Page<T>}？</b>
 * Spring Data 的 Page 是<b>基础设施层</b>的对象，包含大量 JPA 相关字段（Sort、Pageable 等），
 * 直接暴露到接口层会导致接口契约与持久化技术耦合。
 * 这里定义一个纯业务的分页 DTO，位于共享内核，跨服务复用。</p>
 *
 * @param <T> 元素类型（通常是 DTO 或 VO）
 * @author ddd-learning
 */
public record PageResult<T>(List<T> items,
                            long total,
                            int pageNum,
                            int pageSize,
                            int totalPages) implements Serializable {

    /**
     * 紧凑构造器：保证 items 非 null。
     */
    public PageResult {
        items = items == null ? Collections.emptyList() : List.copyOf(items);
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }
        totalPages = pageSize == 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);
    }

    /** 构造空分页 */
    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return new PageResult<>(Collections.emptyList(), 0L, pageNum, pageSize, 0);
    }

    /**
     * 从 Spring Data Page 转换。
     * <p>使用 {@code Object} 而不是直接依赖 Spring Data 类型，避免共享内核引入 Spring Data 依赖。</p>
     */
    public static <S, T> PageResult<T> of(List<S> source, long total, int pageNum, int pageSize, Function<S, T> mapper) {
        List<T> mapped = source == null ? Collections.emptyList() : source.stream().map(mapper).toList();
        return new PageResult<>(mapped, total, pageNum, pageSize, 0);
    }

    /** 是否首页 */
    public boolean isFirst() {
        return pageNum <= 1;
    }

    /** 是否末页 */
    public boolean isLast() {
        return pageNum >= totalPages;
    }

    /** 是否有下一页 */
    public boolean hasNext() {
        return !isLast();
    }
}
