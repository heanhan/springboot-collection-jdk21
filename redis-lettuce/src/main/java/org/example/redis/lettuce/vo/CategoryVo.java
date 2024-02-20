package org.example.redis.lettuce.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CategoryVo {
    /**
     * 分类编号
     */
    private Long id;

    /**
     * 分类层级
     */
    private Integer level;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类编号
     */
    private Long parentId;

}