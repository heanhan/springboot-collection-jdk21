package com.example.jpa.model.vo;

import com.example.jpa.config.JpaConverterListJson;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Convert;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;


@Data
public class PersonSelectParam {

    /**
     * ID，唯一标识列，使用主键自增策略
     */
    private Long id;

    /**
     * 用户名称
     */
    private String userName;


    /**
     * 性别
     */
    private Boolean sex;

    /**
     * 身高
     */
    private Double height;

    /**
     * 爱好
     */
    @Convert(converter = JpaConverterListJson.class)
    private List<String> hover;

    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /**
     * 最后修改时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastModifiedTime;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 最后修改人ID
     */
    private Long lastModifierId;

    /**
     * false:0 无效
     * true:1 有效 默认
     */
    private Boolean isDeleted;

    /**
     * 当前页  默认为 0 【第一页】
     */
    private Integer indexSize=0;

    /**
     * 每页大小 默认每页大小为10
     */
    private Integer pageSize=10;
}
