package org.example.redis.lettuce.pojo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "t_category")
@Data
public class Category implements Serializable {
    /**
     * 分类编号
     */
//    @JsonProperty("i")
    @Id
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

    /**
     * 子分类列表
     */
    @Transient
    private List<Category> children;
}
